# Proton

[<img src="https://img.shields.io/maven-central/v/org.realityforge.proton/proton-core.svg?label=latest%20release"/>](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.realityforge.proton%22)

Proton is a library of utilities that simplify building annotation processors.

The code base has yet to develop useful documentation. For the source code and project support please visit
the [GitHub project](https://github.com/realityforge/proton).

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
