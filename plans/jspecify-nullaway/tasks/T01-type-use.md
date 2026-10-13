# T01 — JSpecify recognition and signatures

- Status: `pending`
- Blocked by: None
- Spec coverage: R1, R2, AC1, AC2

## Delivers

Outer type-use recognition and annotation-preserving generated signatures with no legacy generation helpers; unrelated declaration whitelist hooks and refMethod return contract remain usable.

## Acceptance criteria

- [ ] Outer return/parameter/field nullable recognition excludes nested arguments and array components.
- [ ] JavaPoet output preserves root/nested/array/varargs/wildcard/type-variable-bound annotations and inherited resolved signatures.
- [ ] Generated signatures compile; reference return remains nonnull with nested/parameter annotations preserved.

## Validation

- Focused core TestNG target with compiler-backed generator and annotation tests.
- Inspect generated signature output and direct legacy helper removal.

## Evidence

- pending
