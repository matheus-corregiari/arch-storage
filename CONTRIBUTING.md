# Contributing to Arch Storage

Thanks for taking the time to contribute.

## Quick Start

1. Fork and clone the repo.
2. Use JDK `21`.
3. Build once to warm up Gradle:

```bash
./gradlew ciBuild
```

## Repository Layout

- `storage-core/src/commonMain` -> shared entry API and adapters
- `storage-memory/src/commonMain` -> in-memory backend
- `storage-datastore/src/opMain` -> persistent backend for Android, JVM and iOS
- `*/src/commonTest` and `storage-datastore/src/opTest` -> shared contract tests
- `storage-datastore/src/jvmTest` -> real-file persistence tests
- `docs/` -> published MkDocs content

## Development Workflow

1. Create a branch from `master`.
2. Make changes with KMP in mind. Prefer `commonMain` when possible.
3. Keep KDoc and docs aligned with shipped behavior.
4. Add or update tests when behavior changes.
5. Sync the MkDocs contributing page before building docs.

## Branching and Releases

Any work branch can target another development branch. PRs to `master` must use
`release/X.Y.Z` (next major/minor) or `hotfix/X.Y.Z` (next patch), optionally with `-rcN`.
CI rejects duplicate or historical remote versions. The validated master commit receives an
annotated tag; that tag triggers package publication.

See the [CI and release guide](docs/ci.md) for required checks, runner conventions and recovery.

## Local Validation

```bash
./gradlew ciBuild ciCoverage
./gradlew ciLint ciDocs
python -m pip install -r .github/requirements-docs.txt
python -m mkdocs build --strict
```

Use the project wrapper and toolchain settings when validating changes.

## Documentation Expectations

- Keep examples short, real, and KMP-friendly.
- Explain nullability, persistence, coroutine scope ownership and observable errors.
- Mention platform differences, especially the unsupported JS/Wasm DataStore factory.
- Update README, KDoc, generated API docs under `docs/api/`, and examples when public behavior changes.
- Generate each changelog page from the diff between the previous tag and the release tag.

## Dependency Hygiene

- Keep direct build and tooling dependencies current when updates are low risk.
- Prefer stable releases over RC, beta, or alpha unless the repo already depends on a prerelease.
- If an update needs a bigger migration, call that out explicitly instead of sneaking it into a routine refresh.

## Pull Request Checklist

- [ ] Tests updated or added when behavior changed
- [ ] KDoc updated if behavior or usage changed
- [ ] README or docs updated if the public API changed
- [ ] Validation checks passing
