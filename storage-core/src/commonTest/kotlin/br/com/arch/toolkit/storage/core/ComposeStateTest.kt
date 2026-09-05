package br.com.arch.toolkit.storage.core

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeStateTest {
    @Test
    fun stateStartsWithCachedValueAndWritesThrough() = runTest {
        val entry = Entry()
        val recomposer = Recomposer(coroutineContext)
        val composition = Composition(EmptyApplier(), recomposer)
        lateinit var state: MutableState<String>
        try {
            composition.setContent { state = entry.state(this) }
            assertEquals("initial", state.value)
            state.value = "changed"
            assertEquals("changed", entry.lastValue)
            state.value = "changed"
            assertEquals("changed", state.value)
        } finally {
            composition.dispose()
            recomposer.cancel()
        }
    }

    private class Entry : KeyValue<String>() {
        private val values = MutableStateFlow("initial")
        override var lastValue: String
            get() = values.value
            set(value) {
                values.value = value
            }
        override fun get() = values
        override fun set(value: String, scope: CoroutineScope) {
            lastValue = value
        }
    }

    private class EmptyApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit
        override fun insertBottomUp(index: Int, instance: Unit) = Unit
        override fun remove(index: Int, count: Int) = Unit
        override fun move(from: Int, to: Int, count: Int) = Unit
        override fun onClear() = Unit
    }
}
