# Getting started

## Dependencies

For local consumption, run `./gradlew ciPublishLocal`. The development version defaults to
`0.0.0-SNAPSHOT`; configure `mavenLocal()` in the consuming project's repositories.
An independent public release has not been selected or published yet.

```kotlin
kotlin {
    sourceSets.commonMain.dependencies {
        implementation("io.github.matheus-corregiari:storage-memory:0.0.0-SNAPSHOT")
        implementation("io.github.matheus-corregiari:storage-datastore:0.0.0-SNAPSHOT")
    }
}
```

Both backends expose `storage-core` transitively. Use only core to implement your own provider.
All three modules use the same version. Compose runtime remains part of core because `KeyValue.state()`
is part of the existing API. No DI framework is required.

## In-memory provider

```kotlin
import br.com.arch.toolkit.storage.memory.MemoryStoreProvider

val provider = MemoryStoreProvider(mutableMapOf())
val name = provider.string("name")
name.set("Alice")
```

Reuse the provider (or its backing map) to share keys. Separate maps are independent stores.
Use a consistent type for each key. The backing map is caller-owned and is not a concurrent map;
coordinate concurrent entry creation yourself. Memory models are retained as objects, without JSON conversion.

## Persistent provider

Create one DataStore instance per file, with a scope owned by your application or feature.
Example for JVM/Android (choose an application-private file on Android):

```kotlin
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import br.com.arch.toolkit.storage.datastore.DataStoreProvider
import kotlinx.coroutines.CoroutineScope
import java.io.File

fun preferences(file: File, scope: CoroutineScope): DataStoreProvider {
    val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
    return DataStoreProvider(dataStore)
}
```

The file name must end in `.preferences_pb`. On iOS use
`PreferenceDataStoreFactory.createWithPath(scope = scope, produceFile = { path.toPath() })`
with an application-writable absolute path and `okio.Path.Companion.toPath`.
The backend accepts an existing `DataStore<Preferences>`; file location, migrations and scope
ownership stay with the caller.

Pass a live coroutine scope when writing: `entry.set(value, scope)`. Observe `entry.get()` to
confirm a value. Cancelling the scope may cancel pending writes. Never construct the unsupported
JS/Wasm DataStore factory; select the memory backend in platform-specific setup instead.
