package br.com.arch.toolkit.storage.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import br.com.arch.toolkit.storage.core.KeyValue.Companion.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WriteCompletionTest {
    @Test
    fun completionWaitsForBackendAndOtherEntriesObserveTheWrite() = runTest {
        val gate = CompletableDeferred<Unit>()
        val store = Store { gate.await() }
        val provider = DataStoreProvider(store)
        val entry = provider.int("count")
        val observer = provider.int("count")
        val write = async { entry.setAndAwait(7) }
        runCurrent()
        assertFalse(write.isCompleted)
        assertNull(entry.lastValue)
        assertNull(observer.get().first())
        gate.complete(Unit)
        write.await()
        assertEquals(7, entry.lastValue)
        assertNull(observer.lastValue)
        assertEquals(7, observer.get().first())
        entry.setAndAwait(null)
        assertNull(observer.get().first())
    }

    @Test
    fun backendAndConversionErrorsPropagateWithoutReplacingValue() = runTest {
        var failure: Exception? = null
        val provider = DataStoreProvider(Store { failure?.let { throw it } })
        val entry = provider.int("count")
        entry.setAndAwait(1)
        failure = IllegalStateException("disk unavailable")
        assertSame(failure, assertFailsWith<IllegalStateException> { entry.setAndAwait(2) })
        assertEquals(1, entry.lastValue)
        assertEquals(1, entry.get().first())
        val mapped = entry.map({ it.toString() }, { _: String -> error("encode failed") })
        assertFailsWith<IllegalStateException> { mapped.setAndAwait("2") }
        val cancelled = entry.map(
            { it.toString() },
            { _: String -> throw CancellationException("encode") }
        )
        assertFailsWith<CancellationException> { cancelled.setAndAwait("2") }
        failure = CancellationException("backend cancelled")
        assertSame(failure, assertFailsWith<CancellationException> { entry.setAndAwait(2) })
        assertEquals(1, entry.get().first())
    }

    @Test
    fun callerCancellationBeforeBackendCompletionPreservesCacheAndStore() = runTest {
        val gate = CompletableDeferred<Unit>()
        val entry = DataStoreProvider(Store { gate.await() }).int("count")
        val write = async { entry.setAndAwait(7) }
        runCurrent()
        write.cancel()
        write.join()
        assertFailsWith<CancellationException> { write.await() }
        assertNull(entry.lastValue)
        assertNull(entry.get().first())
    }

    @Test
    fun legacyReplacementDoesNotCancelAcknowledgedWrite() = runTest {
        val gate = CompletableDeferred<Unit>()
        val entry = DataStoreProvider(Store { gate.await() }).int("count")
        val write = async { entry.setAndAwait(1) }
        runCurrent()
        entry.set(2, this)
        entry.set(3, this)
        runCurrent()
        assertFalse(write.isCancelled)
        assertFalse(write.isCompleted)
        gate.complete(Unit)
        write.await()
        testScheduler.advanceUntilIdle()
        assertEquals(3, entry.get().first())
    }

    private class Store(private val beforeCommit: suspend () -> Unit) : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(
            transform: suspend (Preferences) -> Preferences
        ): Preferences {
            val updated = transform(data.value)
            beforeCommit()
            data.value = updated
            return updated
        }
    }
}
