# Task Map

- Spec: [SPEC.md](../SPEC.md)
- Status: `closeout`
- Current frontier: none (all implementation tasks complete)
- Planning reviewer: `/root/planning_reviewer` (`2/3` rounds), Findings: none (round 2 confirms post-implementation gate ownership)
- Plan checkpoint: `automatic`; authority: confirmed originating grill and passing planning review
- Implementation reviewer: `/root/implementation_reviewer` (`2/5` rounds); Findings: none after fixing the round 1 intersection-bound crash

## Full-scope validation

- Gate: `tools/check.sh`
- Evidence: final tools/check.sh passed on remote Java 17; 74.26% line / 64.65% branch coverage; all packaging/release tests passed

## Tasks

| ID | Task | Status | Blocked by |
| --- | --- | --- | --- |
| T01 | [JSpecify recognition and signatures](T01-type-use.md) | complete | None |
| T02 | [Generated marking and active NullAway checking](T02-enforcement.md) | complete | T01 |
| T03 | [Distribution migration and full validation](T03-integration.md) | complete | T02 |

## Sequencing notes

- T01 establishes behavior before enforcement. T02 validates actual generated compiler checking. T03 owns release/dependency/docs migration and the full gate.
- Reviewers are read-only and persistent within each phase. The primary agent implements sequentially.
- T03 completion covers implementation and the full gate. The workflow owns the remaining AC6 gates: persistent implementation review, deletion of this exact temporary tree in closeout, then creating and attaching the authorized open PR.

## Promoted knowledge

- not-required: checked docs/adr, docs/glossary, docs/specs and docs/deferred; none exists
