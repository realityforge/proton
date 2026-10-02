# Task Map

- Spec: [SPEC.md](../SPEC.md)
- Status: planned
- Current frontier: T01
- Planning reviewer: /root/planning_review (1/3 rounds; Findings: none)
- Plan checkpoint: automatic (explicit user evidence-based grill/entry exception and passing planning review)
- Implementation reviewer: pending (0/5 rounds)

## Full-scope validation

- Gate: tools/check.sh, git diff --check, reviewed published assigned PR with verified auto-merge state
- Evidence: pending

## Tasks

| ID | Task | Status | Blocked by |
| --- | --- | --- | --- |
| T01 | [Migrate formatting end to end](T01-migrate-formatting.md) | pending | None |

## Sequencing notes

One narrow end-to-end migration covers module, gate, wrappers and formatter consumers together; splitting them would leave broken labels. Review after checks, then remove this exact tree in a closeout commit before PR publication. Publication/assignment/auto-merge evidence is reported at handoff.

## Promoted knowledge

not-required (no domain directories).
