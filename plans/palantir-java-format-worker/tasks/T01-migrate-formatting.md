# T01 — Migrate formatting end to end

- Status: pending
- Blocked by: None
- Spec coverage: R1–R5, AC1–AC5

## Delivers

Pinned external rules, worker-backed CI gate, public write/watch wrappers, graph coverage and preserved packaged formatter behavior; documented and reviewed PR delivery.

## Acceptance criteria

- [ ] Verified module/lockfile; removed obsolete plumbing.
- [ ] Complete supported Java graph coverage; dirty-source failure and worker execution proven.
- [ ] Wrappers preserve write/check behavior and index safety; watcher exercised.
- [ ] Existing generated fixture/package tests pass without fixture changes.
- [ ] Changelog/docs and tools/check.sh pass; implementation review passes; PR assigned and auto-merge state verified at handoff.

## Validation

Archive SHA-256; bash -n; buildifier_check; aquery source comparison; execution log; negative-source/write/watch experiments; tools/check.sh; git diff --check; GitHub PR state.

## Evidence

pending.
