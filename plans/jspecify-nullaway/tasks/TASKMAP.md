# Task Map

- Spec: [SPEC.md](../SPEC.md)
- Status: `implementing`
- Current frontier: `T02`
- Planning reviewer: `/root/planning_reviewer` (`1/3` rounds), Findings: none
- Plan checkpoint: `automatic`; authority: confirmed originating grill and passing planning review
- Implementation reviewer: `pending` (`0/5` rounds)

## Full-scope validation

- Gate: `tools/check.sh`
- Evidence: baseline tools/check.sh passed (72.07% line, 62.53% branch); implementation pending

## Tasks

| ID | Task | Status | Blocked by |
| --- | --- | --- | --- |
| T01 | [JSpecify recognition and signatures](T01-type-use.md) | complete | None |
| T02 | [Generated marking and active NullAway checking](T02-enforcement.md) | pending | T01 |
| T03 | [Distribution migration and full validation](T03-integration.md) | pending | T02 |

## Sequencing notes

- T01 establishes behavior before enforcement. T02 validates actual generated compiler checking. T03 owns release/dependency/docs migration and the full gate.
- Reviewers are read-only and persistent within each phase. The primary agent implements sequentially.
- Closeout removes this exact temporary tree after implementation review, then creates and attaches the authorized open PR.

## Promoted knowledge

- not-required (verify domain directories before implementation review)
