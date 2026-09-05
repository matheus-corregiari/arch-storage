# CI and releases

The CI workflows follow the Arch Lumber base and the shared Arch ecosystem conventions.
The convention plugins select module tasks; `build-logic/ci.json` selects the runner and the isolated
CodeQL compiler. Coverage floors live in `gradle.properties` and must only increase as tests improve.

## Pull requests

Every PR runs CI, regardless of its destination. For a destination other than `master`, branch names
are unrestricted and no release version is reserved. A merge into a development branch does not
trigger another CI run; the next PR update does.

For `master`, Release Policy runs first and accepts only:

- `release/X.Y.Z`: the next major (`M+1.0.0`) or minor (`M.m+1.0`) from the latest stable remote tag.
- `hotfix/X.Y.Z`: the next patch (`M.m.p+1`).
- Optional `-rcN` uses the existing numeric RC convention; increasing RCs and promotion to the stable
  version are allowed, but returning to an RC after a stable release is rejected.

Versions have three components, no leading zeroes, and no `v` prefix. Duplicate or historical remote
tags are rejected. A failed remote query fails the policy. A repository without stable tags begins
at `release/1.0.0`.

## Required gates

| Check | Command or responsibility |
|---|---|
| Release Policy | Python policy unit tests and validation against remote tags |
| Coverage Gate | `./gradlew ciBuild ciCoverage`: assemble, tests, merged coverage verification |
| Static Analysis | `./gradlew ciLint`: Detekt, ktlint and available Android lint tasks |
| Docs Gate | `./gradlew ciDocs`, then `python -m mkdocs build --strict` |
| CodeQL | `./gradlew ciCodeql`: JVM/Android compilation; also analyzes Actions and Python |
| CI Gate | Requires successful completion of every gate, including policy |

`ciCoverage` already includes `ciTest`. There is no second test job. Projects with Apple targets use
macOS for build/tests/publication; Android uses Linux. The same build job owns all supported targets,
so JVM/Android/browser tests are not repeated on a second host. Windows local validation does not
prove Apple binaries; the macOS CI run does.

CodeQL has a separate checkout and compiler configuration. Its outputs are never published. Coverage
reports are uploaded as artifacts; Codecov receives master reports for visibility, while Gradle
enforces the actual gate. The Codecov upload is not the coverage threshold.

Configure branch rules to require `CI Gate`, `Coverage Gate` and `Static Analysis`, with branches up
to date, and retain CodeQL/code-quality merge protections. Requiring `CI Gate` prevents a skipped
downstream job from making a rejected release policy mergeable. Apply common gates to all PR targets;
only the version policy is specific to master. Administrator bypasses remain explicit exceptions.

## Master and publication

The push of a merged commit to `master` reruns the same CI. Tag creation waits for all gates, identifies
the merged PR, fetches remote tags again, and creates an annotated tag on that exact SHA. The release
GitHub App sends the tag so its push triggers `release.yml`. Pages deploys the already-built site.

The tag workflow requires the annotated remote tag, a matching merged PR, master ancestry and a
successful master CI run for the exact SHA. It publishes using the tag's exact version, first to Maven
Central and then to GitHub Packages, from a single host. It confirms publication coordinates before
creating the GitHub Release. No additional test/lint/coverage suite runs for the tag; native publication
tasks may compile/package their dependencies, reusing available Gradle outputs.

Tags and publication are serialized without canceling active releases. GitHub may replace a pending
run if several releases arrive together; resume the affected run explicitly and revalidate the version.
Queue order is not a version reservation. Never move, overwrite or delete an existing release tag to
recover a publication failure.

## Recovery

Use the Release workflow's manual dispatch with the existing tag and destination `central`, `github`,
`both`, or `release-only`. Skipped destinations must already contain every publication's POM; the
workflow verifies this before proceeding and checks both registries before creating the GitHub Release.
If Central is still processing a deployment, wait for that deployment rather than uploading it again.
Selecting `both` is only appropriate when neither destination has accepted the release.

Required secrets: `RELEASE_APP_ID`, `RELEASE_APP_PRIVATE_KEY`, `MAVEN_CENTRAL_USERNAME`,
`MAVEN_CENTRAL_PASSWORD`, `SIGN_KEY`, `SIGN_KEYID`, `SIGN_PASSWORD`; `CODECOV_TOKEN` is used for
reporting. The release App must be allowed to create tags by the tag ruleset. PR validation does not
use publication or signing secrets.

## Local commands

```sh
python -m unittest discover -s .github/scripts -p 'test_*.py'
./gradlew ciBuild ciCoverage
./gradlew ciLint ciDocs
python -m pip install -r .github/requirements-docs.txt
python -m mkdocs build --strict
```

Use JDK 21 and the project wrapper. Publication also has `ciPublicationManifest` for verifying the
complete list of Maven coordinates, without uploading packages.

## Local extraction without Git

The version is read from `build/version-name.txt` (written by the CI action), then from
`-PreleaseVersion=...`, then defaults to `0.0.0-SNAPSHOT`. No Git commands run during version lookup.
`ciPublishLocal` writes to Maven Local and skips signing; it does not publish remotely.
All three modules must appear separately in `build/ci/publications.tsv`.

The checked-in workflows are prepared configuration, not proof of a successful hosted run.
GitHub repository creation, branch rules, secrets, Pages and Codecov activation are separate setup.
Before the first remote publication, check existing Maven coordinates from Arch Toolkit to avoid
reusing a published version. The no-tag policy's initial `1.0.0` is not a Maven availability check.
