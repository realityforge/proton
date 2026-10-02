# Palantir Java Format worker migration

## Source

Explicit user authorization covers implementation, commits, PR publication, assignment to realityforge and auto-merge without bypassing CI. The user waives grill confirmation and the delivery entry gate when repository/reference evidence resolves all decisions. The completed evidence-based design tree below has no unresolved frontier.

Evidence: existing CI invokes tools/check.sh → tools/java_format.sh check; local formatter also supplies release shading and test runtime dependencies. Upstream v0.1.1 archive SHA-256 is e21fe1d1c5663d0ffb8e45e7bd6ebdae60016df5ce3b3a17b8187780c243324f; MODULE pins formatter 2.93.0, rules_java 9.9.0 and rules_jvm_external 7.1. defs.bzl checks workspace JavaInfo sources through deps/runtime_deps/exports/tests. BCR module URL returns 404. Upstream immutable Maven lock advances formatter-transitive error_prone_annotations from 2.47.0 to 2.49.0; no other unrelated updates are intended. No domain-documentation directories or ahab config exist.

## Problem and required outcome

Replace repository-local formatting dependency plumbing with rules_palantir_java_format and actual persistent-worker checks, preserving CI enforcement and Proton generated-source formatting.

## Scope and constraints

Use explicit target/source lists; each source directory owns its BUILD. Preserve default write and check wrapper commands, add public watch command/wrapper. CI already exists; keep its invocation. Formatting gate covers only Java reachable via supported target graph, as explicitly clarified by the user. No out-of-graph enumeration checks, unrelated dependency upgrades, Java reformat churn, protection changes or force merge.

## Requirements and acceptance criteria

- R1 / AC1: Resolve pinned rules v0.1.1 with verified archive integrity, remove local binary/dependency plumbing and stale depgen references, regenerate module lockfile. Only unavoidable module graph upgrades are allowed.
- R2 / AC2: Root testonly java_format_check reaches all JavaInfo-owned workspace sources via explicit main/tool/test roots; aquery coverage comparison and dirty-source negative check prove coverage and failure/remediation. Execution log proves PalantirJavaFormat worker use with one worker instance configured.
- R3 / AC3: Existing check CI reaches bazel build //:java_format_check; write/watch use public external binaries with prior roots (core/src, qa/src/main, qa/src/test/java, tools). Check is read-only and write does not change staged index. Watch command starts and handles an eligible source edit.
- R4 / AC4: Release shaded fallback and test runtime dependencies use upstream Maven extension's direct public formatter artifact at 2.93.0; processor fixture/package tests pass without changing fixtures.
- R5 / AC5: Changelog and contributor docs describe new behavior; bash syntax, buildifier, tools/check.sh and git diff --check pass. Publish reviewed PR assigned to realityforge, attach it, enable repository-allowed auto-merge, verify state or report precise blocker.

## Completed design tree and significant decisions

| ID | Decision | Evidence/rationale | Impact | User verification |
| --- | --- | --- | --- | --- |
| D1 | Archive override v0.1.1 | Latest GitHub release; not in BCR | Reproducible module; formatter stays 2.93.0 | Module integrity and lockfile |
| D2 | Graph-only gate | Explicit user clarification; upstream aspect edges | Fixtures/data outside JavaInfo graph stay excluded | Coverage audit and dirty-source failure |
| D3 | Root main/core/qa, all_tests, release dist and javadoc_jar_builder | Existing source targets and test suites | All reachable production/tests/tools checked | aquery coverage audit |
| D4 | Share upstream Maven extension repo | Release shading and tests require formatter JavaInfo; public direct artifact | Removes duplicate dependency plumbing; rules_java 9.9.0 minimum and its locked error_prone_annotations 2.49.0 are required by upstream | Packaging/generated fixtures tests |
| D5 | Public write/watch commands, prior roots | Reference wrappers and current formatter scope | Preserve wrapper/default semantics and fixture exclusion | Write/index/watch experiments |
| D6 | Existing CI and allowed auto-merge | Explicit requested result | No immediate merge or bypass | PR settings/check status |

## Testing decisions

Audit aquery inputs versus all owned workspace JavaInfo sources. Introduce temporary formatting-only defects in representative main/test/tool sources, verify named failures and remediation without writes, then restore exact contents. Exercise write and watcher while preserving index. Run full tools/check.sh (build, tests, generated dependencies, coverage). Keep experiments outside delivered diff and record evidence in the task.

## Open questions

None.
