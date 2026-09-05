# Arch Storage

Reactive, type-safe key-value storage for Kotlin Multiplatform, extracted from Arch Toolkit.
Use the same entry API for temporary in-memory values and persistent DataStore preferences.

[![CI](https://github.com/matheus-corregiari/arch-storage/actions/workflows/ci.yml/badge.svg)](https://github.com/matheus-corregiari/arch-storage/actions/workflows/ci.yml)
[![Coverage](https://codecov.io/gh/matheus-corregiari/arch-storage/graph/badge.svg)](https://codecov.io/gh/matheus-corregiari/arch-storage)

This checkout is the initial extraction. No independent Arch Storage release has been published.
Repository, documentation and badge URLs are prepared for the future GitHub repository.

## Modules

| Artifact | Responsibility | Platforms |
|---|---|---|
| `storage-core` | `StorageProvider`, `KeyValue`, Flow, defaults, mapping, JSON and Compose state | Android, JVM, iOS, JS, WasmJS |
| `storage-memory` | Immediate, in-memory values backed by StateFlow | Android, JVM, iOS, JS, WasmJS |
| `storage-datastore` | Persistent AndroidX DataStore Preferences backend | Android, JVM, iOS; JS/Wasm factory throws |

Coordinates retain the group `io.github.matheus-corregiari`. Packages remain under
`br.com.arch.toolkit.storage`. The modules share one release version.

## Quick example

```kotlin
import br.com.arch.toolkit.storage.core.KeyValue.Companion.required
import br.com.arch.toolkit.storage.memory.MemoryStoreProvider

val storage = MemoryStoreProvider(mutableMapOf())
val visits = storage.int("visits").required { 0 }
visits.set(1)
// In a coroutine:
// visits.get().collect { count -> println(count) }
```

Memory writes are immediate. DataStore writes are asynchronous; observe `get()` to await a
persisted value. `current()` uses a short timeout and can return the cached value instead.
Memory values do not survive process termination.

## Build and quality

Use JDK 21, the checked-in Gradle Wrapper, and Android SDK 37/build tools 37.0.0.
Set `ANDROID_HOME` to your SDK installation. The minimum Android API is 20.
Browser tests require Chrome; Apple compilation and simulator tests require macOS and Xcode.

```sh
./gradlew ciBuild ciCoverage
./gradlew ciLint ciDocs ciPublicationManifest
python -m unittest discover -s .github/scripts -p 'test_*.py'
python -m pip install -r .github/requirements-docs.txt
python -m mkdocs build --strict
```

Use `gradlew.bat` in PowerShell. Coverage gates require at least 65% of lines, instructions and
branches, both per module and in aggregate. No Git checkout or publishing credentials are needed
for local build/test/docs. The local version defaults to `0.0.0-SNAPSHOT`; override with
`-PreleaseVersion=...` or the CI-generated `build/version-name.txt`.

## Documentation

- [Getting started](docs/getting-started.md): dependencies and provider setup.
- [Core concepts](docs/core-concepts.md): entries, nullability, mapping and lifecycle.
- [Recipes](docs/recipes.md): preferences, JSON models and Compose.
- [CI and releases](docs/ci.md): gates, secrets and publication workflow.
- [Changelog](docs/changelog/index.md): extraction changes.
- [Contributing](CONTRIBUTING.md).
- [Local validation record](VALIDATION.md): checks, coverage and platform limits.

Licensed under [Apache 2.0](LICENSE).
