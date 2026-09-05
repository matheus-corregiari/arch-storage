package br.com.arch.toolkit.storage.core

import br.com.arch.toolkit.storage.core.KeyValue.Companion.map
import br.com.arch.toolkit.storage.core.KeyValue.Companion.required
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.enums.EnumEntries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StorageProviderTest {
    @Test
    fun defaultJsonAndExplicitJsonRoundTripBuiltInModels() = runTest {
        val provider = Provider()
        val defaultJson = StorageProvider.defaultJson
        assertTrue(defaultJson.configuration.ignoreUnknownKeys)
        assertTrue(defaultJson.configuration.encodeDefaults)
        assertTrue(defaultJson.configuration.prettyPrint)
        val customJson = Json { prettyPrint = false }
        try {
            StorageProvider.json(customJson)
            assertSame(customJson, StorageProvider.defaultJson)
            val entry = provider.model<List<Int>>("numbers")
            entry.set(listOf(1, 2), this)
            assertEquals("[1,2]", provider.string("numbers").get().first())
            assertEquals(listOf(1, 2), entry.get().first())
            val pretty = provider.model<List<Int>>("pretty", defaultJson)
            pretty.set(listOf(3), this)
            assertTrue(provider.string("pretty").get().first().orEmpty().contains('\n'))
            entry.set(null, this)
            assertNull(entry.get().first())
        } finally {
            StorageProvider.json(defaultJson)
        }
    }

    @Test
    fun reifiedEnumUsesEntriesAndFallback() = runTest {
        val provider = Provider()
        val entry = provider.enum("mode", Mode.Light)
        assertEquals(Mode.Light, entry.get().first())
        entry.set(Mode.Dark, this)
        assertEquals("Dark", provider.string("mode").get().first())
        assertEquals(Mode.Dark, entry.get().first())
    }

    private enum class Mode { Light, Dark }

    private class Provider : StorageProvider() {
        private val strings = mutableMapOf<String, Entry<String?>>()
        override fun string(key: String): KeyValue<String?> = strings.getOrPut(key) { Entry(null) }
        override fun boolean(key: String): KeyValue<Boolean?> = Entry(null)
        override fun byteArray(key: String): KeyValue<ByteArray?> = Entry(null)
        override fun double(key: String): KeyValue<Double?> = Entry(null)
        override fun float(key: String): KeyValue<Float?> = Entry(null)
        override fun int(key: String): KeyValue<Int?> = Entry(null)
        override fun long(key: String): KeyValue<Long?> = Entry(null)
        override fun <T : Enum<T>> enum(
            key: String,
            entries: EnumEntries<T>,
            default: T
        ): KeyValue<T> =
            string(
                key
            ).map({ name -> entries.find { it.name == name } }, { it?.name }).required { default }
        override fun <T : Any> model(
            key: String,
            fromJson: (String) -> T,
            toJson: (T) -> String
        ): KeyValue<T?> =
            string(key).map({ it?.let(fromJson) }, { it?.let(toJson) })
    }

    private class Entry<T>(initial: T) : KeyValue<T>() {
        private val values = MutableStateFlow(initial)
        override var lastValue: T
            get() = values.value
            set(value) {
                values.value = value
            }
        override fun get() = values
        override fun set(value: T, scope: CoroutineScope) {
            lastValue = value
        }
    }
}
