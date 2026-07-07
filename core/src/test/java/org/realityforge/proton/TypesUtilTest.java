package org.realityforge.proton;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
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
        final TypeProcessor processor = new TypeProcessor();

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

                assertTrue(TypesUtil.containsArrayType(fields.get("arrayNested").asType()));
                assertTrue(TypesUtil.containsArrayType(fields.get("array").asType()));
                assertFalse(TypesUtil.containsArrayType(fields.get("noArray").asType()));

                assertTrue(TypesUtil.containsRawType(fields.get("rawList").asType()));
                assertTrue(TypesUtil.containsRawType(fields.get("nestedRawList").asType()));
                assertFalse(TypesUtil.containsRawType(fields.get("typedList").asType()));

                assertTrue(TypesUtil.containsWildcard(fields.get("wildcardList").asType()));
                assertFalse(TypesUtil.containsWildcard(fields.get("noWildcard").asType()));

                assertTrue(TypesUtil.hasRawTypes(
                        processingEnv, fields.get("rawList").asType()));
                assertTrue(TypesUtil.hasRawTypes(
                        processingEnv, fields.get("nestedRawList").asType()));
                assertFalse(TypesUtil.hasRawTypes(
                        processingEnv, fields.get("typedList").asType()));

                assertTrue(
                        TypesUtil.isDeprecated(processingEnv, fields.get("old").asType()));
                assertTrue(TypesUtil.isDeprecated(
                        processingEnv, fields.get("oldList").asType()));
                assertFalse(TypesUtil.isDeprecated(
                        processingEnv, fields.get("string").asType()));

                final DeclaredType targetType = (DeclaredType) target.asType();
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

        @Nonnull
        private ExecutableType executableType(
                @Nonnull final DeclaredType targetType, @Nonnull final ExecutableElement method) {
            return (ExecutableType) processingEnv.getTypeUtils().asMemberOf(targetType, method);
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
