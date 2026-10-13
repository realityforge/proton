# T02 — Generated marking and active NullAway checking

- Status: `complete`
- Blocked by: T01
- Spec coverage: R3, R4, AC3, AC4

## Delivers

Marked generation, strict JSpecify-mode checking across owned sourceful rules, and configurable QA javac checking demonstrated by valid and invalid generated sources.

## Acceptance criteria

- [x] Owned sourceful libraries, binaries and tests pass error-level JSpecifyMode, OnlyNullMarked and explicit marking.
- [x] Generated fixtures are NullMarked; valid fixture passes strict checking.
- [x] The same QA compiler configuration rejects generated invalid code with a NullAway diagnostic.
- [x] Actual compiler reads binary type-use annotations with the selected flags while retaining Java 17 output; no blanket exemptions.

## Validation

- Focused core and QA targets; observe negative NullAway control and successful generated compilation.
- Build all owned targets with enabled flags.

## Evidence

- Strict core suite passed (46 tests), QA suite passed (10 tests), and `bazel build //...` passed (81 targets).
- The generated Invalid fixture produces exactly two NullAway errors: root nullable dereference and incompatible nullable generic return. Valid formatted/unformatted fixtures pass the same checking compiler.
- Added a JavaCompiler factory hook and compiler-taking compile/assert overloads; existing system-compiler entry points remain usable. QA checks use BaseErrorProneJavaCompiler with just NullAway and RequireExplicitNullMarking.
- Oracle 17.0.12 failed binary annotation-reading probe despite -XDaddTypeAnnotationsToSymbol=true; the already configured remote JDK 25 is now the runtime for fixture javac, retaining --release 17.
- Strict generic checking exposed the existing nullable name-extractor contract; annotation now expresses nullable callback results without changing runtime behavior.
- Resolved inherited method bounds preserve declaration annotations; compiler-backed core fixture covers substituted nested nullable bounds.
