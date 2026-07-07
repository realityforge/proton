package org.realityforge.proton;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.util.ElementFilter;
import org.testng.annotations.Test;

public final class TypesUtilTest {
    @Test
    public void typeInspectionHelpersDetectArraysRawTypesWildcardsAndDeprecation() throws Exception {
        final var processor = new TypeProcessor();

        TestUtil.compile(TestUtil.source("com.example.TypeTarget", """
            package com.example;
            import java.util.List;
            public final class TypeTarget<T extends TypeTarget.Old> {
              @Deprecated
              static final class Old {}
              @Deprecated
              static final class OldException extends Exception {}
              List<String[]> arrayNested;
              String[] array;
              List<String> noArray;
              List rawList;
              List<String> typedList;
              List<List> nestedRawList;
              List<?> wildcardList;
              List<String> noWildcard;
              Old old;
              List<Old> oldList;
              String string;
              List rawReturn(Old old) throws OldException { return null; }
              String clean(String value) { return value; }
            }
            """), processor);

        assertTrue(processor.wasValidated());
    }

    private static final class TypeProcessor extends TestUtil.TestProcessor {
        private boolean _validated;

        @Override
        public boolean process(
                final Set<? extends TypeElement> annotations,
                final javax.annotation.processing.RoundEnvironment roundEnv) {
            if (!_validated && !roundEnv.processingOver()) {
                final TypeElement target = processingEnv.getElementUtils().getTypeElement("com.example.TypeTarget");
                assertNotNull(target);
                final Map<String, VariableElement> fields =
                        ElementFilter.fieldsIn(target.getEnclosedElements()).stream()
                                .collect(Collectors.toMap(f -> f.getSimpleName().toString(), f -> f));

                assertTrue(
                        TypesUtil.containsArrayType(field(fields, "arrayNested").asType()));
                assertTrue(TypesUtil.containsArrayType(field(fields, "array").asType()));
                assertFalse(TypesUtil.containsArrayType(field(fields, "noArray").asType()));

                assertTrue(TypesUtil.containsRawType(field(fields, "rawList").asType()));
                assertTrue(
                        TypesUtil.containsRawType(field(fields, "nestedRawList").asType()));
                assertFalse(TypesUtil.containsRawType(field(fields, "typedList").asType()));

                assertTrue(
                        TypesUtil.containsWildcard(field(fields, "wildcardList").asType()));
                assertFalse(
                        TypesUtil.containsWildcard(field(fields, "noWildcard").asType()));

                assertTrue(TypesUtil.hasRawTypes(
                        processingEnv, field(fields, "rawList").asType()));
                assertTrue(TypesUtil.hasRawTypes(
                        processingEnv, field(fields, "nestedRawList").asType()));
                assertFalse(TypesUtil.hasRawTypes(
                        processingEnv, field(fields, "typedList").asType()));

                assertTrue(TypesUtil.isDeprecated(
                        processingEnv, field(fields, "old").asType()));
                assertTrue(TypesUtil.isDeprecated(
                        processingEnv, field(fields, "oldList").asType()));
                assertFalse(TypesUtil.isDeprecated(
                        processingEnv, field(fields, "string").asType()));

                final var targetType = (DeclaredType) target.asType();
                final ExecutableType rawReturn = executableType(targetType, method(target, "rawReturn"));
                final ExecutableType clean = executableType(targetType, method(target, "clean"));
                assertTrue(TypesUtil.hasRawTypes(processingEnv, rawReturn));
                assertFalse(TypesUtil.hasRawTypes(processingEnv, clean));
                assertTrue(TypesUtil.isDeprecated(processingEnv, rawReturn));
                assertFalse(TypesUtil.isDeprecated(processingEnv, clean));
                _validated = true;
            }
            return false;
        }

        boolean wasValidated() {
            return _validated;
        }

        private static VariableElement field(final Map<String, VariableElement> fields, final String name) {
            return Objects.requireNonNull(fields.get(name));
        }

        private ExecutableType executableType(final DeclaredType targetType, final ExecutableElement method) {
            return (ExecutableType) processingEnv.getTypeUtils().asMemberOf(targetType, method);
        }

        private static ExecutableElement method(final TypeElement type, final String name) {
            return ElementFilter.methodsIn(type.getEnclosedElements()).stream()
                    .filter(method -> name.contentEquals(method.getSimpleName()))
                    .findFirst()
                    .orElseThrow();
        }
    }
}
