package br.com.arch.toolkit.storage.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ConcurrentWritesTest {
    @Test
    fun simultaneousWritersLeaveOnlyOnePendingJobPerEntry() = runTest {
        val parent = Job()
        // Keep persistence paused while real dispatcher threads submit competing writes.
        val writeScheduler = TestCoroutineScheduler()
        val scope = CoroutineScope(parent + StandardTestDispatcher(writeScheduler))
        val store = object : DataStore<Preferences> {
            override val data = MutableStateFlow(emptyPreferences())
            override suspend fun updateData(
                transform: suspend (Preferences) -> Preferences
            ): Preferences =
                transform(data.value).also { data.value = it }
        }
        val entry = DataStoreProvider(store).int("counter")
        try {
            repeat(20) {
                val ready = Channel<Unit>(8)
                val start = CompletableDeferred<Unit>()
                val writers = List(8) { value ->
                    launch(Dispatchers.Default) {
                        ready.send(Unit)
                        start.await()
                        entry.set(value, scope)
                    }
                }
                repeat(8) { ready.receive() }
                start.complete(Unit)
                writers.joinAll()
                assertEquals(1, parent.children.count { it.isActive })
            }
            entry.set(99, scope)
            writeScheduler.advanceUntilIdle()
            assertEquals(99, entry.get().first())
            assertEquals(99, entry.lastValue)
        } finally {
            parent.cancel()
        }
    }
}
