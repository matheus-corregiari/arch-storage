# Extraction validation

Validated locally on Windows on 2026-09-05 before Git initialization.
The Arch Lumber and Arch Toolkit source directories were not modified by this extraction.

## Project base

All applicable tracked Arch Lumber infrastructure files are present, including workflows, local
actions, release scripts and tests, Gradle conventions, wrapper, lint configuration, documentation
tooling, issue templates, license and contribution guide. The Lumber module was replaced by the three
storage modules. Lumber release history was replaced by an unreleased storage changelog. Machine-local
files, credentials, Git metadata and pre-existing build caches were not copied.

Storage packages and artifact names are preserved. Public API dependencies are exposed transitively.
Compose remains in core. The extraction fixes mapped nullable deletion and nullable default emissions,
both covered by regression tests. Detekt analyzes every KMP source directory, with no baseline applied.

## Passing checks

| Check | Evidence |
|---|---|
| Build, test coverage, lint, API docs, publication manifest | `build/verification/storage-verify.log`: `ciBuild ciCoverage ciLint ciDocs ciPublicationManifest` succeeded |
| Local publication | `build/verification/storage-publish.log`: `ciPublishLocal` succeeded without signing or remote publication |
| CodeQL compilation entrypoint | `build/verification/storage-reports.log`: `ciCodeql` succeeded using the normal project compiler |
| Documentation | `build/verification/storage-docs.log`: MkDocs strict build succeeded |
| Release policy scripts | 14 Python unittest cases passed |
| Static analysis | Detekt reports contain zero findings; ktlint and Android lint passed |
| Coordinates | `build/ci/publications.tsv` lists 24 distinct publications across three modules and seven platform variants each |

Tests executed successfully on JVM (24), Android host (23), JavaScript/Chrome (19) and WasmJS/Chrome
(19): 85 executions in total. They cover adapters, Compose state writes, serialization, memory sharing
and isolation, DataStore preferences, pending writes, real-file store recreation and the unsupported
web DataStore factory. Reports are in each module's `build/test-results` and `build/reports/tests`.

## Coverage

Final Kover XML reports; the configured minimum is 65% for lines, instructions and branches,
at both module and aggregate levels. No coverage exclusions were added.

| Scope | Lines | Instructions | Branches |
|---|---:|---:|---:|
| Aggregate | 95.36% | 92.26% | 73.53% |
| storage-core | 85.88% | 81.46% | 71.43% |
| storage-memory | 94.44% | 95.33% | N/A: zero instrumented branches |
| storage-datastore | 100.00% | 96.97% | 75.00% |

The aggregate includes cross-module test execution, so its percentages need not equal a weighted
average of the standalone module reports. View `build/reports/kover/html/index.html` and each module's
`build/reports/kover/report.xml` for the measured code.

## Documentation and publication

The strict MkDocs site is in `site/index.html`. API output directories are distinct:
`docs/api/storage-core`, `docs/api/storage-memory` and `docs/api/storage-datastore`. The final Dokka
run reported no undocumented declarations or unresolved documentation links.

Maven Local contains `io.github.matheus-corregiari:storage-core`, `storage-memory` and
`storage-datastore`, including target variants, at `0.0.0-SNAPSHOT`. POMs were inspected for distinct
artifact IDs, storage project metadata and transitive public dependencies. This local version is an
unpublished development version, not a selected release.

## Platform and hosted validation limits

iOS main and test sources compiled to KLIBs on Windows. Framework linking and simulator execution
were skipped because they require macOS/Xcode; the inherited macOS CI runner is configured for them.
Native macOS targets were not configured in the source projects and are not added by this extraction.

After validation, Git was initialized on `master` and `origin` was configured as
`git@github.com:matheus-corregiari/arch-storage.git` at the owner's request.
No GitHub repository, hosted CI execution, CodeQL scan, release, tag or Pages deployment
was created. The workflows are prepared for later repository activation. The isolated CodeQL compiler
configuration is preserved from the base; the local `ciCodeql` check validates its task entrypoint,
not a hosted instrumented scan. See `docs/ci.md` for activation requirements and version coordination.
