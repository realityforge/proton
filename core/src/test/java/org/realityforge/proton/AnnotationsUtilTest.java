package org.realityforge.proton;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import org.testng.annotations.Test;

public final class AnnotationsUtilTest {
    private static final String MARKER = "com.example.Marker";
    private static final String NAMING = "com.example.Naming";
    private static final String TAG = "com.example.Tag";
    private static final String TAGS = "com.example.Tags";

    @Test
    public void annotationHelpersReadMirrorsValuesAndDefaults() throws Exception {
        final var processor = new AnnotationProcessor();

        TestUtil.compile(TestUtil.source("com.example.AnnotationTarget", """
            package com.example;
            import java.lang.annotation.Repeatable;
            import javax.annotation.Nonnull;
            import javax.annotation.Nullable;
            enum Mode { FAST, SLOW }
            @interface Marker {
              String name() default "defaultName";
              Mode mode() default Mode.FAST;
              Class<?>[] types() default {String.class};
            }
            @Repeatable(Tags.class)
            @interface Tag {
              String value();
            }
            @interface Tags {
              Tag[] value();
            }
            @interface Naming {
              String value() default "<default>";
            }
            @Marker(name = "explicit", mode = Mode.SLOW, types = {String.class, Integer.class})
            @Tag("alpha")
            @Tag("beta")
            public final class AnnotationTarget {
              @Marker
              String defaultMarker;
              @Nonnull
              String nonnullField;
              @Nullable
              String nullableField;
              @Tag("single")
              void singleTag() {}
              @Naming("renamed")
              void named() {}
              @Naming("<default>")
              void derived() {}
              @Naming("<default>")
              void missingDefault() {}
              @Naming("1bad")
              void invalidIdentifier() {}
              @Naming("class")
              void keywordIdentifier() {}
            }
            """), processor);

        assertTrue(processor.wasValidated());
    }

    private static final class AnnotationProcessor extends TestUtil.TestProcessor {
        private boolean _validated;

        @Override
        public boolean process(
                final Set<? extends TypeElement> annotations,
                final javax.annotation.processing.RoundEnvironment roundEnv) {
            if (!_validated && !roundEnv.processingOver()) {
                final TypeElement target =
                        processingEnv.getElementUtils().getTypeElement("com.example.AnnotationTarget");
                assertNotNull(target);
                validateTypeAnnotations(target);
                validateFieldAnnotations(target);
                validateMethodAnnotations(target);
                _validated = true;
            }
            return false;
        }

        boolean wasValidated() {
            return _validated;
        }

        private void validateTypeAnnotations(final TypeElement target) {
            assertTrue(AnnotationsUtil.hasAnnotationOfType(target, MARKER));
            assertFalse(AnnotationsUtil.hasAnnotationOfType(target, "com.example.Missing"));

            final AnnotationMirror marker = AnnotationsUtil.getAnnotationByType(target, MARKER);
            assertSame(AnnotationsUtil.findAnnotationByType(target, MARKER), marker);
            assertNull(AnnotationsUtil.findAnnotationByType(target, "com.example.Missing"));

            assertEquals(AnnotationsUtil.getAnnotationValueValue(marker, "name"), "explicit");
            assertEquals(
                    AnnotationsUtil.getAnnotationValue(target, MARKER, "name").getValue(), "explicit");
            assertEquals(
                    Objects.requireNonNull(AnnotationsUtil.findAnnotationValue(target, MARKER, "name"))
                            .getValue(),
                    "explicit");
            assertEquals(
                    Objects.requireNonNull(AnnotationsUtil.findAnnotationValueNoDefaults(marker, "name"))
                            .getValue(),
                    "explicit");
            assertEquals(AnnotationsUtil.getEnumAnnotationParameter(target, MARKER, "mode"), "SLOW");

            final List<String> typeNames =
                    AnnotationsUtil.getTypeMirrorsAnnotationParameter(target, MARKER, "types").stream()
                            .map(Object::toString)
                            .collect(Collectors.toList());
            assertEquals(typeNames, List.of("java.lang.String", "java.lang.Integer"));

            final List<AnnotationMirror> tags = AnnotationsUtil.getRepeatingAnnotations(target, TAGS, TAG);
            assertEquals(tagValues(tags), List.of("alpha", "beta"));
        }

        private void validateFieldAnnotations(final TypeElement target) {
            final VariableElement defaultMarker = field(target, "defaultMarker");
            final AnnotationMirror marker = AnnotationsUtil.getAnnotationByType(defaultMarker, MARKER);
            final Map<ExecutableElement, AnnotationValue> values =
                    AnnotationsUtil.getAnnotationValuesWithDefaults(marker);
            assertEquals(
                    values.keySet().stream()
                            .map(e -> e.getSimpleName().toString())
                            .collect(Collectors.toList()),
                    List.of("name", "mode", "types"));
            assertEquals(
                    Objects.requireNonNull(AnnotationsUtil.findAnnotationValue(marker, "name"))
                            .getValue(),
                    "defaultName");
            assertNull(AnnotationsUtil.findAnnotationValueNoDefaults(marker, "name"));

            assertTrue(AnnotationsUtil.hasNonnullAnnotation(field(target, "nonnullField")));
            assertTrue(AnnotationsUtil.hasNullableAnnotation(field(target, "nullableField")));
            assertFalse(AnnotationsUtil.hasNullableAnnotation(field(target, "nonnullField")));
        }

        private void validateMethodAnnotations(final TypeElement target) {
            assertEquals(
                    tagValues(AnnotationsUtil.getRepeatingAnnotations(method(target, "singleTag"), TAGS, TAG)),
                    List.of("single"));

            final Function<ExecutableElement, String> deriveName = method -> method.getSimpleName() + "Name";
            assertEquals(
                    AnnotationsUtil.extractName(method(target, "named"), deriveName, NAMING, "value", "<default>"),
                    "renamed");
            assertEquals(
                    AnnotationsUtil.extractName(method(target, "derived"), deriveName, NAMING, "value", "<default>"),
                    "derivedName");

            assertProcessorException(
                    () -> AnnotationsUtil.extractName(
                            method(target, "missingDefault"), method -> null, NAMING, "value", "<default>"),
                    "@Naming target did not specify the parameter value and the default value could not be derived");
            assertProcessorException(
                    () -> AnnotationsUtil.extractName(
                            method(target, "invalidIdentifier"), deriveName, NAMING, "value", "<default>"),
                    "@Naming target specified an invalid value '1bad' for the parameter value. "
                            + "The value must be a valid java identifier");
            assertProcessorException(
                    () -> AnnotationsUtil.extractName(
                            method(target, "keywordIdentifier"), deriveName, NAMING, "value", "<default>"),
                    "@Naming target specified an invalid value 'class' for the parameter value. "
                            + "The value must not be a java keyword");
        }

        private static List<String> tagValues(final List<AnnotationMirror> annotations) {
            return annotations.stream()
                    .map(a -> (String) AnnotationsUtil.getAnnotationValueValue(a, "value"))
                    .collect(Collectors.toList());
        }

        private static VariableElement field(final TypeElement type, final String name) {
            return ElementFilter.fieldsIn(type.getEnclosedElements()).stream()
                    .filter(field -> name.contentEquals(field.getSimpleName()))
                    .findFirst()
                    .orElseThrow();
        }

        private static ExecutableElement method(final TypeElement type, final String name) {
            return ElementFilter.methodsIn(type.getEnclosedElements()).stream()
                    .filter(method -> name.contentEquals(method.getSimpleName()))
                    .findFirst()
                    .orElseThrow();
        }

        private static void assertProcessorException(final ThrowingRunnable runnable, final String message) {
            try {
                runnable.run();
                fail("Expected ProcessorException");
            } catch (final ProcessorException e) {
                assertEquals(e.getMessage(), message);
                assertNotNull(e.getElement());
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
