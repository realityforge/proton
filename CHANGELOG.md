# Change Log

### Unreleased

* Decouple `GeneratorUtil` from `com.google.auto:auto-common`.
* Change the `GeneratorUtil.getPackageElement(...)` to take an `Element` so can retrieve the package of any element rather than just a`TypeElement`.
* Decouple `AnnotationsUtil` from `com.google.auto:auto-common`. This involved importing and reimplementing `AnnotationsUtil.getAnnotationValuesWithDefaults()` from `auto-common`. The original `auto-common`
* Expose `AbstractStandardProcessor.reportError(...)` method as protected access so subclasses can invoke.
* Add `AbstractStandardProcessor.emitTypeSpec(...)` helper method as it appeared in all downstream classes. In the future it will also make it possible for the toolkit to track processing statistics.
* Decouple `AbstractStandardProcessor` from `com.google.auto:auto-common`.
* Remove unused dependencies `com.google.auto:auto-common` and `com.google.guava:guava`.
* Add `ElementsUtil.isWarningNotSuppressed(...)` utility method to compliment `ElementsUtil.isWarningSuppressed(...)` as the first form is that which is usually used in downstream libraries.
* Introduce several `AbstractProcessorTest.assertCompilesWith*(...)` helper methods to aid testing annotation processors using common test patterns.
* Add a template method `AbstractProcessorTest.getFixtureKeyPart()` that makes it possible to customize the property used to retrieve the fixture dir.

### [v0.06](https://github.com/realityforge/proton/tree/v0.06) (2019-12-25) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.05...v0.06)

Changes in this release:

* Rename library to `proton` and define a separate module named `core` that contains the same contents as was previously in the `proton-processor-pack` artifact.
* Introduce a `qa` module the contains the base class we use to test the annotation processors.

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
