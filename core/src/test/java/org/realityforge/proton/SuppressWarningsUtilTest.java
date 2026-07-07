package org.realityforge.proton;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.testng.annotations.Test;

public final class SuppressWarningsUtilTest {
    @Test
    public void maybeSuppressWarningsAnnotationReturnsNullWhenNoWarningsRemain() {
        assertNull(SuppressWarningsUtil.maybeSuppressWarningsAnnotation((String) null));
    }

    @Test
    public void suppressWarningsAnnotationSortsAndFiltersWarnings() {
        final AnnotationSpec annotation =
                SuppressWarningsUtil.suppressWarningsAnnotation("unchecked", null, "deprecation");

        assertEquals(annotation.toString(), "@java.lang.SuppressWarnings({\"deprecation\", \"unchecked\"})");
    }

    @Test
    public void processingEnvSuppressWarningsHelpersDetectRawAndDeprecatedTypes() throws Exception {
        final SuppressProcessor processor = new SuppressProcessor();

        TestUtil.compile(TestUtil.source("com.example.SuppressTarget", """
            package com.example;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;
            import java.util.List;
            @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
            @interface SuppressCustomWarnings {
              String[] value();
            }
            @SuppressWarnings("fromType")
            @SuppressCustomWarnings("customFromType")
            public final class SuppressTarget {
              @Deprecated
              static final class Old {}
              List rawList;
              List<String> typedList;
              Old old;
              String string;
              @SuppressWarnings({"fromField", "shared"})
              String field;
              @SuppressCustomWarnings({"customFromMethod", "sharedCustom"})
              void action() {}
            }
            """), processor);

        assertTrue(processor.wasValidated());
    }

    private static final class SuppressProcessor extends TestUtil.TestProcessor {
        private boolean _validated;

        @Override
        public boolean process(
                final Set<? extends TypeElement> annotations,
                final javax.annotation.processing.RoundEnvironment roundEnv) {
            if (!_validated && !roundEnv.processingOver()) {
                final TypeElement target = processingEnv.getElementUtils().getTypeElement("com.example.SuppressTarget");
                assertNotNull(target);
                final Map<String, VariableElement> fields =
                        ElementFilter.fieldsIn(target.getEnclosedElements()).stream()
                                .collect(Collectors.toMap(f -> f.getSimpleName().toString(), f -> f));
                validateAnnotationGeneration(fields);
                validateBuilderHelpers(fields);
                validateReadHelpers(target, fields);
                _validated = true;
            }
            return false;
        }

        boolean wasValidated() {
            return _validated;
        }

        private void validateAnnotationGeneration(@Nonnull final Map<String, VariableElement> fields) {
            assertNull(SuppressWarningsUtil.maybeSuppressWarningsAnnotation(
                    processingEnv,
                    List.of(
                            fields.get("typedList").asType(),
                            fields.get("string").asType())));
            assertEquals(
                    SuppressWarningsUtil.maybeSuppressWarningsAnnotation(
                                    processingEnv, List.of(fields.get("rawList").asType()))
                            .toString(),
                    "@java.lang.SuppressWarnings(\"rawtypes\")");
            assertEquals(
                    SuppressWarningsUtil.maybeSuppressWarningsAnnotation(
                                    processingEnv, List.of(fields.get("old").asType()))
                            .toString(),
                    "@java.lang.SuppressWarnings(\"deprecation\")");
            assertEquals(
                    SuppressWarningsUtil.maybeSuppressWarningsAnnotation(
                                    processingEnv,
                                    List.of("unchecked", "rawtypes"),
                                    List.of(
                                            fields.get("old").asType(),
                                            fields.get("rawList").asType()))
                            .toString(),
                    "@java.lang.SuppressWarnings({\"deprecation\", \"rawtypes\", \"unchecked\"})");
        }

        private void validateBuilderHelpers(@Nonnull final Map<String, VariableElement> fields) {
            final TypeMirror rawList = fields.get("rawList").asType();

            final TypeSpec.Builder type = TypeSpec.classBuilder("Target");
            SuppressWarningsUtil.addSuppressWarningsIfRequired(processingEnv, type, rawList);
            assertEquals(
                    suppressWarningsAnnotation(type.build().annotations()),
                    "@java.lang.SuppressWarnings(\"rawtypes\")");

            final MethodSpec.Builder method = MethodSpec.methodBuilder("target");
            SuppressWarningsUtil.addSuppressWarningsIfRequired(
                    processingEnv, method, List.of("unchecked"), List.of(rawList));
            assertEquals(
                    suppressWarningsAnnotation(method.build().annotations()),
                    "@java.lang.SuppressWarnings({\"rawtypes\", \"unchecked\"})");

            final FieldSpec.Builder field = FieldSpec.builder(TypeName.get(String.class), "target");
            SuppressWarningsUtil.addSuppressWarningsIfRequired(
                    processingEnv, field, fields.get("old").asType());
            assertEquals(
                    suppressWarningsAnnotation(field.build().annotations()),
                    "@java.lang.SuppressWarnings(\"deprecation\")");

            final ParameterSpec.Builder parameter = ParameterSpec.builder(TypeName.get(String.class), "target");
            SuppressWarningsUtil.addSuppressWarningsIfRequired(processingEnv, parameter, List.of(rawList));
            assertEquals(
                    suppressWarningsAnnotation(parameter.build().annotations()),
                    "@java.lang.SuppressWarnings(\"rawtypes\")");
        }

        private static void validateReadHelpers(
                @Nonnull final TypeElement target, @Nonnull final Map<String, VariableElement> fields) {
            final VariableElement field = fields.get("field");
            final ExecutableElement method = method(target, "action");
            final String customSuppressWarnings = "com.example.SuppressCustomWarnings";

            assertTrue(SuppressWarningsUtil.isSuppressed((AnnotatedConstruct) target, "fromType"));
            assertFalse(SuppressWarningsUtil.isSuppressed((AnnotatedConstruct) method, "fromType"));
            assertTrue(SuppressWarningsUtil.isSuppressed(method, "fromType"));
            assertTrue(SuppressWarningsUtil.isSuppressed(field, "fromField"));
            assertTrue(SuppressWarningsUtil.isSuppressed(field, "shared"));
            assertFalse(SuppressWarningsUtil.isSuppressed(field, "missing"));
            assertTrue(SuppressWarningsUtil.isNotSuppressed(field, "missing"));
            assertFalse(SuppressWarningsUtil.isNotSuppressed(field, "shared"));

            assertTrue(SuppressWarningsUtil.isSuppressed(method, "customFromMethod", customSuppressWarnings));
            assertTrue(SuppressWarningsUtil.isSuppressed(method, "customFromType", customSuppressWarnings));
            assertFalse(SuppressWarningsUtil.isSuppressed(
                    (AnnotatedConstruct) method, "customFromType", customSuppressWarnings));
            assertTrue(SuppressWarningsUtil.isNotSuppressed(method, "missing", customSuppressWarnings));
            assertTrue(ElementsUtil.isWarningSuppressed(method, "customFromMethod", customSuppressWarnings));
            assertTrue(ElementsUtil.isWarningNotSuppressed(method, "missing", customSuppressWarnings));
        }

        @Nonnull
        private static String suppressWarningsAnnotation(@Nonnull final List<AnnotationSpec> annotations) {
            return annotations.stream()
                    .map(AnnotationSpec::toString)
                    .filter(annotation -> annotation.contains("SuppressWarnings"))
                    .findFirst()
                    .orElseThrow();
        }

        @Nonnull
        private static ExecutableElement method(@Nonnull final TypeElement type, @Nonnull final String name) {
            return ElementFilter.methodsIn(type.getEnclosedElements()).stream()
                    .filter(method -> name.contentEquals(method.getSimpleName()))
                    .findFirst()
                    .orElseThrow();
        }
    }
}
