# Proton

[<img src="https://img.shields.io/maven-central/v/org.realityforge.proton/proton-core.svg?label=latest%20release"/>](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.realityforge.proton%22)

Proton is a library of utilities that simplify building annotation processors.

Proton requires Java 21 or later to run annotation processors. Generated sources can still target Java 17.

The code base has yet to develop useful documentation. For the source code and project support please visit
the [GitHub project](https://github.com/realityforge/proton).

# Bazel Dependencies

Use `@proton//:core` and `@proton//:qa` in Java `deps`. These public targets import the same binary and source JARs
used for Maven Central releases. Core includes the bundled formatter and depends on JavaPoet and JSpecify. QA depends
on TestNG and JSpecify; add core separately when your processor uses it.

To use an unreleased checkout beside your consumer project, add this to the consumer's `MODULE.bazel`:

```starlark
bazel_dep(name = "proton_project", repo_name = "proton")
local_path_override(module_name = "proton_project", path = "../proton")

archive_override(
    module_name = "rules_palantir_java_format",
    integrity = "sha256-4h/h0cVmPQ/7jkXnvW69rmABbfXOOzoXuBh3gMJDMk8=",
    strip_prefix = "rules_palantir_java_format-0.1.1",
    urls = ["https://github.com/realityforge/rules_palantir_java_format/releases/download/v0.1.1/rules_palantir_java_format-v0.1.1.tar.gz"],
)
```

The formatter rule module is not in the Bazel Central Registry, so its archive override must be declared by the
consumer. Bazel only applies overrides from the root module. Use a Java 21 or later toolchain; Proton's `.bazelrc`
is not inherited by consumer projects.

# JSpecify Nullability

Proton recognises JSpecify nullability and marks generated types with `@NullMarked`. Add
`org.jspecify:jspecify` to the compilation and processor classpaths. Use
`GeneratorUtil.getTypeName(TypeMirror)` to preserve explicit type-use annotations when building JavaPoet types.

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
