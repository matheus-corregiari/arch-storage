package br.com.arch.toolkit.storage.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import br.com.arch.toolkit.lumber.Lumber
import br.com.arch.toolkit.storage.core.KeyValue
import br.com.arch.toolkit.storage.datastore.DataStoreKeyValue.BooleanKV
import br.com.arch.toolkit.storage.datastore.DataStoreKeyValue.ByteArrayKV
import br.com.arch.toolkit.storage.datastore.DataStoreKeyValue.DoubleKV
import br.com.arch.toolkit.storage.datastore.DataStoreKeyValue.FloatKV
import br.com.arch.toolkit.storage.datastore.DataStoreKeyValue.IntKV
import br.com.arch.toolkit.storage.datastore.DataStoreKeyValue.LongKV
import br.com.arch.toolkit.storage.datastore.DataStoreKeyValue.StringKV
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * [KeyValue] implementation backed by AndroidX [DataStore] with [Preferences].
 *
 * This sealed class adapts primitive preference keys into reactive [KeyValue] entries.
 * Each subclass corresponds to a supported [Preferences.Key] type.
 *
 * ---
 *
 * ### Behavior
 * - **Read:** Values are exposed as a [Flow] via [get]. Null if the key is not set.
 * - **Write:** Updates are performed inside [DataStore.edit], replacing or removing the key.
 * - **Cache:** The last successfully read or written value is stored in [lastValue].
 * - **Concurrency:** The latest pending write wins per entry instance (previous is cancelled).
 * - **Errors:** Failures are logged via [Lumber].
 *
 * ---
 *
 * ### Example
 * ```kotlin
 * val store: DataStore<Preferences> = ...
 *
 * val userName = DataStoreKeyValue.StringKV("user_name", store)
 *
 * // Reactive
 * scope.launch {
 *     userName.get().collect { println("User: $it") }
 * }
 *
 * // Update
 * userName.set("Alice", scope)
 * ```
 *
 * ---
 *
 * ### Subclasses
 * - [BooleanKV] → `booleanPreferencesKey`
 * - [ByteArrayKV] → `byteArrayPreferencesKey`
 * - [DoubleKV] → `doublePreferencesKey`
 * - [FloatKV] → `floatPreferencesKey`
 * - [IntKV] → `intPreferencesKey`
 * - [LongKV] → `longPreferencesKey`
 * - [StringKV] → `stringPreferencesKey`
 *
 * ---
 *
 * @param Result Type of the stored value.
 * @property key The [Preferences.Key] associated with this entry.
 * @property store The [DataStore] used to persist and read values.
 *
 * @see DataStoreProvider For the factory that exposes these keys through `StorageProvider`.
 */
@OptIn(ExperimentalAtomicApi::class)
internal sealed class DataStoreKeyValue<Result>(
    private val key: Preferences.Key<Result>,
    private val store: DataStore<Preferences>
) : KeyValue<Result?>() {

    private val cached = AtomicReference<Result?>(null)
    override var lastValue: Result?
        get() = cached.load()
        set(value) = cached.store(value)
    private val job = AtomicReference<Job>(Job().apply { complete() })

    override fun get(): Flow<Result?> = store.data.map { preferences ->
        preferences[key].also { lastValue = it }
    }.catch {
        if (it !is CancellationException) Lumber.tag("DataStore - get").error(it)
        throw it
    }

    override fun set(value: Result?, scope: CoroutineScope) {
        val next = scope.launch(start = CoroutineStart.LAZY) {
            runCatching {
                store.edit { pref ->
                    if (value == null) pref.remove(key) else pref[key] = value
                }
                lastValue = value
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                Lumber.tag("DataStore - set").error(failure)
            }
        }
        job.exchange(next).cancel()
        next.start()
    }

    internal class BooleanKV(key: String, store: DataStore<Preferences>) :
        DataStoreKeyValue<Boolean>(key = booleanPreferencesKey(key), store = store)

    internal class ByteArrayKV(key: String, store: DataStore<Preferences>) :
        DataStoreKeyValue<ByteArray>(key = byteArrayPreferencesKey(key), store = store)

    internal class DoubleKV(key: String, store: DataStore<Preferences>) :
        DataStoreKeyValue<Double>(key = doublePreferencesKey(key), store = store)

    internal class FloatKV(key: String, store: DataStore<Preferences>) :
        DataStoreKeyValue<Float>(key = floatPreferencesKey(key), store = store)

    internal class IntKV(key: String, store: DataStore<Preferences>) :
        DataStoreKeyValue<Int>(key = intPreferencesKey(key), store = store)

    internal class LongKV(key: String, store: DataStore<Preferences>) :
        DataStoreKeyValue<Long>(key = longPreferencesKey(key), store = store)

    internal class StringKV(key: String, store: DataStore<Preferences>) :
        DataStoreKeyValue<String>(key = stringPreferencesKey(key), store = store)
}
