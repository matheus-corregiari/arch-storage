package br.com.arch.toolkit.storage.memory

import br.com.arch.toolkit.storage.core.KeyValue
import br.com.arch.toolkit.storage.core.KeyValue.Companion.required
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MemoryStoreProviderTest {
    @Test
    fun primitivesRoundTripAndClear() = runTest {
        val provider = MemoryStoreProvider(mutableMapOf())
        checkEntry(provider.boolean("boolean"), true, this)
        checkEntry(provider.double("double"), 2.5, this)
        checkEntry(provider.float("float"), 1.5f, this)
        checkEntry(provider.int("int"), 42, this)
        checkEntry(provider.long("long"), 42L, this)
        checkEntry(provider.string("string"), "hello", this)
        val bytes = provider.byteArray("bytes")
        bytes.set(byteArrayOf(1, 2), this)
        assertContentEquals(byteArrayOf(1, 2), bytes.get().first())
        bytes.set(null, this)
        assertNull(bytes.lastValue)
    }

    @Test
    fun sameKeySharesValuesWhileDifferentStoresAreIsolated() = runTest {
        val provider = MemoryStoreProvider(mutableMapOf())
        val first = provider.string("name")
        val second = provider.string("name")
        first.set("Alice", this)
        assertEquals("Alice", second.get().first())
        assertNull(provider.string("other").lastValue)
        assertNull(MemoryStoreProvider(mutableMapOf()).string("name").lastValue)
    }

    @Test
    fun enumsUseDefaultUntilAssigned() = runTest {
        val provider = MemoryStoreProvider(mutableMapOf())
        val theme = provider.enum("theme", Theme.entries, Theme.Light)
        assertEquals(Theme.Light, theme.get().first())
        theme.set(Theme.Dark, this)
        assertEquals(Theme.Dark, provider.enum("theme", Theme.Light).get().first())
    }

    @Test
    fun modelsRetainObjectsWithoutSerializing() = runTest {
        val provider = MemoryStoreProvider(mutableMapOf())
        val model = provider.model<String>("model", {
            error("Unexpected decode")
        }, { error("Unexpected encode") })
        model.set("object", this)
        assertEquals("object", model.get().first())
        model.set(null, this)
        assertNull(model.lastValue)
    }

    @Test
    fun delegatesReadAndWriteStorage() = runTest {
        val entry = MemoryStoreProvider(mutableMapOf()).int("count").required { 0 }.scope(this)
        val holder = Counter(entry)
        assertEquals(0, holder.count)
        holder.count = 3
        assertEquals(3, entry.get().first())
        assertEquals(3, holder.count)
    }

    private suspend fun <T> checkEntry(entry: KeyValue<T?>, value: T, scope: CoroutineScope) {
        assertNull(entry.get().first())
        entry.set(value, scope)
        assertEquals(value, entry.get().first())
        assertEquals(value, entry.lastValue)
        entry.set(null, scope)
        assertNull(entry.get().first())
    }

    private enum class Theme { Light, Dark }
    private class Counter(entry: KeyValue<Int>) {
        var count by entry.delegate()
    }
}
