# T01 — Migrate formatting end to end

- Status: complete
- Blocked by: None
- Spec coverage: R1–R5, AC1–AC5

## Delivers

Pinned external rules, worker-backed CI gate, public write/watch wrappers, graph coverage and preserved packaged formatter behavior; documented and reviewed PR delivery.

## Acceptance criteria

- [x] Verified module/lockfile; removed obsolete plumbing.
- [x] Complete supported Java graph coverage; dirty-source failure and worker execution proven.
- [x] Wrappers preserve write/check behavior and index safety; watcher exercised.
- [x] Existing generated fixture/package tests pass without fixture changes.
- [x] Changelog/docs and tools/check.sh pass.
- Delivery continuation: closeout, PR assignment and verified auto-merge/merge state at handoff; implementation review passed.

## Validation

Archive SHA-256; bash -n; buildifier_check; aquery source comparison; execution log; negative-source/write/watch experiments; tools/check.sh; git diff --check; GitHub PR state.

## Evidence

- Verified archive SHA-256 e21fe1d1c5663d0ffb8e45e7bd6ebdae60016df5ce3b3a17b8187780c243324f and BCR 404; Bazel module graph resolves rules_java 9.9.0, rules_jvm_external 7.1 and archive-overridden rules module.
- `bash -n tools/java_format.sh tools/java_format_watch.sh tools/update_java_deps.sh` and `bazel run //:buildifier_check` passed.
- `bazel aquery 'mnemonic("PalantirJavaFormat", deps(//:java_format_check))' --include_aspects --output=jsonproto` yields 10 scan actions covering 46 unique Java files. Exact set equality with `bazel query 'labels(srcs, kind("java_.*", //...))'` workspace Java labels and old root enumeration; 3 QA fixtures excluded.
- `bazel build //:java_format_check --execution_log_json_file=...` passed; parsed log contains 11 uncached PalantirJavaFormat actions (10 scans + report), all runner=worker, exitCode=0. Config limits instances to 1.
- Temporary trailing whitespace in StopWatch.java, TestUtil.java and DistBuilder.java caused wrapper check to fail and name all 3 plus remediation. Byte comparison confirmed source and index unchanged. Default write restored exact bytes while preserving the deliberately staged dirty StopWatch blob. Index restored afterward.
- Watch wrapper emitted startup message and formatted a StopWatch edit back to exact original bytes; process terminated and all experiments restored. No Java/fixture diff remains.
- Public target binary initially failed under implicit Java 11 (`records are not supported in -source 11`). Pin target compilation and runtime to 17, matching Proton release level and upstream tool assumptions; public write/watch now pass. Tool compilation/runtime stay 25. This implements existing wrapper outcomes, without a scope change.
- Initial full gate hit local Java/Maven TLS handshake failure for existing ASM 9.10.1; isolated cache populated with 20 missing artifacts via curl and every module artifact SHA-256 verified. Full gate retry passed (exit 0): dependency regeneration, buildifier, format check, all 63 build targets, 4 packaging/config/tool test targets and 2 coverage targets; line 72.07% (1058/1468), branch 62.64% (456/728). Generated-source and vendor fallback tests passed; fixtures unchanged.
- Logs/experiment script are outside the repository at /tmp/proton-format-reference; no temporary source, index, debug or fixture changes retained.
- Simplify preflight retained the direct case wrapper and shared dependency graph; no extra abstraction or compatibility path needed. `git diff --check` passed; no Java/fixture or third-party generated BUILD diff remains.
- Implementation review: /root/implementation_review, 2/5 attempts (first interrupted by user pause, second Findings: none). Same reviewer resumed against unchanged bcc8d79 at restored worktree; bash syntax, diff, archive integrity and retained behavior/full-gate evidence checked. Remaining delivery gates: publish, assign, exact-head CI and merge automation.
