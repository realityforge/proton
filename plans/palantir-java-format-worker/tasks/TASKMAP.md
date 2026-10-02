# Task Map

- Spec: [SPEC.md](../SPEC.md)
- Status: implementation-review
- Current frontier: implementation review
- Planning reviewer: /root/planning_review (1/3 rounds; Findings: none)
- Plan checkpoint: automatic (explicit user evidence-based grill/entry exception and passing planning review)
- Implementation reviewer: pending (0/5 rounds)

## Full-scope validation

- Gate: tools/check.sh, git diff --check, reviewed published assigned PR with verified auto-merge state
- Evidence: `tools/check.sh` exit 0 after checksum-verified cache recovery; 4 tests + 2 coverage targets pass; line 72.07%, branch 62.64%; git diff --check passes. Implementation review and GitHub delivery continue through closeout/handoff.

## Tasks

| ID | Task | Status | Blocked by |
| --- | --- | --- | --- |
| T01 | [Migrate formatting end to end](T01-migrate-formatting.md) | complete | None |

## Sequencing notes

One narrow end-to-end migration covers module, gate, wrappers and formatter consumers together; splitting them would leave broken labels. Review after checks, then remove this exact tree in a closeout commit before PR publication. Publication/assignment/auto-merge evidence is reported at handoff.

## Promoted knowledge

not-required (no domain directories).
