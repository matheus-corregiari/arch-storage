package br.com.arch.toolkit.storage.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import br.com.arch.toolkit.storage.core.KeyValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreProviderTest {
    @Test
    fun primitivesRoundTripAndClear() = runTest {
        val provider = DataStoreProvider(TestStore())
        checkEntry(provider.boolean("boolean"), true)
        checkEntry(provider.double("double"), 2.5)
        checkEntry(provider.float("float"), 1.5f)
        checkEntry(provider.int("int"), 42)
        checkEntry(provider.long("long"), 42L)
        checkEntry(provider.string("string"), "hello")
        val bytes = provider.byteArray("bytes")
        bytes.set(byteArrayOf(1, 2), this)
        advanceUntilIdle()
        assertContentEquals(byteArrayOf(1, 2), bytes.get().first())
        bytes.set(null, this)
        advanceUntilIdle()
        assertNull(bytes.get().first())
    }

    @Test
    fun separateProvidersObserveSameStore() = runTest {
        val store = TestStore()
        val first = DataStoreProvider(store).string("name")
        val second = DataStoreProvider(store).string("name")
        first.set("Alice", this)
        advanceUntilIdle()
        assertEquals("Alice", second.get().first())
        assertEquals("Alice", second.lastValue)
        assertNull(DataStoreProvider(store).string("other").get().first())
    }

    @Test
    fun newestPendingWriteWins() = runTest {
        val entry = DataStoreProvider(TestStore()).int("counter")
        entry.set(1, this)
        entry.set(2, this)
        advanceUntilIdle()
        assertEquals(2, entry.get().first())
    }

    @Test
    fun enumsPersistNamesAndFallbackForUnknownNames() = runTest {
        val provider = DataStoreProvider(TestStore())
        val theme = provider.enum("theme", Theme.Light)
        assertEquals(Theme.Light, theme.get().first())
        theme.set(Theme.Dark, this)
        advanceUntilIdle()
        assertEquals("Dark", provider.string("theme").get().first())
        assertEquals(Theme.Dark, theme.get().first())
        provider.string("theme").set("LIGHT", this)
        advanceUntilIdle()
        assertEquals(Theme.Light, theme.get().first())
        provider.string("theme").set("unknown", this)
        advanceUntilIdle()
        assertEquals(Theme.Light, theme.get().first())
    }

    @Test
    fun modelsSerializeAndNullRemovesPersistedValue() = runTest {
        val provider = DataStoreProvider(TestStore())
        val model = provider.model<List<Int>>("model")
        assertNull(model.get().first())
        model.set(listOf(1, 2), this)
        advanceUntilIdle()
        assertEquals(listOf(1, 2), model.get().first())
        assertEquals(listOf(1, 2), provider.model<List<Int>>("model").get().first())
        model.set(null, this)
        advanceUntilIdle()
        assertNull(provider.string("model").get().first())
    }

    private suspend fun <T> TestScope.checkEntry(entry: KeyValue<T?>, value: T) {
        assertNull(entry.get().first())
        entry.set(value, this)
        advanceUntilIdle()
        assertEquals(value, entry.lastValue)
        assertEquals(value, entry.get().first())
        entry.set(null, this)
        advanceUntilIdle()
        assertNull(entry.get().first())
    }

    private enum class Theme { Light, Dark }
    private class TestStore : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        private val mutex = Mutex()
        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences
        ): Preferences =
            mutex.withLock { transform(data.value).also { data.value = it } }
    }
}
