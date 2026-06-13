# Proton Generated Source Formatting Requirements

Status: accepted
Last updated: 2026-06-13

## Mission

Backport the Arez generated-source formatting capability into Proton so processors built on
`AbstractStandardProcessor` can opt into formatting generated Java source before it is written through the annotation
processing `Filer`.

## Scope

- Add common support for `<optionPrefix>.format_generated_source=true` in Proton core.
- Keep default generated output unchanged when the option is absent or false.
- Embed and relocate `palantir-java-format` and its required dependency closure into `proton-core.jar`.
- Keep formatter dependencies out of Proton's published transitive dependency surface.
- Extend Proton QA helpers so successful fixture compiles verify both unformatted and formatted generated output.
- Add Proton-owned regression tests for option recognition, formatting, diagnostics, fixture behavior, and shaded jar
  packaging.
- Update documentation and changelog for downstream annotation processor authors.

## Out Of Scope

- Do not change JavaPoet code generation semantics beyond optional formatting.
- Do not require downstream processors to directly declare, shade, or version formatter dependencies.
- Do not add an opt-out for formatted fixture verification in the standard successful fixture helper.
- Do not retrofit downstream repositories in this Proton change; downstream adoption happens by upgrading and shading
  Proton.

## Locked Decisions

- The option name is `format_generated_source`, scoped by each processor's existing `getOptionPrefix()`.
- `AbstractStandardProcessor` owns option parsing, option advertising, and the `emitTypeSpec(...)` formatting hook.
- Formatter API access is reflective.
- Formatter lookup order is:
  1. original `com.palantir.javaformat.java.Formatter`;
  2. Proton bundled vendor formatter derived from Proton's runtime package, normally
     `org.realityforge.proton.vendor.javaformat.java.Formatter`.
- Bundled formatter classes use the `org.realityforge.proton.vendor` namespace before any downstream processor
  relocation.
- Formatted writes manually create the source file and preserve JavaPoet originating elements.
- Failed writes after source-file creation delete the partially created file before rethrowing.
- `AbstractProcessorTest.assertSuccessfulCompile(...)` runs both unformatted and formatted successful fixture compiles.
- `output_fixture_data=true` emits unformatted fixtures under `expected` and formatted fixtures under
  `expectedFormatted`.
- Proton strips Java 21 multi-release entries from the shaded formatter jar content.

## Command Surface And Behavior

### Processor Option

- Every processor extending `AbstractStandardProcessor` supports `<prefix>.format_generated_source`.
- Default is `false`.
- `true` formats JavaPoet source before writing it.
- `false` uses the existing JavaPoet `JavaFile.writeTo(Filer)` path.

### Supported Options

- `AbstractStandardProcessor.getSupportedOptions()` must include common Proton options scoped by `getOptionPrefix()`,
  including `<prefix>.format_generated_source`.
- The common supported option set is:
  - `<prefix>.verbose_out_of_round.errors`
  - `<prefix>.defer.errors`
  - `<prefix>.defer.unresolved`
  - `<prefix>.debug`
  - `<prefix>.profile`
  - `<prefix>.warnings_as_errors`
  - `<prefix>.format_generated_source`
- Existing subclass-declared `@SupportedOptions` remain supported.
- This should prevent javac warnings for the new common option without forcing every downstream processor to edit its
  annotation.

### Formatter Failure Diagnostics

When formatting is enabled and formatter setup or formatting fails, the thrown `IOException` must include:

- the enabled option key, such as `<prefix>.format_generated_source=true`;
- the original formatter class name;
- the derived Proton vendor formatter class name;
- a clear missing-class explanation when `ClassNotFoundException` occurs;
- the JDK 16+ module exports required by `palantir-java-format`;
- a note that downstream processors shading Proton should include Proton's bundled vendor classes in their processor jar.

### JDK Exports

The required formatter exports are:

- `--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED`
- `--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED`
- `--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED`
- `--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED`
- `--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED`
- `--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED`

Proton QA should expose these as a reusable helper/constant for downstream test configuration and diagnostic assertions.

## Packaging Expectations

- Add the exact Arez-validated formatter dependency closure to `build.yaml`:
  - `com.palantir.javaformat:palantir-java-format:jar:2.92.0`
  - `com.palantir.javaformat:palantir-java-format-spi:jar:2.92.0`
  - `com.google.guava:guava:jar:33.6.0-jre`
  - `com.google.guava:failureaccess:jar:1.0.3`
  - `com.google.guava:listenablefuture:jar:9999.0-empty-to-avoid-conflict-with-guava`
  - `org.jspecify:jspecify:jar:1.0.0`
  - `com.google.errorprone:error_prone_annotations:jar:2.49.0`
  - `com.google.j2objc:j2objc-annotations:jar:3.1`
  - `org.functionaljava:functionaljava:jar:5.0`
  - `com.fasterxml.jackson.core:jackson-core:jar:2.21.1`
  - `com.fasterxml.jackson.core:jackson-databind:jar:2.21.1`
  - `com.fasterxml.jackson.core:jackson-annotations:jar:2.21`
  - `com.fasterxml.jackson.datatype:jackson-datatype-jdk8:jar:2.21.1`
  - `com.fasterxml.jackson.datatype:jackson-datatype-guava:jar:2.21.1`
  - `com.fasterxml.jackson.module:jackson-module-parameter-names:jar:2.21.1`
- Merge the formatter dependency closure into `proton-core.jar`.
- Relocate bundled formatter dependencies with this map:
  - `com.palantir.javaformat` -> `org.realityforge.proton.vendor.javaformat`
  - `com.google.common` -> `org.realityforge.proton.vendor.google.common`
  - `com.google.thirdparty` -> `org.realityforge.proton.vendor.google.thirdparty`
  - `com.google.errorprone` -> `org.realityforge.proton.vendor.google.errorprone`
  - `com.google.j2objc` -> `org.realityforge.proton.vendor.google.j2objc`
  - `org.jspecify` -> `org.realityforge.proton.vendor.jspecify`
  - `fj` -> `org.realityforge.proton.vendor.fj`
  - `com.fasterxml.jackson` -> `org.realityforge.proton.vendor.jackson`
- Preserve normal Proton API packages.
- Avoid publishing formatter dependencies as normal transitive `proton-core` dependencies, using an explicit Buildr
  dependency-filtering mechanism and POM inspection validation.
- Preserve existing non-formatter `proton-core` dependency metadata, including `javax.annotation`, `javax.json`,
  `javapoet`, and Proton's existing non-formatter Guava dependency.
- `proton-qa.jar` should not embed a second formatter copy; it depends on core.

## QA Harness Expectations

- Standard successful fixture helpers run twice:
  - existing options against `fixtures/expected`;
  - same options plus `-A<prefix>.format_generated_source=true` against `fixtures/expectedFormatted`.
- Both compiles use the same expected output list and generated-file filter.
- Both generated source and non-class class-output resources continue to be supported.
- Lower-level `compile(...)` APIs remain available for custom tests that intentionally bypass the standard fixture path.

## Proton Regression Coverage

Add minimal Proton-local test fixtures and test processors that verify:

- default generation still matches `expected`;
- formatted generation matches `expectedFormatted`;
- `output_fixture_data=true` writes both fixture trees;
- common supported options include `<prefix>.format_generated_source`;
- missing formatter / formatter setup diagnostics mention option key, class lookup targets, and JDK exports;
- packaged `proton-core.jar` contains vendor formatter/dependency classes and omits unrelocated formatter dependency
  packages for JavaFormat, Guava, Jackson, FunctionalJava, jspecify, error-prone, and j2objc;
- generated `proton-core` POM metadata does not publish formatter closure artifacts as transitive dependencies and still
  publishes existing non-formatter dependencies;
- `proton:qa:test` runs the new QA tests under the full gate with fixture properties and formatter JDK exports;
- an isolated external `javac` compile can format source when only packaged Proton artifacts and required
  non-formatter dependencies are reachable on the explicit classpath and processor path.

## Quality Gates

Targeted checks while implementing:

- `bundle exec buildr proton:core:compile`
- `bundle exec buildr proton:qa:compile`
- targeted Proton test commands for new core/qa tests
- jar inspection command for packaged `proton-core.jar`
- generated POM inspection for formatter dependency leakage
- generated POM inspection for existing non-formatter dependency preservation
- isolated external `javac` smoke compile using explicit classpath and processor path entries

Required full gate:

```bash
bundle exec buildr clean package test
```

This was selected after `bundle exec buildr --tasks` showed no Proton `ci` task.

## Documentation And Release Notes

- Update `CHANGELOG.md` under `Unreleased`.
- Update `README.md` or the nearest available project documentation with concise usage notes:
  - option key pattern;
  - default behavior;
  - bundled vendor formatter fallback;
  - JDK exports required when enabling formatting on JDK 16+;
  - formatted fixture expectations for `AbstractProcessorTest`.

## Known Intentional Divergences From Arez

- Arez currently owns formatter integration in `ArezProcessor`; Proton should move this behavior into
  `AbstractStandardProcessor`.
- Arez made downstream processor jars manage formatter dependencies; Proton should instead embed the shaded formatter
  fallback in `proton-core.jar`.
- Proton should first allow an original formatter on the processor path to win, then fall back to its bundled vendor copy.

## Open Questions Register

All design questions are resolved.

| id | status | question | user_decision | artifacts_updated |
| --- | --- | --- | --- | --- |
| Q-01 | resolved | Should Proton core make formatter support runtime-optional instead of a normal published transitive dependency? | Use runtime-optional behavior; no normal published transitive formatter dependency. | 00-requirements.md, 10-implementation-plan.md |
| Q-02 | resolved | Should the option be common as `<optionPrefix>.format_generated_source` and automatically advertised? | Yes. | 00-requirements.md, 10-implementation-plan.md |
| Q-03 | resolved | Should Proton call the formatter reflectively? | Yes. | 00-requirements.md, 10-implementation-plan.md |
| Q-04 | resolved | Should Proton embed shaded Palantir formatter support rather than requiring downstreams to manage dependencies? | Yes, Proton owns shaded fallback. | 00-requirements.md, 10-implementation-plan.md |
| Q-05 | resolved | What vendor namespace should Proton use? | `org.realityforge.proton.vendor`. | 00-requirements.md, 10-implementation-plan.md |
| Q-06 | resolved | Should lookup prefer original formatter before the Proton vendor fallback? | Yes. | 00-requirements.md, 10-implementation-plan.md |
| Q-07 | resolved | Should reflection be used even for the bundled formatter? | Yes. | 00-requirements.md, 10-implementation-plan.md |
| Q-08 | resolved | Should successful fixture compiles run against both `expected` and `expectedFormatted`? | Yes. | 00-requirements.md, 10-implementation-plan.md |
| Q-09 | resolved | Should there be a standard formatted-fixture opt-out? | No. | 00-requirements.md, 10-implementation-plan.md |
| Q-10 | resolved | Should Proton expose formatter JDK exports helpers in QA? | Yes. | 00-requirements.md, 10-implementation-plan.md |
| Q-11 | resolved | Should formatter failure diagnostics cover missing classes and missing exports? | Yes. | 00-requirements.md, 10-implementation-plan.md |
| Q-12 | resolved | Should Proton add direct regression tests? | Yes. | 00-requirements.md, 10-implementation-plan.md |
| Q-13 | resolved | Should formatter internals become public extension points? | No; expose only small downstream-needed hooks. | 00-requirements.md, 10-implementation-plan.md |
| Q-14 | resolved | Should formatted writes preserve JavaPoet originating elements? | Yes. | 00-requirements.md, 10-implementation-plan.md |
| Q-15 | resolved | Should partial source files be deleted on formatted write failure? | Yes. | 00-requirements.md, 10-implementation-plan.md |
| Q-16 | resolved | Should shading live in `proton-core.jar`, leaving QA to depend on core? | Yes. | 00-requirements.md, 10-implementation-plan.md |
| Q-17 | resolved | Should Java 21 multi-release formatter entries be stripped? | Yes. | 00-requirements.md, 10-implementation-plan.md |
| Q-18 | resolved | Should docs and changelog be updated? | Yes. | 00-requirements.md, 10-implementation-plan.md |
| Q-19 | resolved | Should Proton use the same formatter versions Arez used? | Yes. | 00-requirements.md, 10-implementation-plan.md |
| Q-20 | resolved | Should Proton add packaged-jar smoke and jar inspection validation? | Yes. | 00-requirements.md, 10-implementation-plan.md |
| Q-21 | resolved | Should vendor fallback class name be dynamically derived after downstream relocation? | Yes. | 00-requirements.md, 10-implementation-plan.md |
| Q-22 | resolved | What is the full validation gate? | Use `bundle exec buildr clean package test`. | 00-requirements.md, 10-implementation-plan.md |
