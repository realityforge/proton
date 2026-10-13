# T03 — Distribution migration and full validation

- Status: `complete`
- Blocked by: T02
- Spec coverage: R5, R6, AC5, AC6 (implementation and full-gate portions; post-implementation gates owned by workflow closeout)

## Delivers

Migrated dependencies, fixtures, public migration documentation and release metadata, validated distribution, and a reviewable final change with workflow closeout and open PR.

## Acceptance criteria

- [x] Owned legacy generation dependencies/references removed; packaging checks prove JSpecify metadata and artifacts.
- [x] Unreleased changelog and docs state the breaking migration, generator API and local core/QA labels.
- [x] Focused checks and full tools/check.sh pass, with final diff inspection.
- [x] Implementation and full-gate evidence are committed and ready for persistent implementation review, closeout and the authorized open PR.

## Validation

- Dependency regeneration, packaging QA tests, owned-reference scan, tools/check.sh.
- Workflow review/closeout gates and GitHub PR state inspection.

## Evidence

- Rebased the plan/implementation commits onto current origin/master (1970a44), retaining upstream Bazel 9.3.0, rules_java 9.9.0 and Palantir worker formatting.
- Verified remote Zulu JDK 17.0.20 reads binary type-use annotations with -XDaddTypeAnnotationsToSymbol=true; retained the existing remote Java 17 runtime/source level and JDK 25 tooling.
- Focused Java 17 QA target passed, including exactly two NullAway errors in the deliberately invalid @Generated/@NullMarked source.
- Final `tools/check.sh` passed: regenerated dependency metadata, Buildifier, worker formatting, all builds, packaging/release tests and core/QA coverage. Line coverage 74.21% (1145/1543); branch coverage 64.59% (498/771).
- Parsed generated core and QA Maven POMs: JSpecify 1.0.0 present; direct legacy annotation artifact absent.
- Owned-source/reference scan finds no legacy generation helpers/constants/dependencies; historical changelog and migration/absence assertions intentionally name removed APIs/artifacts.
- Inspected final scope and diff; no Sting files modified. README documents the generator/QA APIs, local labels and compiler requirements.
- No domain directories exist; no canonical promotion is required. Ready for persistent implementation review and subsequent closeout.
