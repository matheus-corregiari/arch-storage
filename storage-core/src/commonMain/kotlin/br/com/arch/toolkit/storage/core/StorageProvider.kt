package br.com.arch.toolkit.storage.core

import br.com.arch.toolkit.storage.core.KeyValue.Companion.map
import br.com.arch.toolkit.storage.core.KeyValue.Companion.required
import br.com.arch.toolkit.storage.core.StorageProvider.Defaults.defaultJson
import kotlinx.serialization.json.Json
import kotlin.enums.EnumEntries
import kotlin.enums.enumEntries

/**
 * Abstract factory for creating [KeyValue] entries across different types.
 *
 * A [StorageProvider] defines the contract for storage backends. Concrete
 * implementations (e.g., DataStore, in-memory, file-based) must implement this class
 * to provide type-safe access to persisted or cached values.
 *
 * ---
 *
 * ### Core responsibilities
 * - Expose [KeyValue] entries for primitive types (Boolean, Int, String, etc.).
 * - Provide helpers for working with [Enum] values and serializable models.
 * - Handle serialization/deserialization with configurable [Json].
 *
 * ---
 *
 * ### Example: Using primitive keys
 * ```kotlin
 * val provider: StorageProvider = DataStoreProvider(store)
 *
 * val isLoggedIn: KeyValue<Boolean?> = provider.boolean("is_logged_in")
 * val userName: KeyValue<String?> = provider.string("user_name")
 *
 * isLoggedIn.set(true)
 * println("User: ${userName.instant()}")
 * ```
 *
 * ### Example: Enum support
 * ```kotlin
 * enum class Theme { Light, Dark }
 *
 * val theme: KeyValue<Theme> = provider.enum("theme", Theme.entries, Theme.Light)
 *
 * theme.set(Theme.Dark)
 * println("Theme is now ${theme.instant()}")
 * ```
 *
 * ### Example: Model serialization
 * ```kotlin
 * @Serializable
 * data class User(val id: String, val name: String)
 *
 * val user: KeyValue<User?> = provider.model("user")
 *
 * user.set(User("42", "Alice"))
 *
 * lifecycleScope.launch {
 *     user.get().collect { println("Current user: $it") }
 * }
 * ```
 *
 * ---
 *
 * ### Default JSON configuration
 * By default, [model] serialization uses [defaultJson], which is preconfigured with:
 * - `ignoreUnknownKeys = true`
 * - `encodeDefaults = true`
 * - `prettyPrint = true`
 *
 * You can override this globally:
 * ```kotlin
 * StorageProvider.json(Json { ignoreUnknownKeys = false })
 * ```
 *
 * @see KeyValue For the reactive entry abstraction.
 * @see KeyValue.required To enforce non-null values.
 * @see KeyValue.map To transform between types.
 */
@Suppress("TooManyFunctions")
abstract class StorageProvider {

    /** Returns a reactive nullable Boolean entry identified by [key]. */
    abstract fun boolean(key: String): KeyValue<Boolean?>

    /** Returns a reactive nullable byte array entry identified by [key]. */
    abstract fun byteArray(key: String): KeyValue<ByteArray?>

    /** Returns a reactive nullable Double entry identified by [key]. */
    abstract fun double(key: String): KeyValue<Double?>

    /** Returns a reactive nullable Float entry identified by [key]. */
    abstract fun float(key: String): KeyValue<Float?>

    /** Returns a reactive nullable Int entry identified by [key]. */
    abstract fun int(key: String): KeyValue<Int?>

    /** Returns a reactive nullable Long entry identified by [key]. */
    abstract fun long(key: String): KeyValue<Long?>

    /** Returns a reactive nullable String entry identified by [key]. */
    abstract fun string(key: String): KeyValue<String?>

    /** Returns an enum entry, using [default] when no matching value is available. */
    abstract fun <T : Enum<T>> enum(key: String, entries: EnumEntries<T>, default: T): KeyValue<T>

    /** Returns a model entry; persistent backends use [fromJson] and [toJson] for conversion. */
    abstract fun <T : Any> model(
        key: String,
        fromJson: (String) -> T,
        toJson: (T) -> String
    ): KeyValue<T?>

    /** Returns a model entry using the serializer for [T] and the supplied [json] configuration. */
    inline fun <reified T : Any> model(
        key: String,
        json: Json = defaultJson
    ) = model<T>(key, json::decodeFromString, json::encodeToString)

    /** Returns a typed enum entry using all entries of [T] and the supplied fallback. */
    inline fun <reified T : Enum<T>> enum(
        key: String,
        default: T
    ) = enum(key, enumEntries<T>(), default)

    /** Default serialization settings for subsequently created model entries. */
    companion object Defaults {
        /** Global JSON configuration; prefer a per-entry configuration for independent stores. */
        var defaultJson: Json
            private set

        init {
            defaultJson = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                prettyPrint = true
            }
        }

        /** Changes the default JSON configuration for subsequently created model entries. */
        fun json(json: Json) = apply { defaultJson = json }
    }
}
