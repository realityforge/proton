package org.realityforge.proton;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.StandardLocation;
import org.testng.annotations.Test;

public final class ElementOriginTest {
    private static final String SOURCE = """
        package com.example;
        public class Origins {
          public class Inner {}
          public static class Nested {}
          public static class WrittenNested { public WrittenNested() {} }
          public void method() {}
        }
        class Implicit {}
        class Public { public Public() {} }
        class Package { Package() {} }
        class Protected { protected Protected() {} }
        class Private { private Private() {} }
        class Parameterized { Parameterized(int value) {} }
        enum ImplicitEnum { VALUE }
        enum ExplicitEnum { VALUE; ExplicitEnum() {} }
        record ImplicitRecord(int value) {}
        record ExplicitRecord(int value) { ExplicitRecord(int value) { this.value = value; } }
        record CompactRecord(int value) { CompactRecord {} }
        """;

    @Test
    @SuppressWarnings("BanClassLoader") // Load only Bazel's pinned compiler, keeping it off the test classpath.
    public void originsAgreeUnderJavacAndTurbine() throws Exception {
        final var processor = new OriginProcessor();
        TestUtil.compile(TestUtil.source("com.example.Origins", SOURCE), processor);
        assertTrue(processor._validated);

        final Path directory = Files.createTempDirectory("proton-origin");
        try (var loader = new URLClassLoader(
                new URL[] {
                    Path.of(System.getProperty("proton.turbine.jar")).toUri().toURL()
                },
                getClass().getClassLoader())) {
            final Path source = directory.resolve("Origins.java");
            Files.writeString(source, SOURCE);
            final Path output = directory.resolve("header.jar");
            loader.loadClass("com.google.turbine.main.Main")
                    .getMethod("compile", String[].class)
                    .invoke(null, (Object) new String[] {
                        "--output", output.toString(),
                        "--sources", source.toString(),
                        "--processors", OriginProcessor.class.getName(),
                        "--system", System.getProperty("java.home")
                    });
            try (var jar = new ZipFile(output.toFile())) {
                assertNotNull(jar.getEntry("origin-validated"), "Turbine must execute the origin assertions");
            }
        } finally {
            try (Stream<Path> paths = Files.walk(directory)) {
                for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    @Test
    public void preservesCompilerOrigins() {
        final Element constructor = TestUtil.proxy(
                Element.class,
                (self, method, args) ->
                        "getKind".equals(method.getName()) ? ElementKind.CONSTRUCTOR : TestUtil.unsupported(method));
        for (final Elements.Origin origin : Elements.Origin.values()) {
            final Elements elements = TestUtil.proxy(
                    Elements.class,
                    (self, method, args) ->
                            "getOrigin".equals(method.getName()) ? origin : TestUtil.unsupported(method));
            assertEquals(ElementsUtil.getOrigin(elements, constructor), origin);
        }
    }

    public static final class OriginProcessor extends AbstractProcessor {
        private boolean _validated;

        @Override
        public Set<String> getSupportedAnnotationTypes() {
            return Set.of("*");
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latestSupported();
        }

        @Override
        public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
            if (!_validated && !roundEnv.processingOver()) {
                final var expected = Map.of(
                        "Origins", Elements.Origin.MANDATED,
                        "Implicit", Elements.Origin.MANDATED,
                        "Inner", Elements.Origin.MANDATED,
                        "Nested", Elements.Origin.MANDATED,
                        "ImplicitEnum", Elements.Origin.MANDATED,
                        "ImplicitRecord", Elements.Origin.MANDATED);
                int count = 0;
                for (final Element root : roundEnv.getRootElements()) {
                    count += validateType((TypeElement) root, expected);
                }
                assertEquals(count, 15, "Every constructor case must be inspected");
                try (var writer = processingEnv
                        .getFiler()
                        .createResource(StandardLocation.CLASS_OUTPUT, "", "origin-validated")
                        .openWriter()) {
                    writer.write("validated");
                } catch (final java.io.IOException e) {
                    throw new IllegalStateException(e);
                }
                _validated = true;
            }
            return false;
        }

        private int validateType(final TypeElement type, final Map<String, Elements.Origin> expected) {
            final Elements elements = processingEnv.getElementUtils();
            assertEquals(ElementsUtil.getOrigin(elements, type), elements.getOrigin(type));
            int count = 0;
            for (final var constructor : ElementFilter.constructorsIn(type.getEnclosedElements())) {
                assertEquals(
                        ElementsUtil.getOrigin(elements, constructor),
                        expected.getOrDefault(type.getSimpleName().toString(), Elements.Origin.EXPLICIT),
                        type.getQualifiedName().toString());
                count++;
            }
            for (final var method : ElementFilter.methodsIn(type.getEnclosedElements())) {
                assertEquals(ElementsUtil.getOrigin(elements, method), elements.getOrigin(method));
            }
            for (final var nested : ElementFilter.typesIn(type.getEnclosedElements())) {
                count += validateType(nested, expected);
            }
            return count;
        }
    }
}
