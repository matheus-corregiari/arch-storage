# Changelog

## Unreleased

- Extracted `storage-core`, `storage-memory` and `storage-datastore` from Arch Toolkit.
- Adopted Arch Lumber's Gradle conventions, CI/release workflows, quality gates and documentation structure.
- Preserved storage package names and artifact names; exposed public API dependencies transitively.
- Added contract tests and a real DataStore file recreation test.
- Fixed mapped nullable writes so clearing a model removes its underlying value.
- Fixed nullable defaults so a missing value can emit null instead of being dropped.
- Added independent API documentation output directories for all three modules.
- Added a local snapshot version that does not require Git.

No independent release is recorded yet. Arch Lumber's historical releases do not apply to this project.
