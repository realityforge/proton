# T02 — Generated marking and active NullAway checking

- Status: `pending`
- Blocked by: T01
- Spec coverage: R3, R4, AC3, AC4

## Delivers

Marked generation, strict JSpecify-mode checking across owned sourceful rules, and configurable QA javac checking demonstrated by valid and invalid generated sources.

## Acceptance criteria

- [ ] Owned sourceful libraries, binaries and tests pass error-level JSpecifyMode, OnlyNullMarked and explicit marking.
- [ ] Generated fixtures are NullMarked; valid fixture passes strict checking.
- [ ] The same QA compiler configuration rejects generated invalid code with a NullAway diagnostic.
- [ ] Actual compiler supports the selected Java 17 type annotation workaround; no blanket exemptions.

## Validation

- Focused core and QA targets; observe negative NullAway control and successful generated compilation.
- Build all owned targets with enabled flags.

## Evidence

- pending
