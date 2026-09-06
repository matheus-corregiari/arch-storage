# Changelog

## 1.0.0

- Preserve the public storage API and artifact coordinates from `2.0.0-rc16` in the
  first independent stable release. See [migration](../migration-1.0.0.md) for Gradle version ordering.
- Propagate coroutine cancellation through convenience reads, defaults and mapping.
- Preserve valid null reads over stale cache and retain cooperative 50 ms read fallback.
- Correct Compose entry/scope changes, remember observed flows and write only from state setters.
- Replace DataStore pending jobs atomically per entry and cache writes only after successful edits.
- Add concurrent writer, failed persistence, Compose lifecycle, mutable memory and adapter tests.
- Raise shared coverage floors to 90% lines, 85% instructions and 80% branches without exclusions.
- Upgrade Compose to 1.12.0, Arch Lumber to 1.4.0, JaCoCo to 0.8.15 and MkDocs Material to 9.7.7.
- Require Android API 23; remove the unused `iosX64` target and AtomicFU configuration.
- Verify the Gradle Wrapper checksum, make Maven Local opt-in and prioritize explicit release versions.
- Match publication gates to all CodeQL matrix jobs and CodeQL Policy; align scheduled analysis.
- Preserve remote signing in combined local/remote invocations and fail API docs on warnings.
- Validate local release artifacts, publication metadata and public dependencies.

### Initial extraction included in this release

- Extracted `storage-core`, `storage-memory` and `storage-datastore` from Arch Toolkit.
- Adopted Arch Lumber's Gradle conventions, CI/release workflows, quality gates and documentation structure.
- Preserved storage package names and artifact names; exposed public API dependencies transitively.
- Added contract tests and a real DataStore file recreation test.
- Fixed mapped nullable writes so clearing a model removes its underlying value.
- Fixed nullable defaults so a missing value can emit null instead of being dropped.
- Added independent API documentation output directories for all three modules.
- Added a local snapshot version that does not require Git.

Arch Lumber's historical releases do not apply to this project.
