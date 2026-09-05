package br.com.arch.toolkit.storage.core

import br.com.arch.toolkit.storage.core.KeyValue.Companion.default
import br.com.arch.toolkit.storage.core.KeyValue.Companion.map
import br.com.arch.toolkit.storage.core.KeyValue.Companion.required
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class KeyValueTest {
    @Test
    fun currentReadsLatestEmission() = runTest {
        val entry = Entry(1)
        entry.set(2, this)
        assertEquals(2, entry.current())
        assertSame(entry, entry.scope(this))
    }

    @Test
    fun currentFallsBackForEmptyAndFailedFlows() = runTest {
        assertEquals(7, Entry(7, emptyFlow()).current())
        assertEquals(8, Entry(8, flow { error("read failed") }).current())
    }

    @Test
    fun requiredUsesDefaultAndThenStoredValue() = runTest {
        val entry = Entry<Int?>(null)
        val required = entry.required { 4 }
        assertEquals(4, required.lastValue)
        assertEquals(4, required.get().first())
        required.set(9, this)
        assertEquals(9, required.lastValue)
        assertEquals(9, required.get().first())
    }

    @Test
    fun requiredWithoutValueFails() = runTest {
        val required = Entry<Int?>(null).required()
        assertFailsWith<IllegalStateException> { required.lastValue }
        assertFailsWith<IllegalStateException> { required.get().first() }
        assertFailsWith<IllegalStateException> {
            Entry<Int?>(null).required { error("default failed") }.lastValue
        }
    }

    @Test
    fun requiredWithoutDefaultReadsPresentValue() = runTest {
        val entry = Entry<Int?>(3).required()
        assertEquals(3, entry.lastValue)
        assertEquals(3, entry.get().first())
    }

    @Test
    fun defaultDoesNotPersistFallback() = runTest {
        val entry = Entry<String?>(null)
        val wrapped = entry.default("fallback")
        assertEquals("fallback", wrapped.lastValue)
        assertEquals("fallback", wrapped.get().first())
        assertNull(entry.lastValue)
        wrapped.set("saved", this)
        assertEquals("saved", wrapped.lastValue)
        assertEquals("saved", wrapped.get().first())
    }

    @Test
    fun nullDefaultStillEmitsNull() = runTest {
        assertNull(Entry<String?>(null).default { null }.get().first())
        assertNull(Entry<String?>(null).default { error("default failed") }.lastValue)
    }

    @Test
    fun mappingTransformsBothDirections() = runTest {
        val entry = Entry(2)
        val mapped = entry.map(Int::toString, String::toInt)
        assertEquals("2", mapped.lastValue)
        assertEquals("2", mapped.get().first())
        mapped.set("13", this)
        assertEquals(13, entry.lastValue)
        mapped.set("invalid", this)
        assertEquals(13, entry.lastValue)
    }

    @Test
    fun mappingCanClearNullableSource() = runTest {
        val entry = Entry<String?>("42")
        val mapped = entry.map({ it?.toInt() }, { it?.toString() })
        mapped.set(null, this)
        assertNull(entry.lastValue)
        assertNull(mapped.get().first())
    }

    @Test
    fun mappingReadErrorsAreObservable() = runTest {
        val mapped = Entry("invalid").map(String::toInt, Int::toString)
        assertFailsWith<NumberFormatException> { mapped.lastValue }
        assertFailsWith<NumberFormatException> { mapped.get().first() }
    }

    private class Entry<T>(initial: T, private val source: Flow<T>? = null) : KeyValue<T>() {
        private val values = MutableStateFlow(initial)
        override var lastValue: T
            get() = values.value
            set(value) {
                values.value = value
            }
        override fun get(): Flow<T> = source ?: values
        override fun set(value: T, scope: CoroutineScope) {
            lastValue = value
        }
    }
}
