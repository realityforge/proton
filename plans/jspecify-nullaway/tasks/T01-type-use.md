# T01 — JSpecify recognition and signatures

- Status: `complete`
- Blocked by: None
- Spec coverage: R1, R2, AC1, AC2

## Delivers

Outer type-use recognition and annotation-preserving generated signatures with no legacy generation helpers; unrelated declaration whitelist hooks and refMethod return contract remain usable.

## Acceptance criteria

- [x] Outer return/parameter/field nullable recognition excludes nested arguments and array components.
- [x] JavaPoet output preserves root/nested/array/varargs/wildcard/type-variable-bound annotations and inherited resolved signatures.
- [x] Generated signatures compile; reference return remains nonnull with nested/parameter annotations preserved.

## Validation

- Focused core TestNG target with compiler-backed generator and annotation tests.
- Inspect generated signature output and direct legacy helper removal.

## Evidence

- `bazel test //core/src/test/java/org/realityforge/proton:core_tests --test_arg=-verbose --test_arg=2 --test_output=errors` passed: 46 tests.
- New processor test emits abstract overrides and javac compiles them; it covers nested annotations, both array positions, varargs, wildcard bounds, recursive intersection bounds, resolved inherited signatures and ref returns.
- javac substitution drops explicit annotations on type-variable roots; declaration/resolved pairing preserves them without nullness inference.
- JDK probe: Oracle 17.0.12 with -XDaddTypeAnnotationsToSymbol=true reads no binary type annotations; OpenJDK 25 reads @Marker from the same classfile. T02 will use the already available remote JDK 25 for strict fixture checking while retaining --release 17.

- Implementation review round 1 found synthetic Object in resolved interface-only intersections. Reproduced the IndexOutOfBoundsException with the compiler-backed fixture, then excluded the synthetic leading bound before pairing declarations. Regression now compiles with substituted nested/nullable bounds.
