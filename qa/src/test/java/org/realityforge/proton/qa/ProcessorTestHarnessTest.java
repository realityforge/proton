package org.realityforge.proton.qa;

import static org.testng.Assert.*;

import com.google.errorprone.BaseErrorProneJavaCompiler;
import com.google.errorprone.scanner.ScannerSupplier;
import com.uber.nullaway.JSpecifyUnrecognizedAnnotationLocation;
import com.uber.nullaway.NullAway;
import com.uber.nullaway.RequireExplicitNullMarking;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.xml.parsers.DocumentBuilderFactory;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class ProcessorTestHarnessTest extends AbstractProcessorTest {
    private record JavacResult(int exitCode, String output, String generatedSource, String processorPath) {}

    @Override
    protected String getOptionPrefix() {
        return "proton_test";
    }

    @Override
    protected Processor processor() {
        return new TestProcessor();
    }

    @Override
    protected JavaCompiler javaCompiler() {
        return new BaseErrorProneJavaCompiler(ScannerSupplier.fromBugCheckerClasses(
                NullAway.class, RequireExplicitNullMarking.class, JSpecifyUnrecognizedAnnotationLocation.class));
    }

    @Override
    protected List<String> getOptions() {
        final List<String> options = new ArrayList<>(super.getOptions());
        options.addAll(List.of(
                "--release",
                "17",
                "-XDcompilePolicy=simple",
                "-XDaddTypeAnnotationsToSymbol=true",
                "-Xep:NullAway:ERROR",
                "-Xep:RequireExplicitNullMarking:ERROR",
                "-Xep:JSpecifyUnrecognizedAnnotationLocation:ERROR",
                "-XepOpt:NullAway:JSpecifyMode=true",
                "-XepOpt:NullAway:OnlyNullMarked=true"));
        return options;
    }

    @Test
    public void generatedNullableDereferenceFailsNullAwayChecking() throws Exception {
        final Processor invalid = new SynthesizingProcessor("com.example.generated.Invalid", """
            package com.example.generated;
            @javax.annotation.processing.Generated("NullAwayControl")
            @org.jspecify.annotations.NullMarked
            public final class Invalid {
              public static int length(@org.jspecify.annotations.Nullable String value) {
                return value.length();
              }
              public static java.util.List<String> nested(java.util.List<@org.jspecify.annotations.Nullable String> values) {
                return values;
              }
            }
            """, 0);
        final Compilation compilation = CompileTestUtil.compile(
                javaCompiler(),
                inputs("com.example.Model"),
                getOptions(),
                List.of(new TestProcessor(), invalid),
                List.of());
        try {
            assertFalse(compilation.success());
            assertTrue(
                    compilation.diagnostics().stream()
                            .anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR
                                    && d.toString().contains("[NullAway]")
                                    && d.toString().contains("Invalid.java")),
                    describeFailureDiagnostics(compilation));
            assertEquals(
                    compilation.diagnostics().stream()
                            .filter(d -> d.getKind() == Diagnostic.Kind.ERROR
                                    && d.toString().contains("[NullAway]"))
                            .count(),
                    2L,
                    describeFailureDiagnostics(compilation));
            compilation.assertSourceOutputFilenamePresent("com/example/generated/GeneratedModel.java");
        } finally {
            deleteDir(compilation.sourceOutput());
            deleteDir(compilation.classOutput());
        }
    }

    @Test
    public void assertSuccessfulCompileChecksExpectedAndExpectedFormatted() throws Exception {
        assertSuccessfulCompile("com.example.Model", "com/example/generated/GeneratedModel.java");
    }

    @Test
    public void generatedCodeTargetsJava17() throws Exception {
        final Compilation compilation = CompileTestUtil.assertCompilesWithoutWarnings(
                javaCompiler(), inputs("com.example.Model"), getOptions(), processors(), List.of());
        try {
            final Path generatedClass = compilation.classOutput().resolve("com/example/generated/GeneratedModel.class");
            assertEquals(
                    ByteBuffer.wrap(Files.readAllBytes(generatedClass)).getShort(6),
                    (short) 61,
                    "Generated class must use Java 17 bytecode");
        } finally {
            deleteDir(compilation.sourceOutput());
            deleteDir(compilation.classOutput());
        }
    }

    @Test
    public void processorAdvertisesCommonSupportedOptions() {
        final Set<String> options = processor().getSupportedOptions();
        assertTrue(options.contains("proton_test.custom_option"));
        for (final String option : Arrays.asList(
                "verbose_out_of_round.errors",
                "defer.errors",
                "defer.unresolved",
                "debug",
                "profile",
                "warnings_as_errors",
                "format_generated_source")) {
            assertTrue(options.contains("proton_test." + option), "Missing option " + option);
        }
    }

    @Test
    public void formatterJdkExportsExposesRequiredExports() {
        assertEquals(formatterJdkExports().size(), 6);
        assertTrue(formatterJdkExports().contains("--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED"));
    }

    @Test
    public void outputFixtureDataWritesExpectedAndExpectedFormatted() throws Exception {
        final Path dir = Files.createTempDirectory("proton-fixtures");
        final String fixtureDir = System.getProperty("proton_test.fixture_dir");
        final String outputFixtureData = System.getProperty("proton_test.output_fixture_data");
        try {
            final Path input = dir.resolve("input/com/example/Model.java");
            Files.createDirectories(input.getParent());
            Files.writeString(
                    input,
                    "package com.example;\n" + "import org.realityforge.proton.qa.GenerateType;\n"
                            + "@org.jspecify.annotations.NullMarked\n"
                            + "@GenerateType\n"
                            + "public class Model {}\n");
            System.setProperty("proton_test.fixture_dir", dir.toString());
            System.setProperty("proton_test.output_fixture_data", "true");
            assertSuccessfulCompile("com.example.Model", "com/example/generated/GeneratedModel.java");
            assertTrue(Files.exists(dir.resolve("expected/com/example/generated/GeneratedModel.java")));
            assertTrue(Files.exists(dir.resolve("expectedFormatted/com/example/generated/GeneratedModel.java")));
        } finally {
            restoreProperty("proton_test.fixture_dir", fixtureDir);
            restoreProperty("proton_test.output_fixture_data", outputFixtureData);
            deleteDir(dir);
        }
    }

    @Test
    public void coreJarContainsRelocatedFormatterDependencies() throws Exception {
        final List<String> entries = jarEntries(coreJar());
        for (final String prefix : Arrays.asList(
                "org/realityforge/proton/vendor/javaformat/",
                "org/realityforge/proton/vendor/google/common/",
                "org/realityforge/proton/vendor/jackson/",
                "org/realityforge/proton/vendor/fj/",
                "org/realityforge/proton/vendor/jspecify/",
                "org/realityforge/proton/vendor/google/errorprone/",
                "org/realityforge/proton/vendor/google/j2objc/")) {
            assertTrue(entries.stream().anyMatch(e -> e.startsWith(prefix)), "Missing relocated package " + prefix);
        }
        for (final String prefix : Arrays.asList(
                "com/palantir/javaformat/",
                "com/google/common/",
                "com/google/thirdparty/",
                "com/google/errorprone/",
                "com/google/j2objc/",
                "org/jspecify/",
                "fj/",
                "com/fasterxml/jackson/",
                "META-INF/versions/21/")) {
            assertFalse(entries.stream().anyMatch(e -> e.startsWith(prefix)), "Unrelocated package " + prefix);
        }
    }

    @Test
    public void corePomUsesJSpecifyWithoutLegacyOrFormatterDependencies() throws Exception {
        final Document pom = loadPom(corePom());
        assertTrue(dependencyVersions(pom, "com.palantir.javaformat", "palantir-java-format")
                .isEmpty());
        assertTrue(dependencyVersions(pom, "com.palantir.javaformat", "palantir-java-format-spi")
                .isEmpty());
        assertTrue(dependencyVersions(pom, "com.google.guava", "failureaccess").isEmpty());
        assertTrue(
                dependencyVersions(pom, "com.google.guava", "listenablefuture").isEmpty());
        assertTrue(dependencyVersions(pom, "com.google.errorprone", "error_prone_annotations")
                .isEmpty());
        assertTrue(dependencyVersions(pom, "com.google.j2objc", "j2objc-annotations")
                .isEmpty());
        assertTrue(
                dependencyVersions(pom, "org.functionaljava", "functionaljava").isEmpty());
        assertTrue(dependencyVersions(pom, "com.fasterxml.jackson.core", "jackson-core")
                .isEmpty());
        assertTrue(dependencyVersions(pom, "com.fasterxml.jackson.core", "jackson-databind")
                .isEmpty());
        assertTrue(dependencyVersions(pom, "com.fasterxml.jackson.core", "jackson-annotations")
                .isEmpty());
        assertTrue(dependencyVersions(pom, "com.fasterxml.jackson.datatype", "jackson-datatype-jdk8")
                .isEmpty());
        assertTrue(dependencyVersions(pom, "com.fasterxml.jackson.datatype", "jackson-datatype-guava")
                .isEmpty());
        assertTrue(dependencyVersions(pom, "com.fasterxml.jackson.module", "jackson-module-parameter-names")
                .isEmpty());

        assertTrue(dependencyVersions(pom, "org.realityforge.javax.annotation", "javax.annotation")
                .isEmpty());
        assertTrue(dependencyVersions(pom, "org.glassfish", "javax.json").isEmpty());
        assertEquals(dependencyVersions(pom, "com.palantir.javapoet", "javapoet"), Arrays.asList("0.14.0"));
        assertTrue(dependencyVersions(pom, "com.google.guava", "guava").isEmpty());
        assertEquals(dependencyVersions(pom, "org.jspecify", "jspecify"), Arrays.asList("1.0.0"));
    }

    @Test
    public void qaPomOnlyDependsOnJSpecifyAndTestNG() throws Exception {
        final Document pom = loadPom(Path.of(System.getProperty("proton.qa.pom")));
        assertEquals(pom.getElementsByTagName("dependency").getLength(), 2);
        assertEquals(dependencyVersions(pom, "org.jspecify", "jspecify"), Arrays.asList("1.0.0"));
        assertEquals(dependencyVersions(pom, "org.testng", "testng"), Arrays.asList("6.8"));
    }

    @Test(groups = "external_javac")
    public void externalJavacUsesPackagedVendorFormatter() throws IOException, InterruptedException {
        final JavacResult result = runExternalJavac(coreJar());
        assertEquals(result.exitCode(), 0, result.output());
        assertFalse(result.processorPath().contains("palantir-java-format"));
        assertTrue(result.generatedSource().contains("public final class GeneratedModel"));
    }

    @Test(groups = "external_javac")
    public void externalJavacReportsMissingFormatter() throws IOException, InterruptedException {
        final JavacResult result = runExternalJavac(unshadedCoreJar());
        assertTrue(0 != result.exitCode(), "Expected javac failure but compilation succeeded");
        assertTrue(result.output().contains("proton_test.format_generated_source=true"), result.output());
        assertTrue(result.output().contains("com.palantir.javaformat.java.Formatter"), result.output());
        assertTrue(
                result.output().contains("org.realityforge.proton.vendor.javaformat.java.Formatter"), result.output());
        assertTrue(
                result.output().contains("--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED"),
                result.output());
    }

    private void restoreProperty(final String key, final String value) {
        if (null == value) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private Path coreJar() {
        return Path.of(System.getProperty("proton.core.jar"));
    }

    private Path corePom() {
        return Path.of(System.getProperty("proton.core.pom"));
    }

    private Path unshadedCoreJar() {
        return Path.of(System.getProperty("proton.core.unshaded.jar"));
    }

    private List<String> jarEntries(final Path jar) throws IOException {
        try (final var jarFile = new JarFile(jar.toFile())) {
            return jarFile.stream().map(ZipEntry::getName).toList();
        }
    }

    private Document loadPom(final Path path) throws Exception {
        final var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(path.toFile());
    }

    private List<String> dependencyVersions(final Document pom, final String groupId, final String artifactId) {
        final var versions = new ArrayList<String>();
        final NodeList dependencies = pom.getElementsByTagName("dependency");
        for (int i = 0; i < dependencies.getLength(); i++) {
            final var dependency = (Element) dependencies.item(i);
            if (groupId.equals(childText(dependency, "groupId"))
                    && artifactId.equals(childText(dependency, "artifactId"))) {
                versions.add(childText(dependency, "version"));
            }
        }
        return versions;
    }

    private String childText(final Element element, final String tagName) {
        final NodeList nodes = element.getElementsByTagName(tagName);
        return 0 == nodes.getLength() ? "" : nodes.item(0).getTextContent();
    }

    private JavacResult runExternalJavac(final Path corePath) throws IOException, InterruptedException {
        final Path dir = Files.createTempDirectory("proton-javac");
        try {
            final Path source = dir.resolve("src/com/example/Model.java");
            final Path sourceOutput = dir.resolve("generated");
            final Path classOutput = dir.resolve("classes");
            Files.createDirectories(source.getParent());
            Files.createDirectories(sourceOutput);
            Files.createDirectories(classOutput);
            Files.writeString(
                    source,
                    "package com.example;\n" + "import org.realityforge.proton.qa.GenerateType;\n"
                            + "@org.jspecify.annotations.NullMarked\n"
                            + "@GenerateType\n"
                            + "public class Model {}\n");

            final var testClasses = Path.of(System.getProperty("baseDir"));
            final String classPath = joinPaths(testClasses, Path.of(System.getProperty("proton.jspecify.jar")));
            final String processorPath = joinPaths(
                    testClasses,
                    corePath,
                    Path.of(System.getProperty("proton.jspecify.jar")),
                    Path.of(System.getProperty("proton.javapoet.jar")));
            assertFalse(processorPath.contains("palantir-java-format"));

            final var command = new ArrayList<String>();
            command.add(Path.of(System.getProperty("java.home"))
                    .resolve("bin/javac")
                    .toString());
            command.addAll(formatterJdkExports().stream().map(e -> "-J" + e).toList());
            command.add("--release");
            command.add("17");
            command.add("-classpath");
            command.add(classPath);
            command.add("-processorpath");
            command.add(processorPath);
            command.add("-processor");
            command.add(TestProcessor.class.getName());
            command.add("-Xlint:all,-processing");
            command.add("-implicit:none");
            command.add("-Aproton_test.defer.errors=false");
            command.add("-s");
            command.add(sourceOutput.toString());
            command.add("-d");
            command.add(classOutput.toString());
            command.add(source.toString());

            final var process =
                    new ProcessBuilder(command).redirectErrorStream(true).start();
            final var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            final int exitCode = process.waitFor();
            final Path generatedSource = sourceOutput.resolve("com/example/generated/GeneratedModel.java");
            if (0 == exitCode) {
                final Path generatedClass = classOutput.resolve("com/example/generated/GeneratedModel.class");
                assertEquals(
                        ByteBuffer.wrap(Files.readAllBytes(generatedClass)).getShort(6),
                        (short) 61,
                        "Generated class must use Java 17 bytecode");
            }
            return new JavacResult(
                    exitCode,
                    output,
                    Files.exists(generatedSource) ? Files.readString(generatedSource) : "",
                    processorPath);
        } finally {
            deleteDir(dir);
        }
    }

    private String joinPaths(final Path... paths) {
        return Arrays.stream(paths)
                .map(Path::toString)
                .reduce((a, b) -> a + File.pathSeparator + b)
                .orElse("");
    }

    @SuppressWarnings({"resource", "ResultOfMethodCallIgnored"})
    private void deleteDir(final Path directory) throws IOException {
        try (final Stream<Path> stream = Files.walk(directory)) {
            stream.sorted((a, b) -> b.compareTo(a)).map(Path::toFile).forEach(file -> assertTrue(file.delete()));
        }
    }
}
