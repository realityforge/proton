# Bazel Build Migration Requirements

## Mission

Convert Proton from the existing Buildr/Rake build to Bazel while preserving the current Java 17 library behavior, tests,
Maven Central artifact contracts, and release workflow.

## Scope Boundaries

In scope:

* Bazel 9.1.1 module-mode build configuration.
* `bazel-depgen` managed third-party Java dependencies.
* Explicit Bazel targets for all Java sources, tests, fixtures, tooling, and release artifacts.
* GitHub Actions CI that runs the repository verification gate.
* Palantir Java Format automation.
* Buildifier checks.
* Test coverage enforcement.
* Maven Central release packaging and lifecycle scripts.
* `tools/intellij/.managed.bazelproject`.
* `CONTRIBUTING.md` release documentation and changelog updates.

Out of scope:

* Public API changes unrelated to the build migration.
* Backwards-compatible Buildr compatibility shims.
* Publishing a release or committing changes.

## Locked Decisions

* Use Bazel `9.1.1`, matching the user request and the local `bazel-depgen` reference project.
* Use Java 17 source/target behavior, matching the existing `buildfile`.
* Use Bazel module mode with generated `http_file` repositories from `bazel-depgen`, matching the reference project.
* Use explicit source lists in all Bazel targets. Do not use `glob()`.
* Use one `BUILD.bazel` per source-owning directory.
* Run `tools/check.sh` before claiming completion.
* Do not commit unless explicitly asked.

## Command Surface

Expected commands:

* `tools/check.sh`
* `tools/update_java_deps.sh`
* `tools/java_format.sh [write|check]`
* `bazel run //:buildifier`
* `bazel run //:buildifier_check`
* `bazel build //...`
* `bazel test //...`
* `bazel coverage ...`
* `tools/release/check_ready.sh`
* `tools/release/next_version.sh`
* `tools/release/prepare_release.sh <version> [--dry-run]`
* `tools/package_maven_central.sh <version>`
* `tools/release/upload_maven_central.sh <version>`
* `tools/release/finalize_release.sh <version>`
* `tools/release/perform_release.sh <version>`

## Quality Gates

The full gate is `tools/check.sh`.

The gate must:

* Regenerate Bazel dependency outputs.
* Run buildifier checks.
* Run Palantir Java Format in check mode.
* Build all Bazel targets.
* Run tests.
* Run coverage and enforce Proton's current Bazel baseline: 72% line coverage and 62% branch coverage.

## Intentional Divergences

* The Buildr/Rake build and release tasks are removed rather than kept as compatibility shims.
* The release lifecycle helper is adapted from the reference project but does not require README version replacement
  because Proton's README does not currently contain version-pinned installation snippets.
* The coverage enforcement mechanism matches the reference project, but the numeric threshold is set to Proton's current
  measured Bazel baseline rather than the reference project's 95% line / 85% branch threshold. The old Buildr build had
  no coverage gate, and raising Proton to the reference threshold would require a broad test-suite expansion outside this
  build-system migration.

## Open Questions Register

No open questions. The user directed unresolved decisions to follow `/Users/peter/Code/realityforge/bazel-depgen`, and
the remaining project-specific differences are determined by the existing Proton build and source layout.
