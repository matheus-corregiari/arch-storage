package br.com.arch.toolkit.storage.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ConcurrentWritesTest {
    @Test
    fun simultaneousWritersLeaveOnlyOnePendingJobPerEntry() = runTest {
        val parent = Job()
        val scope = CoroutineScope(parent + StandardTestDispatcher(testScheduler))
        val store = object : DataStore<Preferences> {
            override val data = MutableStateFlow(emptyPreferences())
            override suspend fun updateData(
                transform: suspend (Preferences) -> Preferences
            ): Preferences =
                transform(data.value).also { data.value = it }
        }
        val entry = DataStoreProvider(store).int("counter")
        val pool = Executors.newFixedThreadPool(8)
        try {
            repeat(20) {
                val barrier = CyclicBarrier(8)
                val writers = (0 until 8).map { value ->
                    pool.submit {
                        barrier.await(10, TimeUnit.SECONDS)
                        entry.set(value, scope)
                    }
                }
                writers.forEach { it.get(10, TimeUnit.SECONDS) }
                assertEquals(1, parent.children.count { it.isActive })
            }
            entry.set(99, scope)
            advanceUntilIdle()
            assertEquals(99, entry.get().first())
            assertEquals(99, entry.lastValue)
        } finally {
            pool.shutdownNow()
            parent.cancel()
        }
    }
}
