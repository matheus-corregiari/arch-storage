# Core concepts

## Providers and entries

`StorageProvider` creates entries for Boolean, ByteArray, Double, Float, Int, Long and String,
plus enum and model helpers. `KeyValue<T>` exposes `get(): Flow<T>`, `set(value, scope)`,
`setAndAwait(value)`, `lastValue`, `current()`, `instant()`, property delegates and Compose state.

| Operation | Semantics |
|---|---|
| `get()` | Observe the backend; absent primitive keys emit null |
| `set(value, scope)` | Memory updates immediately; DataStore schedules an asynchronous write |
| `setAndAwait(value)` | Suspends until backend completion; conversion/backend errors and cancellation propagate |
| `get().first()` | Waits for an observed emission, including null; read errors propagate without cache fallback |
| `set(null, scope)` | Clears a nullable entry; DataStore removes the preference |
| `lastValue` | Cached value; a new DataStore entry starts at null until read or written |
| `current()` | Attempts a Flow read for 50 ms, falling back to the cached value on timeout/error |
| `instant()` | Blocking `current()` on JVM/Android/iOS; cached value only on JS/Wasm |

`current()` and `instant()` are convenience reads, not persistence acknowledgements. Avoid blocking
reads on UI threads. Use a caller-owned scope and Flow for lifecycle-sensitive work.
Cancellation propagates. A valid null emission replaces stale cache; only an absent emission,
ordinary failure or timeout uses the fallback. The timeout is cooperative and cannot interrupt
blocking synchronous code.
For acknowledged writes, use `entry.setAndAwait(value)` inside your coroutine. Memory updates
shared state; DataStore waits for `edit` to return after persistence, following the
[AndroidX DataStore contract](https://developer.android.com/reference/kotlin/androidx/datastore/core/DataStore). This is not a guarantee against
hardware failure, a transaction across entries, or proof that collectors have processed the update.
Cancellation can race with backend completion: a cancelled call does not prove the write was rolled
back. Other writers may replace the value before the next read. `lastValue` is a cache, not a receipt.

Use `entry.get().first()` (import `kotlinx.coroutines.flow.first`) when you need an observed value
without the 50 ms fallback. It can suspend indefinitely; add your own timeout when needed. An observed
value is not necessarily the latest concurrent write. Defaults and mappings also apply to observations.

`setAndAwait` runs in the caller's context, independently of the scope/pending job used by `set`.
It neither cancels nor is cancelled by the legacy latest-pending-write policy. Avoid mixing concurrent
writes when order matters; await sequential calls. Custom `KeyValue` backends must override
`setAndAwait`; its default throws `UnsupportedOperationException` without scheduling a write.

DataStore cancels the previous pending write on the same entry when another write is submitted;
do not use repeated `set` calls as an atomic increment or transaction API.

## Defaults and required values

Import helpers from `KeyValue.Companion`:

```kotlin
import br.com.arch.toolkit.storage.core.KeyValue.Companion.default
import br.com.arch.toolkit.storage.core.KeyValue.Companion.required

// Given a StorageProvider named provider:
// val name = provider.string("name").default("Guest")
// val count = provider.int("count").required { 0 }
```

Defaults do not persist themselves. `default` preserves nullable values and emits null when
both source and fallback are null. `required()` without a default fails when the value is absent;
`required { value }` supplies a non-null fallback. A throwing fallback is treated as unavailable.
Cancellation is the exception: it propagates. Synchronous default suppliers have no interrupting
timeout. `KeyValue<Int>.set(null)` does not compile.

## Bidirectional mapping

`entry.map(mapTo, mapBack)` converts values on read and write. A mapped null is forwarded to
the backend, allowing nullable models to be deleted. Read conversion failures propagate through
Flow; write conversion failures leave the source unchanged. `set` discards ordinary conversion errors;
`setAndAwait` propagates them to its caller. Cancellation propagates in both APIs. Defaults are useful for missing values,
but do not automatically recover malformed JSON or arbitrary conversion errors.

## Serialization

`provider.model<T>(key)` uses kotlinx.serialization. Apply Kotlin's serialization compiler plugin
and mark custom models `@Serializable`. Built-in types such as `List<Int>` already have serializers.
Alternatively supply `fromJson` and `toJson` lambdas. DataStore stores model JSON in string preferences;
memory stores the object directly. DataStore enums are stored by name, read case-insensitively and
fall back to the supplied enum value for an unknown name.

Pass `json = ...` per model when possible. `StorageProvider.json(...)` changes the global default
for subsequent model entries. Its initial configuration ignores unknown keys, encodes defaults and
pretty-prints JSON. Storage provides no encryption layer.

## Compose and concurrency

`entry.state(scope)` observes one remembered Flow per entry. Replacing the entry or scope redirects
subsequent assignments; disposing the composition stops collection. Assignments update local state
and schedule the backend write. Snapshot application and equality comparisons do not write again.

DataStore cache visibility is protected across threads, and pending jobs are replaced atomically per
entry instance. An edit must complete successfully before its value is cached. Distinct entries for
the same key remain independent writers; this is not a transaction or a compare-and-set API.
