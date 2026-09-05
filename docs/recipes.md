# Recipes

## Observe a setting

```kotlin
import br.com.arch.toolkit.storage.core.StorageProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun observeName(provider: StorageProvider, scope: CoroutineScope) = scope.launch {
    provider.string("name").get().collect { name -> println(name ?: "Guest") }
}
```

Cancel the returned job or owning scope when the observer is no longer needed.

## Store a JSON model

```kotlin
import br.com.arch.toolkit.storage.core.StorageProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable

@Serializable
data class Profile(val name: String, val age: Int)

fun saveProfile(provider: StorageProvider, scope: CoroutineScope) {
    provider.model<Profile>("profile").set(Profile("Alice", 30), scope)
}
```

Use the same entry and `set(null, scope)` to remove the model. DataStore serialization callbacks
run as part of mapping; invalid persisted JSON is an observable read error.

## Bind to Compose

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import br.com.arch.toolkit.storage.core.KeyValue
import br.com.arch.toolkit.storage.core.KeyValue.Companion.required

@Composable
fun nameState(entry: KeyValue<String?>): MutableState<String> {
    val scope = rememberCoroutineScope()
    return entry.required { "" }.state(scope)
}
```

Create and retain the underlying entry outside repeated recompositions. Assigning to the returned
state writes through to storage; observed backend values drive subsequent state updates.

## Use property delegation

```kotlin
import br.com.arch.toolkit.storage.core.KeyValue.Companion.required
import br.com.arch.toolkit.storage.memory.MemoryStoreProvider

class Session {
    private val storage = MemoryStoreProvider(mutableMapOf())
    var count by storage.int("count").required { 0 }.delegate()
}
```

Delegates use `instant()` and therefore have platform-specific read behavior. Prefer Flow for
persistent settings. For unit tests, inject a fresh memory provider per test; use a real temporary
DataStore file when testing serialization and persistence across store recreation.
