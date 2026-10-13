# Proton

[<img src="https://img.shields.io/maven-central/v/org.realityforge.proton/proton-core.svg?label=latest%20release"/>](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.realityforge.proton%22)

Proton is a library of utilities that simplify building annotation processors.

The code base has yet to develop useful documentation. For the source code and project support please visit
the [GitHub project](https://github.com/realityforge/proton).

# JSpecify Nullability

The next release changes generated nullability to [JSpecify](https://jspecify.dev/). This is a breaking migration:
replace legacy `javax.annotation` nullability with `org.jspecify.annotations`, and put `jspecify:1.0.0` on the
compilation and processor paths. Proton no longer emits legacy annotations or publishes a legacy variant.
`AnnotationsUtil.hasNonnullAnnotation(...)` and both `NONNULL_CLASSNAME` constants have been removed.
The remaining `NULLABLE_CLASSNAME` constants now refer to JSpecify.

`AnnotationsUtil.hasNullableAnnotation(Element)` reads the outer return type of a method and the outer type of a
parameter or field. The `TypeMirror` overload checks the supplied type itself. Nested annotations do not make the
outer value nullable: `@Nullable String[]` has nullable components, `String @Nullable []` is a nullable array,
and `List<@Nullable String>` is a list with nullable elements.

Use `GeneratorUtil.getTypeName(TypeMirror)` when turning a javac type into a JavaPoet type. It preserves explicit
JSpecify annotations on declared types, generic arguments, arrays, wildcards' bounds and type-variable uses.
`copyTypeParameters(...)` preserves annotated bounds. `overrideMethod(...)` and `copyParameters(...)` preserve
annotations when resolving inherited signatures. The default declaration annotation whitelist copies `Deprecated`;
custom whitelist overloads remain available for framework annotations.

`AbstractStandardProcessor.emitTypeSpec(...)` adds `@NullMarked` to generated types. `refMethod(...)` keeps its
nonnull return contract while preserving nullable parameters and nested annotations. Concrete nonnull returns use
the marked default; type-variable returns use `@NonNull T`. Passing `false` to the existing override helper's
nullability flag removes only the outer return annotation. Unmarked consumer signatures remain supported: Proton
preserves explicit annotations without inferring nullability for unspecified nested types. This does not establish
checking of unmarked implementation bodies.

# Checking Processor Fixtures

Bazel checking does not automatically reach a processor test's in-process javac. To check generated fixture code,
override `AbstractProcessorTest.javaCompiler()` with a checking `JavaCompiler`, and configure its flags in
`getOptions()`. Every compilation helper uses that factory. `CompileTestUtil.compile(...)` and
`assertCompilesWithoutWarnings(...)` also have overloads taking a `JavaCompiler` as their first argument; existing
entry points continue to use the system compiler.

Proton's QA tests use Error Prone's `BaseErrorProneJavaCompiler` with `NullAway`, `RequireExplicitNullMarking` and
`JSpecifyUnrecognizedAnnotationLocation`, and these options:

```text
--release 17
-XDcompilePolicy=simple
-XDaddTypeAnnotationsToSymbol=true
-Xep:NullAway:ERROR
-Xep:RequireExplicitNullMarking:ERROR
-Xep:JSpecifyUnrecognizedAnnotationLocation:ERROR
-XepOpt:NullAway:JSpecifyMode=true
-XepOpt:NullAway:OnlyNullMarked=true
```

The checking compiler needs Error Prone, NullAway and their dependencies on the test runtime classpath, plus javac
JVM exports for `api`, `code`, `comp`, `file`, `jvm`, `main`, `model`, `parser`, `processing`, `tree` and `util`, and JVM opens
for `code` and `comp` under `jdk.compiler/com.sun.tools.javac`. The working configuration is in
`qa/src/test/java/org/realityforge/proton/qa/BUILD.bazel`. Error Prone remains a test dependency, rather than a required
Proton QA dependency.

Repository checks retain the remote Java 17 runtime and Java 17 output. Zulu 17.0.20 was verified to read binary
type-use annotations with `-XDaddTypeAnnotationsToSymbol=true`; the local Oracle JDK 17.0.12 discards them.
Compiler runtimes must support binary annotation reading to run [NullAway JSpecify mode](https://github.com/uber/NullAway/wiki/JSpecify-Support#supported-jdk-versions).
The QA suite includes valid generated fixtures and a failing generated control for nullable dereferences and generic
return types.

Local Bazel consumers can use these public targets before a release:

```text
//core/src/main/java/org/realityforge/proton:core
//qa/src/main/java/org/realityforge/proton/qa:qa
```

Processors using default source formatting should consume `//tools/release:proton_core_jar`, with
`//tools/release:proton_core_sources_jar` for sources. The raw `core` JavaInfo target does not bundle the formatter.
These artifacts build from the local checkout. A consuming root module must carry Proton's formatter
`archive_override` from `MODULE.bazel`; [only root-module overrides take effect](https://bazel.build/external/module#overrides).

# Generated Source Formatting

Processors extending `AbstractStandardProcessor` support the `<optionPrefix>.format_generated_source` option. The
option defaults to `true`; when enabled, Proton formats JavaPoet-generated source with palantir-java-format before
writing it through the annotation processing `Filer`.

Proton first looks for `com.palantir.javaformat.java.Formatter` on the processor path, then falls back to the formatter
copy bundled in `proton-core.jar` under `org.realityforge.proton.vendor`. Downstream processors that already shade
Proton should include Proton's bundled vendor classes in their processor jar; they do not need to declare formatter
dependencies directly.

On JDK 16+, javac needs these JVM exports when formatting is enabled:

```bash
--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
```

`AbstractProcessorTest.assertSuccessfulCompile(...)` verifies generated fixtures twice: unformatted output under
`fixtures/expected`, and formatted output under `fixtures/expectedFormatted`. Setting
`-D<optionPrefix>.output_fixture_data=true` regenerates both fixture trees.

# Contributing

The project was released as open source so others could benefit from the project. We are thankful for any
contributions from the community. A [Code of Conduct](CODE_OF_CONDUCT.md) has been put in place and
a [Contributing](CONTRIBUTING.md) document is under development.

# License

The project is licensed under [Apache License, Version 2.0](LICENSE).

# Credit

* [Stock Software](http://www.stocksoftware.com.au/) for providing significant support in building and maintaining
  the tools from which this was originally extracted.

* The `com.google.auto:auto-common` library on which proton was originally based. The `auto-common` library provided
  inspiration and initial variants for some of the code (i.e. `AnnotationsUtil.getAnnotationValuesWithDefaults()`).
  Other code (i.e. `SuperficialValidation`) was directly copied and reworked within the context of this library.
