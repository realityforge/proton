# Change Log

### Unreleased

* Extract `AbstractProcessorTest.processors()` to simplify getting complete set of processors in downstream projects.
* Extract `AbstractProcessorTest.compiler()` to simplify setup of compiler.
* In the `AbstractStandardProcessor` class, stop emitting duplicate error messages if the error is about an `Element` that was not part of the current compilation round.

### [v0.25](https://github.com/realityforge/proton/tree/v0.25) (2020-01-23) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.24...v0.25)

Changes in this release:

* Add `AbstractProcessorTest.buildClasspath(File...)`, `AbstractProcessorTest.describeFailureDiagnostics(Compilation)` and `AbstractProcessorTest.assertCompilationSuccessful(Compilation)` to make it easier for downstream tests to support multi-stage compilations while still generating useful errors when failures occur.

### [v0.24](https://github.com/realityforge/proton/tree/v0.24) (2020-01-23) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.23...v0.24)

Changes in this release:

* Add `AbstractProcessorTest.outputFiles(...)` overloaded method that omits the filter under the assumption that all files are emitted to the target directory. This is useful when building up multi-stage compiles and all output should be written to the target directory so that it can be picked up by a subsequent compile stage

### [v0.23](https://github.com/realityforge/proton/tree/v0.23) (2020-01-23) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.22...v0.23)

Changes in this release:

* Refactor `AbstractProcessorTest.outputFile()` and invoke it from existing test infrastructure.

### [v0.22](https://github.com/realityforge/proton/tree/v0.22) (2020-01-23) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.21...v0.22)

Changes in this release:

* Extract `AbstractProcessorTest.outputFiles()` to make it easier to emit files into intermediate directories in downstream projects.

### [v0.21](https://github.com/realityforge/proton/tree/v0.21) (2020-01-23) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.20...v0.21)

Changes in this release:

* Extract `AbstractProcessorTest.outputFile()` to make it easier to emit files into intermediate directories in downstream projects.

### [v0.20](https://github.com/realityforge/proton/tree/v0.20) (2020-01-21) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.19...v0.20)

Changes in this release:

* Refactor `AbstractStandardProcessor.performAction` to use generics to support processing arbitrary `Element` subtypes.

### [v0.19](https://github.com/realityforge/proton/tree/v0.19) (2020-01-21) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.18...v0.19)

Changes in this release:

* Address crash that resulted when the `ProcessorException._element`  field referred to a parameter of a method or a constructor.

### [v0.18](https://github.com/realityforge/proton/tree/v0.18) (2020-01-20) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.17...v0.18)

Changes in this release:

* Upgrade the `com.google.truth` artifact to version `0.44`.
* Upgrade the `com.google.testing.compile` artifact to version `0.18`.
* Upgrade the `com.squareup:javapoet` artifact to version `1.12.0`.
* Import the `IOUtil` util class from downstream projects.

### [v0.17](https://github.com/realityforge/proton/tree/v0.17) (2020-01-20) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.16...v0.17)

Changes in this release:

* Emit a stack trace when an `IOException` occurs. Previously the code would just emit the message component of the IO error which made tracking down the root cause difficult. The code also crashed if there was no message supplied to the `IOException`.

### [v0.16](https://github.com/realityforge/proton/tree/v0.16) (2020-01-15) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.15...v0.16)

Changes in this release:

* Extract `JsonUtil` and `ResourceUtil` from downstream libraries. Basic utilities for emitting files and resources from an annotation processor.

### [v0.15](https://github.com/realityforge/proton/tree/v0.15) (2020-01-15) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.14...v0.15)

Changes in this release:

* Add `AbstractProcessorTest.inputs(String... classnames)` for building up a set of input fixtures.
* Add `AbstractProcessorTest.assertSuccessfulCompile(...)` method variant that accepts a predicate for selecting which files to save to the filesystem.

### [v0.14](https://github.com/realityforge/proton/tree/v0.14) (2020-01-13) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.13...v0.14)

Changes in this release:

* Expose `AbstractStandardProcessor.deferElement(...)` so that annotation processors can schedule elements for processing in the next processing round.
* Expose `AbstractStandardProcessor.performAction(...)` so that subclasses can perform the same error recovery actions as the base class.

### [v0.13](https://github.com/realityforge/proton/tree/v0.13) (2020-01-12) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.12...v0.13)

Changes in this release:

* Add `SuperficialValidation.validateType(...)` and `SuperficialValidation.validateTypes(...)` helper methods.

### [v0.12](https://github.com/realityforge/proton/tree/v0.12) (2020-01-09) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.11...v0.12)

Changes in this release:

* Add `MemberChecks.mustNotHaveAnyTypeParameters(...)` helper method.

### [v0.11](https://github.com/realityforge/proton/tree/v0.11) (2020-01-05) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.10...v0.11)

Changes in this release:

* Replace `ElementsUtil.isEnclosedInNonStaticClass(TypeElement)` with easier to understand `ElementsUtil.isNonStaticNestedClass(TypeElement)` method.

### [v0.10](https://github.com/realityforge/proton/tree/v0.10) (2020-01-05) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.09...v0.10)

Changes in this release:

* Add an overloaded `AbstractStandardProcessor.toFilename(...)` method that accepts a prefix and postfix parameter to simplify deriving file names for expected files that are derived from input classes.
* Change the default behaviour of `AbstractStandardProcessor` to emit all files generated from annotation processor except `.class` files. Subclasses that override `AbstractStandardProcessor.emitGeneratedFile()` will need to invoke super method otherwise `.class` files will begin to be emitted.
* Add `ElementsUtil.isEnclosedInNonStaticClass(TypeElement)` helper method.

### [v0.09](https://github.com/realityforge/proton/tree/v0.09) (2019-12-30) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.08...v0.09)

Changes in this release:

* Extract `AbstractStandardProcessor.errorIfProcessingOverAndInvalidTypesDetected()` to simplify reuse in subclasses.
* Expose `ElementsUtil.getTopLevelElement(Element)` that returns the top-level class, interface, enum, etc within a package that contained the specified `Element`.
* Expose `AbstractStandardProcessor.processTypeElements(...)` helper methods to make subclassing easier.
* Remove `AbstractStandardProcessor.process()` method and instead require that it is implemented in subclasses.

### [v0.08](https://github.com/realityforge/proton/tree/v0.08) (2019-12-30) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.07...v0.08)

Changes in this release:

* Add several overloaded methods to `MemberChecks` that assume a `null` value for `alternativeSuppressWarnings`.
* Add an overloaded method `MemberChecks.mustReturnAnInstanceOf(...)` that accepts a `TypeMirror` for the expected type.
* Add an overloaded methods `ElementsUtil.isWarningNotSuppressed(...)` and `ElementsUtil.isWarningSuppressed(...)` that assume a `null` value for `alternativeSuppressWarnings`.

### [v0.07](https://github.com/realityforge/proton/tree/v0.07) (2019-12-29) · [Full Changelog](https://github.com/realityforge/proton/compare/v0.06...v0.07)

Changes in this release:

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
