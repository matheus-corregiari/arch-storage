# Release 1.0.0 validation

Local checks run on Windows, 2026-09-05. This document distinguishes executable local evidence
from hosted validation. The release branch must remain open; no merge, tag or remote publication
is part of this preparation.

## Confirmed defects and corrections

- Regression tests reproduced stale-cache fallback for a valid null and swallowed cancellation
  in `current()` and adapters before correction (three failing JVM tests).
- A cancelled DataStore edit after transformation reproduced an incorrectly updated cache.
  Cache updates now follow successful edits; pending jobs are replaced atomically per entry.
- Compose collection follows entry identity, scope changes, external updates and disposal.
  Snapshot application cannot duplicate backend writes; setters retain immediate local updates.
  The lifecycle regression fails against the old implementation in an isolated copy
  (`build/compose-negative-test.log`).
- Public synchronous reads preserve a valid null; default suppliers no longer imply interrupting timeouts.
- Publication gates now match three CodeQL matrix names and CodeQL Policy. Missing/running gates
  wait; failed/skipped gates reject publication. The scheduled scan uses manual compiler setup.

## Local evidence

| Validation | Result and evidence |
|---|---|
| `ciBuild ciCoverage ciLint ciDocs` | Passed; `build/release-validation.log` |
| JVM tests | 40 executions, zero failures |
| Android host tests | 38 executions, zero failures |
| JavaScript/Chrome tests | 29 executions, zero failures |
| WasmJS/Chrome tests | 29 executions, zero failures |
| Test total | 136 executions, zero failures; module `build/test-results` XML |
| Release, gate and publication Python tests | 20 passing unittest cases |
| Strict MkDocs | Passed using isolated Material 9.7.7; `build/docs-validation.log` |
| Negative API documentation test | Undocumented class and unresolved KDoc link rejected with three warnings; `build/docs-negative-test.log`. Temporary probes removed and docs regenerated successfully. |
| Local publications | All 21 POMs, module metadata and referenced files validated by `tools/verify_publications.py` |
| Independent consumer | RC16 wrote a real preferences file; 1.0.0 read all primitives, bytes, enum and JSON successfully; `build/rc16-writer.log`, `build/consumer-validation.log` |
| Public JVM API | Eight public classes compared to RC16 with `tools/compare_release_api.py`; no removed signatures |
| Signing policy | Real predicates for all 21 signing tasks: false for local-only, true for combined local/GitHub; dry-runs only, `build/signing-local.log`, `build/signing-combined.log` |
| Coordinate availability | All 21 Central POM URLs returned HTTP 404 on 2026-09-05; `build/coordinate-availability.json`. Recheck before eventual publication. |

The standalone consumer explicitly adds core only for RC16 because that release omitted it from the
backends' compile classpath. Candidate consumption proves the new transitive public dependencies.
The source build keeps Android, JVM, JS, WasmJS, iosArm64 and iosSimulatorArm64. iosX64 was absent
from RC16 publications and is removed from this build. Android minimum is now 23.

An initial Wasm incremental-link failure after dependency changes disappeared after a nonincremental
rebuild. The final normal `ciBuild ciCoverage` succeeded with no persistent compiler override.

## Coverage before and after

No production exclusions, baselines or artificial accessor tests were added. Every module and the
aggregate enforce the same floors: 90% lines, 85% instructions and 80% branches. Zero instrumented
branches is N/A. Before values are the verified pre-change extraction reports; after values are
current Kover XML reports.

| Scope | Lines before → after | Instructions before → after | Branches before → after |
|---|---:|---:|---:|
| Aggregate | 95.36% → 95.60% | 92.26% → 92.75% | 73.53% → 90.32% |
| storage-core | 85.88% → 93.33% | 81.46% → 88.39% | 71.43% → 88.10% |
| storage-memory | 94.44% → 94.44% | 95.33% → 95.33% | N/A → N/A |
| storage-datastore | 100.00% → 100.00% | 96.97% → 99.46% | 75.00% → 95.00% |

Aggregate coverage includes backend tests exercising core and is not a weighted average of the
standalone module reports. Inspect each module's `build/reports/kover/report.xml` separately.

## Hosted validation and release activation

[CI run 33995918752](https://github.com/matheus-corregiari/arch-storage/actions/runs/33995918752)
passed the macOS build and coverage suite, including 38 iOS Simulator ARM64 tests with zero failures
or skips. All three real CodeQL scans, CodeQL Policy, static analysis and documentation passed.
The downloaded simulator XML reports are under `build/remote-ci-33995918752`.

That run subsequently hit a 10-second timeout in the legacy RC16 fixture writer. The fixture harness
now owns a separate job for each asynchronous write, joins it before checking the persisted value,
and identifies every key in its output. The revised RC16 writer and 1.0.0 reader passed locally
(`build/rc16-writer-second.log`, `build/consumer-second.log`). Current [PR checks](https://github.com/matheus-corregiari/arch-storage/pull/1/checks)
remain the authority for the complete hosted artifact-validation result.

The lifecycle and concurrent-writer regressions both fail against the original implementations in
an isolated copy. Explicit `-PreleaseVersion=1.0.0` also produced all 21 candidate coordinates despite
an isolated stale `build/version-name.txt` containing `9.9.9`. No production source was reverted.

The GitHub Packages listing could not be checked locally: the CLI token lacks `read:packages` (HTTP 403).
Recheck that destination with release credentials before publication.

The repository currently exposes no configured Actions secrets; the Pages endpoint returns 404.
Before merging/publishing, configure the release App and signing/Central secrets listed in
[CI and releases](docs/ci.md), enable Pages if documentation deployment is desired, and apply required
branch checks. PR validation does not require publication secrets. This preparation does not create
a tag, merge the PR, upload release artifacts remotely or enable hosting.
