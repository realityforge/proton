# Proton Generated Source Formatting Implementation Plan

Status: accepted
Last updated: 2026-06-13

## Phase Sequence

1. Package formatter support in Proton core.
2. Add core processor formatting behavior.
3. Extend QA fixture infrastructure.
4. Add Proton-local regression and packaging tests.
5. Update documentation and release notes.
6. Run full validation and request final implementation approval.

## Delivery Approach

- Keep implementation centralized in `core` and `qa`.
- Preserve existing default behavior unless `<prefix>.format_generated_source=true`.
- Use reflection for formatter loading and invocation.
- Validate package promises with both tests and jar inspection.
- Keep downstream processors simple: upgrade Proton, shade Proton as they already do, and add JDK exports when enabling
  formatting in tests or builds.

## Detailed Plan

### Phase 1: Core Build And Shading

- Update `build.yaml` with the exact Arez-validated formatter dependency closure:
  - `palantir_java_format`: `com.palantir.javaformat:palantir-java-format:jar:2.92.0`
  - `palantir_java_format_spi`: `com.palantir.javaformat:palantir-java-format-spi:jar:2.92.0`
  - `palantir_java_format_guava`: `com.google.guava:guava:jar:33.6.0-jre`
  - `failureaccess`: `com.google.guava:failureaccess:jar:1.0.3`
  - `listenablefuture`: `com.google.guava:listenablefuture:jar:9999.0-empty-to-avoid-conflict-with-guava`
  - `jspecify`: `org.jspecify:jspecify:jar:1.0.0`
  - `error_prone_annotations`: `com.google.errorprone:error_prone_annotations:jar:2.49.0`
  - `j2objc_annotations`: `com.google.j2objc:j2objc-annotations:jar:3.1`
  - `functionaljava`: `org.functionaljava:functionaljava:jar:5.0`
  - `jackson_core`: `com.fasterxml.jackson.core:jackson-core:jar:2.21.1`
  - `jackson_databind`: `com.fasterxml.jackson.core:jackson-databind:jar:2.21.1`
  - `jackson_annotations`: `com.fasterxml.jackson.core:jackson-annotations:jar:2.21`
  - `jackson_datatype_jdk8`: `com.fasterxml.jackson.datatype:jackson-datatype-jdk8:jar:2.21.1`
  - `jackson_datatype_guava`: `com.fasterxml.jackson.datatype:jackson-datatype-guava:jar:2.21.1`
  - `jackson_module_parameter_names`: `com.fasterxml.jackson.module:jackson-module-parameter-names:jar:2.21.1`
- Update `buildfile`:
  - require `buildr/shade`;
  - define `FORMATTER_DEPS`;
  - compile `core` with formatter artifacts as needed;
  - merge formatter artifacts into `proton-core.jar`;
  - strip `META-INF/versions/21/**`;
  - relocate formatter dependencies using this map:
    - `com.palantir.javaformat` -> `org.realityforge.proton.vendor.javaformat`
    - `com.google.common` -> `org.realityforge.proton.vendor.google.common`
    - `com.google.thirdparty` -> `org.realityforge.proton.vendor.google.thirdparty`
    - `com.google.errorprone` -> `org.realityforge.proton.vendor.google.errorprone`
    - `com.google.j2objc` -> `org.realityforge.proton.vendor.google.j2objc`
    - `org.jspecify` -> `org.realityforge.proton.vendor.jspecify`
    - `fj` -> `org.realityforge.proton.vendor.fj`
    - `com.fasterxml.jackson` -> `org.realityforge.proton.vendor.jackson`
  - keep formatter dependencies out of published transitive dependencies with `pom.dependency_filter` or the
    equivalent Buildr mechanism;
  - preserve existing published non-formatter dependency metadata for `javax.annotation`, `javax.json`, `javapoet`, and
    Proton's existing non-formatter Guava dependency.

### Phase 2: Core Processor Behavior

- Add a common `format_generated_source` option in `AbstractStandardProcessor`.
- Parse the option during `init(...)`, defaulting to `false`.
- Override `getSupportedOptions()` to union subclass-supported options with all Proton common options scoped by
  `getOptionPrefix()`:
  - `verbose_out_of_round.errors`
  - `defer.errors`
  - `defer.unresolved`
  - `debug`
  - `profile`
  - `warnings_as_errors`
  - `format_generated_source`
- Update `emitTypeSpec(...)`:
  - existing JavaPoet write path when formatting is disabled;
  - formatted path when enabled.
- Implement private/internal formatter helpers:
  - create `JavaFile` with `skipJavaLangImports(true)`;
  - load original formatter first;
  - derive and load Proton vendor fallback second;
  - invoke `Formatter.create().formatSource(String)` reflectively;
  - catch `ClassNotFoundException` and produce a clear missing formatter message;
  - catch formatter setup, invocation, linkage, and module-access failures and wrap in actionable `IOException`.
- Implement formatted source writing:
  - `Filer.createSourceFile(...)` with `typeSpec.originatingElements()`;
  - write formatted source;
  - delete partial source file if open/write fails.

### Phase 3: QA Fixture Infrastructure

- Extend `AbstractProcessorTest.assertSuccessfulCompile(...)` so it runs:
  - baseline compile against `expected`;
  - formatted compile with `-A<prefix>.format_generated_source=true` against `expectedFormatted`.
- Preserve generated-file filtering and class-output resource behavior for both runs.
- Update `outputFilesIfEnabled(...)` so fixture regeneration writes into the matching expected directory for each run.
- Add Proton-owned formatter JDK exports helper/constant in QA.

### Phase 4: Regression And Packaging Tests

- Add minimal Proton test fixtures for a test-only processor extending `AbstractStandardProcessor`.
- Wire the QA module so `proton:qa:test` runs new TestNG tests under the full gate, with fixture-directory properties and
  formatter JVM exports configured in `buildfile`.
- Cover:
  - default expected output;
  - formatted expected output;
  - all common options advertised through `getSupportedOptions()`;
  - diagnostic text for formatter setup failures, including option key, attempted class names, and JDK exports;
  - fixture regeneration behavior using a temporary fixture directory so normal tests prove both `expected` and
    `expectedFormatted` writes without mutating committed fixtures.
- Add jar packaging validation:
  - `proton-core.jar` contains `org/realityforge/proton/vendor/javaformat/...`;
  - expected vendor package entries exist for relocated Guava, Jackson, FunctionalJava, jspecify, error-prone, and
    j2objc support where the dependency contributes classes;
  - no unrelocated formatter dependency package entries remain for `com/palantir/javaformat`, `com/google/common`,
    `com/google/thirdparty`, `com/google/errorprone`, `com/google/j2objc`, `org/jspecify`, `fj`, or
    `com/fasterxml/jackson`;
  - `META-INF/versions/21/**` formatter entries are absent.
- Add generated POM validation for `proton-core` proving no formatter closure artifacts are published transitively.
- Add generated POM validation for `proton-core` proving existing non-formatter dependencies remain published.
- Add an isolated external `javac` smoke test proving formatting works using packaged Proton artifacts and required
  non-formatter dependencies only:
  - run in a separate process, not the ambient test JVM;
  - use explicit `-classpath` and `-processorpath`;
  - omit unrelocated formatter jars from both paths;
  - prove vendor fallback use by classpath/processorpath isolation, absence of unrelocated formatter jars/classes, and
    successful formatted output.

### Phase 5: Docs And Changelog

- Add `CHANGELOG.md` entry under `Unreleased`.
- Update `README.md` or nearest documentation with concise usage and downstream test notes.

### Phase 6: Validation

- Run targeted checks while iterating:
  - `bundle exec buildr proton:core:compile`
  - `bundle exec buildr proton:qa:compile`
  - `bundle exec buildr proton:qa:test`
  - focused tests for new core/qa behavior
  - jar inspection
  - generated POM inspection
  - isolated javac smoke compile
- Run full gate before marking implementation complete:

```bash
bundle exec buildr clean package test
```

## High-Risk Areas

- **Buildr shading semantics**
  - Impact: formatter classes may leak unrelocated or be missing from `proton-core.jar`.
  - Mitigation: inspect jar contents for the full formatter dependency package set and add automated packaging
    assertions.
- **Buildr test wiring**
  - Impact: new QA tests could compile but not execute in the full gate.
  - Mitigation: explicitly configure `proton:qa:test`, fixture properties, and formatter JVM exports; run
    `bundle exec buildr proton:qa:test` as a targeted gate.
- **Published POM leakage**
  - Impact: formatter dependencies could appear as transitive `proton-core` dependencies despite being shaded.
  - Mitigation: use explicit dependency filtering and inspect generated POM metadata for both formatter absence and
    preservation of existing non-formatter dependencies.
- **Reflection across downstream relocation**
  - Impact: shaded downstream processors may not find Proton's bundled vendor formatter.
  - Mitigation: derive fallback formatter class from Proton runtime package and validate with an isolated packaged-jar
    smoke test.
- **JDK module access**
  - Impact: formatting can fail on JDK 16+ without required `--add-exports`.
  - Mitigation: centralize exports helper and include exports in diagnostic text and docs.
- **Fixture helper behavior change**
  - Impact: downstream tests using `assertSuccessfulCompile(...)` will need `expectedFormatted` fixtures.
  - Mitigation: document the change, support fixture regeneration for both trees, and keep lower-level compile APIs
    available for custom flows.

## Required Full Gate

```bash
bundle exec buildr clean package test
```

## Completion Criteria

- All planned tasks completed.
- All resolved design decisions reflected in code, tests, docs, and task board.
- Jar inspection and isolated processor smoke test prove the shaded fallback works.
- Generated POM inspection proves formatter dependencies are not published transitively and existing non-formatter
  dependencies remain published.
- Full gate passes.
- Working tree is clean after approved commits, or any exception is explicitly documented.

## Decision Log

- Q-01: Implement runtime-optional formatting; keep formatter out of published transitive dependencies.
- Q-02: Add common `<prefix>.format_generated_source` option and advertise it centrally.
- Q-03: Use reflection for formatter access.
- Q-04: Proton embeds shaded formatter fallback so downstream processors do not manage formatter dependencies.
- Q-05: Use `org.realityforge.proton.vendor` as Proton's vendor namespace.
- Q-06: Prefer original formatter class before Proton vendor fallback.
- Q-07: Reflectively access both original and bundled formatter variants.
- Q-08: Run successful fixture compiles twice, using `expected` and `expectedFormatted`.
- Q-09: Do not add a standard opt-out for formatted fixture verification.
- Q-10: Add QA helper/constant for formatter JDK exports.
- Q-11: Include missing-class and module-export guidance in formatter errors.
- Q-12: Add Proton-local regression tests.
- Q-13: Keep formatter internals private/internal; expose only downstream-needed hooks.
- Q-14: Preserve originating elements in formatted writes.
- Q-15: Delete partial files when formatted write fails.
- Q-16: Put shaded formatter support in `proton-core.jar`; QA depends on core.
- Q-17: Strip Java 21 multi-release entries from packaged formatter content.
- Q-18: Update docs and changelog.
- Q-19: Use Arez-validated formatter versions.
- Q-20: Add packaged-jar smoke and jar inspection validation.
- Q-21: Dynamically derive the relocated vendor formatter fallback name.
- Q-22: Use `bundle exec buildr clean package test` as the required full gate.

## Review Fix Log

- 2026-06-13 round 1: Added explicit QA test wiring, formatter dependency closure, relocation map, POM filtering and
  inspection, full common supported option list, and strict external javac smoke-test isolation.
- 2026-06-13 round 2: Added POM preservation checks for existing non-formatter dependencies, aligned QA test wiring
  with the task that edits `buildfile`, and replaced the unobservable vendor-fallback output assertion with classpath
  isolation plus successful formatting.
- 2026-06-13 round 3: Made fixture-regeneration validation mandatory through a non-mutating temporary fixture directory
  test.

## User Review Checkpoint

The user reviewed the latest plan after iterative plan review and approved implementation on 2026-06-13.
