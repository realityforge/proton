# Bazel Build Migration Implementation Plan

## Phase Sequence

1. Add planning records and confirm existing Buildr behavior.
2. Add Bazel root/module configuration, depgen inputs, formatting, buildifier, coverage, IntelliJ, and CI scripts.
3. Add explicit Bazel packages for production Java sources, tests, fixtures, and release tooling.
4. Add release artifact rules that preserve the two Maven artifacts: `proton-core` and `proton-qa`.
5. Update docs and changelog.
6. Regenerate generated dependency outputs, buildify, format, and run `tools/check.sh`.

## Delivery Approach

The implementation uses hard cutover semantics: Bazel becomes the build, verification, and release system. Existing
Buildr/Rake files are removed once Bazel replacements exist and pass verification.

## High-Risk Areas

* `proton-core.jar` must continue bundling relocated formatter classes while excluding formatter dependencies from the
  published POM.
* QA tests need Bazel runfile paths for fixtures, release jars, generated POMs, and external dependency jars.
* Coverage enforcement must avoid double-running the same suites while still exercising production code.
* Coverage enforcement follows the reference project mechanism, but uses Proton's measured Bazel baseline threshold
  of 72% line coverage and 62% branch coverage.
* Generated dependency sections must stay reproducible through `tools/update_java_deps.sh`.

## Mitigations

* Implement release jar tests and preserve existing QA artifact assertions.
* Use explicit system properties in Bazel test targets for fixture and artifact paths.
* Use a single full gate, `tools/check.sh`, in local verification and GitHub CI.
* Run `git diff --exit-code` in CI after checks to detect stale generated files or formatting.

## Required Full Gate

```bash
tools/check.sh
```

## Decision Log

No user-facing open questions were required. Decisions were copied from the `bazel-depgen` reference project unless
Proton's existing Buildr configuration required a project-specific value. Coverage uses the same enforcement script and
coverage command shape as the reference project, but the numeric threshold is project-specific because Proton had no
previous coverage gate and currently measures 72.57% line / 62.50% branch coverage under Bazel.
