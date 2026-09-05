# Migrating from 2.0.0-rc16 to 1.0.0

`1.0.0` is the first stable release prepared in the independent Arch Storage repository.
The coordinates remain `io.github.matheus-corregiari:storage-*` and packages remain
`br.com.arch.toolkit.storage.*`. This candidate is not published until the release workflow completes.

## Version selection

The version sequence restarts at `1.0.0`. Gradle considers `2.0.0-rc16` higher than `1.0.0`,
so merely changing one dependency may leave an RC selected by another dependency.
Inspect the resolved version in the consuming application's configuration:

```sh
./gradlew dependencyInsight --dependency storage-core --configuration runtimeClasspath
```

For Android use the relevant variant, such as `debugRuntimeClasspath`; KMP applications should use
the target's resolvable configuration. Coordinate upgrades of libraries that still request the RCs.
An explicit strict constraint detects incompatible requirements instead of silently selecting an RC:

```kotlin
dependencies {
    implementation("io.github.matheus-corregiari:storage-memory:1.0.0")
    constraints {
        implementation("io.github.matheus-corregiari:storage-core") {
            version { strictly("1.0.0") }
            because("All storage modules must use the independent stable release")
        }
    }
}
```

Apply equivalent constraints to any other storage modules in your dependency graph. Resolve conflicts
with their owners; do not apply a blanket `force` that can hide incompatible transitive requirements.

## Platform and dependency changes

Android now requires API 23. Supported targets are Android, JVM, JavaScript, WasmJS, iOS ARM64 and
iOS Simulator ARM64. Intel iOS simulators (`iosX64`) are removed from the source build; RC16 did not
publish that variant. DataStore remains unavailable on the web; use the memory backend there.
Compose stays in core and moves to 1.12.0. Arch Lumber moves to 1.4.0.

## Behavioral corrections

- Cancellation propagates through `current()` and adapter conversions/defaults.
- A first `null` emission overrides stale cache. An empty, failed or timed-out read still falls back
  to cache after at most 50 ms of cooperative suspension. Synchronous blocking code cannot be
  interrupted by this timeout; default suppliers are ordinary synchronous functions.
- Compose state follows the selected entry and scope. Snapshot comparisons no longer trigger writes.
  Assignments update local state and call `set`; applying a snapshot does not write again.
- DataStore atomically replaces pending write jobs per entry instance. Separate entries with the
  same key do not cancel one another. `set` still returns `Unit` immediately and is not a persistence
  acknowledgement. The cache changes only after a successful edit or observed persisted value.
- Read and persistence failures are logged at the backend boundary. Flow read failures remain
  observable; cancellation is not logged as a successful write.
- Defaults are not persisted. Nullable mapped writes remove values. Ordinary write conversion errors
  preserve the source; read conversion errors remain visible. Cancellation is always propagated.

Memory retains model objects and the caller's map. It does not copy mutable objects or make concurrent
entry creation safe. Use consistent key types and synchronize access to the caller-owned map.

## Reproducible consumer and persistence check

The standalone `tools/release-consumer` build compiles against published RC16 without source-project
substitution, writes primitives, enum names and JSON to a real DataStore file, then compiles against
locally published `1.0.0` to read the same file and exercise adapters and delegates.
RC16 requires an explicit `storage-core` dependency on the consumer's compile classpath;
`1.0.0` exposes it transitively from both backends. Public JVM signatures are compared with
`python tools/compare_release_api.py` after resolving RC16 and publishing locally; JDK `javap`
must be on `PATH`.

```sh
./gradlew ciPublishLocal -PreleaseVersion=1.0.0
./gradlew -p tools/release-consumer run -PstorageVersion=2.0.0-rc16 \
  --args="write /absolute/path/rc16.preferences_pb"
./gradlew -p tools/release-consumer run -PstorageVersion=1.0.0 \
  --args="read /absolute/path/rc16.preferences_pb"
```

Use a fresh fixture file for each run. In PowerShell quote arguments such as
`'-PreleaseVersion=1.0.0'` and `'-PstorageVersion=2.0.0-rc16'`.
