# Arch Storage

Arch Storage gives Kotlin Multiplatform applications a shared API for reactive preferences and
temporary values. The project keeps the build, CI, coverage and documentation conventions of
Arch Lumber, with three storage modules extracted from Arch Toolkit.

| Module | What it provides |
|---|---|
| storage-core | Typed entries, Flow, mapping, defaults, JSON helpers and Compose integration |
| storage-memory | StateFlow-backed, process-local storage |
| storage-datastore | Persistent AndroidX DataStore Preferences adapter |

Start with [installation and setup](getting-started.md), then see [concepts](core-concepts.md)
and [recipes](recipes.md). API references are generated separately for
[core](api/storage-core/index.html), [memory](api/storage-memory/index.html) and
[DataStore](api/storage-datastore/index.html).

## Platform support

Core and memory target Android, JVM, iOS (device and simulators), JavaScript and WasmJS.
DataStore persistence is available on Android, JVM and iOS. Its JS/Wasm factory deliberately
throws `IllegalStateException`: use `MemoryStoreProvider` there for nonpersistent values.
Native macOS artifacts are not configured. A desktop JVM application can run on macOS.

This is an unreleased extraction; the prepared GitHub URLs become active when the repository
is created. See [the changelog](changelog/index.md) for changes made during extraction.
