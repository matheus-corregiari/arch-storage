# Ecosystem

Arch Storage owns reactive key-value storage. Its sources were extracted from Arch Toolkit;
the original checkout is left unchanged during this preparation.

- [Arch Lumber](https://github.com/matheus-corregiari/arch-lumber) supplies the project conventions
  and the logging dependency used by the DataStore backend.
- [Arch Toolkit](https://github.com/matheus-corregiari/arch-toolkit) contains the original storage
  modules and integration sample.
- [Arch Event Observer](https://github.com/matheus-corregiari/arch-event-observer) handles reactive
  result/event observation and is not a storage dependency.
- [Arch Android](https://github.com/matheus-corregiari/arch-android) provides Android-specific utilities.

Core does not depend on Lumber, Event Observer, Splinter, state-handle or Koin. Memory depends on
core. DataStore depends on core, AndroidX DataStore and Lumber.

## Migration

Packages and module artifact names are retained. Existing imports continue to use
`br.com.arch.toolkit.storage.*`. In a composite source setup, replace
`:toolkit:multi:storage-core` with `:storage-core`, and similarly for the two backends.
Public dependency coordinates retain `io.github.matheus-corregiari:storage-*`.
Choose a coordinated, unused release version before publishing these existing coordinates;
the initial GitHub release policy does not check historical Maven versions from Arch Toolkit.
