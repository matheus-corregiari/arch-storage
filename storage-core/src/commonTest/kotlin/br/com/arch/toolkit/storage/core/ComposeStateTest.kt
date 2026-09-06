package br.com.arch.toolkit.storage.core

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotApplyResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ComposeStateTest {
    @Test
    fun conflictingSnapshotApplicationDoesNotWriteToTheBackend() = runTest {
        val entry = Entry()
        val recomposer = Recomposer(coroutineContext)
        val composition = Composition(EmptyApplier(), recomposer)
        lateinit var state: MutableState<String>
        try {
            composition.setContent { state = entry.state(this) }
            val snapshot = Snapshot.takeMutableSnapshot()
            try {
                snapshot.enter { state.value = "child" }
                state.value = "parent"
                val writes = entry.writes
                kotlin.test.assertTrue(
                    snapshot.apply() is SnapshotApplyResult.Failure
                )
                assertEquals(writes, entry.writes)
                assertEquals("parent", entry.lastValue)
            } finally {
                snapshot.dispose()
            }
        } finally {
            composition.dispose()
            recomposer.cancel()
        }
    }

    @Test
    fun compositionObservesChangesRetainsCollectionAndDisposesIt() = runTest {
        val clock = BroadcastFrameClock()
        val recomposer = Recomposer(coroutineContext + clock)
        val composition = Composition(EmptyApplier(), recomposer)
        backgroundScope.launch(clock) { recomposer.runRecomposeAndApplyChanges() }
        val selected = androidx.compose.runtime.mutableStateOf(Entry())
        val selectedScope = androidx.compose.runtime.mutableStateOf<CoroutineScope>(this)
        lateinit var state: MutableState<String>
        try {
            composition.setContent { state = selected.value.state(selectedScope.value) }
            pump(clock)
            val first = selected.value
            first.set("external", this)
            pump(clock)
            assertEquals("external", state.value)
            assertEquals(1, first.collections)
            val second = Entry()
            second.set("external", this)
            selected.value = second
            pump(clock)
            val (value, write) = state
            assertEquals("external", value)
            write("second")
            assertEquals("external", first.lastValue)
            assertEquals("second", second.lastValue)
            assertEquals(0, first.activeCollectors)
            selectedScope.value = backgroundScope
            pump(clock)
            state.value = "new scope"
            kotlin.test.assertSame(backgroundScope, second.writeScope)
            assertEquals(1, second.collections)
        } finally {
            composition.dispose()
            recomposer.cancel()
            runCurrent()
        }
        assertEquals(0, selected.value.activeCollectors)
    }

    private fun TestScope.pump(clock: BroadcastFrameClock) {
        repeat(3) {
            Snapshot.sendApplyNotifications()
            runCurrent()
            clock.sendFrame(testScheduler.currentTime * 1_000_000)
            runCurrent()
        }
    }

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
            assertEquals(2, entry.writes)
            val snapshot = Snapshot.takeMutableSnapshot()
            try {
                snapshot.enter { state.value = "snapshot" }
                assertEquals(3, entry.writes)
                snapshot.apply().check()
                assertEquals(3, entry.writes)
            } finally {
                snapshot.dispose()
            }
        } finally {
            composition.dispose()
            recomposer.cancel()
        }
    }

    @Test
    fun switchingEntryAndScopeRoutesWritesToCurrentEntry() = runTest {
        val first = Entry()
        val second = Entry()
        val recomposer = Recomposer(coroutineContext)
        val composition = Composition(EmptyApplier(), recomposer)
        lateinit var state: MutableState<String>
        try {
            composition.setContent { state = first.state(this) }
            composition.setContent { state = second.state(backgroundScope) }
            state.value = "second"
            assertEquals("initial", first.lastValue)
            assertEquals("second", second.lastValue)
            kotlin.test.assertSame(backgroundScope, second.writeScope)
        } finally {
            composition.dispose()
            recomposer.cancel()
        }
    }

    private class Entry : KeyValue<String>() {
        var collections = 0
        var activeCollectors = 0
        var writes = 0
        var writeScope: CoroutineScope? = null
        private val values = MutableStateFlow("initial")
        override var lastValue: String
            get() = values.value
            set(value) {
                values.value = value
            }
        override fun get() = flow {
            collections++
            activeCollectors++
            try {
                emitAll(values)
            } finally {
                activeCollectors--
            }
        }
        override fun set(value: String, scope: CoroutineScope) {
            writes++
            writeScope = scope
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
