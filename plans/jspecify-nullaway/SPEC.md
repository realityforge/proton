# JSpecify / NullAway Migration Spec

## Source

- Shared understanding: the human-confirmed grill from the originating Sting chat, supplied in this chat's delegation. It authorizes implementation on a new branch and an open Proton PR, without a release or another plan approval pause.
- Repository evidence: clean master at 9675580; AnnotationsUtil reads exact legacy declaration names; GeneratorUtil emits legacy declaration annotations and uses annotation-dropping JavaPoet conversions; CompileTestUtil uses the system compiler with caller-supplied options; sourceful Bazel wrappers already enforce OnlyNullMarked and explicit marking but omit JSpecifyMode. Reference: jdbt/third_party/java/rules.bzl.

## Problem

Processor consumers need correctly positioned JSpecify type-use nullability and enforced checking of generated sources. Existing legacy declaration copying loses nested type annotations and cannot express JSpecify array nullability.

## Required outcome

The next Proton release defaults exclusively to JSpecify recognition and generation. Owned code is checked by NullAway in JSpecify mode, including a demonstrably active configurable QA compilation path for generated code. Local core and QA libraries remain consumable by Sting.

## Scope

- In scope: recognition, JavaPoet type emission, generated marking, fixtures, QA compiler verification, Bazel enforcement, owned dependencies, docs, release metadata, tests, and open PR.
- Out of scope: Sting edits, publishing a release, GWT/J2CL, legacy variants, general nullness inference, or unrelated public utility changes.

## Constraints

- Breaking forward migration: remove legacy generation constants/helpers/dependencies rather than maintain a compatibility path.
- Preserve unrelated public utilities and intentional processor/framework extension hooks.
- Preserve explicit JSpecify annotations at supported type positions; do not infer nullable for unspecified nested types. Unmarked consumers remain supported through optimistic boundaries, without promising their bodies are checked.
- No blanket suppression or generated NullUnmarked holes. Java 17 source level remains; verify -XDaddTypeAnnotationsToSymbol=true with the actual compiler.
- Explicit Bazel sources, directory ownership, CHANGELOG Unreleased entry, full tools/check.sh gate.

## Requirements

- R1: nullable recognition distinguishes the outer return/parameter/field type from array components and generic arguments.
- R2: generated signatures preserve supported explicit JSpecify root, nested, array, wildcard-bound and type-variable-bound annotations, including resolved inherited overrides, while dropping redundant explicit nonnull emission. Reference helpers retain their existing nonnull-return contract while preserving nested/parameter annotations.
- R3: generated types are NullMarked and checked without blanket exemptions; all owned sourceful library/binary/test rules enforce error-level NullAway JSpecifyMode, OnlyNullMarked and explicit marking.
- R4: QA supports the smallest configurable compiler seam that can check handwritten and generated fixture code; a negative generated control proves NullAway actually ran.
- R5: owned legacy nullability dependencies, fixtures, docs and release metadata migrate; unrelated utilities/hooks and local Bazel core/QA targets remain available.
- R6: focused behavior checks, full repository gate, persistent implementation review and temporary plan removal precede an open attached PR.

## Acceptance criteria

- AC1 (R1): processor tests assert outer nullable return, parameter and array cases and negative nested/component cases.
- AC2 (R2): compiler-backed generator tests cover annotated declared types, nested generics, arrays/varargs, wildcard and type-variable bounds, resolved overrides, and ref-return behavior; emitted source compiles.
- AC3 (R3): sourceful builds pass strict checking; a generated marked success fixture compiles, and no owned generated NullUnmarked hole is added.
- AC4 (R4): configurable QA compilation rejects a deliberately invalid generated nullable dereference with a NullAway diagnostic, and passes a valid generated fixture using the same configuration.
- AC5 (R5): dependency regeneration and packaging checks pass; docs/Unreleased explain the break, exported generator API and local labels; no owned legacy generation references remain.
- AC6 (R6): tools/check.sh passes, diff is inspected, same implementation reviewer reports Findings: none, plan tree is removed in a closeout commit, and PR is created/attached and left open.

## Significant decisions

| ID | Decision | Rationale | Impact | User verification |
| --- | --- | --- | --- | --- |
| D1 | JSpecify-only forward migration | Confirmed release contract | Consumers must migrate legacy annotations and removed APIs | Review changelog and Sting local integration |
| D2 | Focused recursive JavaPoet type conversion | Actual JavaPoet loses type-use annotations | New utility preserves explicit type annotations without a general resolver | Review generated root/nested/array signatures |
| D3 | Mark generated types and check generated fixture code | Bazel wrapper flags do not reach plain QA javac | Compiler configuration seam and negative control are required | Verify generated error test and valid fixture |
| D4 | Keep Java 17 source compatibility and existing local labels | Settled toolchain/integration contract | No release needed for Sting local override | Consume core/QA targets and inspect compiler checks |
| D5 | Open PR, no release | Explicit authorization | Human verifies final breaking change before release | Review open PR |

## Technical decisions

- Use JavaPoet's existing type classes to emit explicit JSpecify type annotations; preserve declaration annotation whitelist overloads as framework hooks with migrated defaults.
- Resolve override member signatures through javac asMemberOf before annotation-aware conversion. Preserve refMethod's nonnull root return contract, without legacy emission.
- Expose a compiler injection overload/hook while retaining the existing system-compiler utility behavior for unrelated consumers. Wire Error Prone/NullAway into owned fixture tests and configure all relevant javac exports.
- Remove direct legacy annotation artifacts from distribution metadata; unrelated third-party transitive dependency metadata is not an owned generation path.

## Testing decisions

- Run baseline tools/check.sh and record failures without weakening checks.
- Use compiler-backed assertions and valid/invalid generated fixtures, not string-only confidence.
- Run focused core and QA targets after each slice; final full gate includes dependency regeneration, formatting, all builds, packaging tests and coverage thresholds.

## Open questions

None.
