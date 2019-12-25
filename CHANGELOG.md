# Change Log

### Unreleased

* Rename library to `proton` and define a separate module named `core` that contains the same contents as was previously in the `proton-processor-pack` artifact. 

### [v0.05](https://github.com/realityforge/proton/tree/v0.05) (2019-12-25) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.04...v0.05)

Changes in this release:

* Extract the logic for determining whether unresolved types are processed into `AbstractStandardProcessor.shouldDeferUnresolved()` so that subclasses can override the behaviour.

### [v0.04](https://github.com/realityforge/proton/tree/v0.04) (2019-12-24) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.03...v0.04)

Changes in this release:

* Add missing variant of `SuppressWarningsUtil.addSuppressWarningsIfRequired(...)` for FieldSpec.Builder types.
* Add variants of `SuppressWarningsUtil.addSuppressWarningsIfRequired(...)` for ParameterSpec.Builder type.

### [v0.03](https://github.com/realityforge/proton/tree/v0.03) (2019-12-24) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.02...v0.03)

Changes in this release:

* Make the `AnnotationsUtil.getRepeatingAnnotations(...)` utility method public.

### [v0.02](https://github.com/realityforge/proton/tree/v0.02) (2019-12-24) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.01...v0.02)

Changes in this release:

* Introduce `AnnotationsUtil.getRepeatingAnnotations(...)` utility.

### [v0.01](https://github.com/realityforge/proton/tree/v0.01) (2019-12-24) · [Full Changelog](https://github.com/realityforge/proton/compare/5d8d0136c796a3732c5d74715aa5e01764a9eaa9...v0.01)

Changes in this release:

‎🎉 Initial project release ‎🎉
