package org.realityforge.proton;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import org.jspecify.annotations.Nullable;
import org.testng.annotations.Test;

public final class MemberChecksTest {
    private static final String SCOPE = "com.example.Scope";

    @Test
    public void messageHelpersFormatAnnotationNamesAndSuppressionInstructions() {
        assertEquals(MemberChecks.toSimpleName("com.example.Scope"), "@Scope");
        assertEquals(MemberChecks.must(SCOPE, "be static"), "@Scope target must be static");
        assertEquals(MemberChecks.mustNot(SCOPE, "be static"), "@Scope target must not be static");
        assertEquals(MemberChecks.should(SCOPE, "be protected"), "@Scope target should be protected");
        assertEquals(MemberChecks.shouldNot(SCOPE, "be public"), "@Scope target should not be public");
        assertEquals(
                MemberChecks.suppressedBy("warning"),
                "This warning can be suppressed by annotating the element with @SuppressWarnings( \"warning\" )");
        assertEquals(
                MemberChecks.suppressedBy("warning", "com.example.AltSuppress"),
                "This warning can be suppressed by annotating the element with @SuppressWarnings( \"warning\" ) "
                        + "or @AltSuppress( \"warning\" )");
    }

    @Test
    public void memberChecksValidateModifiersSignaturesAnnotationsAndWarnings() throws Exception {
        final var processor = new MemberProcessor();

        TestUtil.compile(
                List.of(TestUtil.source("com.example.MemberTarget", """
                    package com.example;
                    import java.io.IOException;
                    @interface One {}
                    @interface Two {}
                    @interface Three {}
                    interface Api {
                      void api();
                    }
                    public abstract class MemberTarget implements Api {
                      public void publicMethod() {}
                      @SuppressWarnings("public-method")
                      public void publicSuppressed() {}
                      protected void protectedMethod() {}
                      private void privateMethod() {}
                      static void staticMethod() {}
                      final void finalMethod() {}
                      abstract void abstractMethod();
                      void packageMethod() {}
                      void parameterMethod(String value) {}
                      <T> void typeParameterMethod() {}
                      String valueMethod() { return ""; }
                      void throwsMethod() throws IOException {}
                      @One @Two void overlapping() {}
                      @One @Three void allowedOverlap() {}
                      @Override public void api() {}
                    }
                    class PlainClass {}
                    interface Contract {}
                    enum Mode { ACTIVE }
                    class Generic<T> {}
                    class Outer {
                      class Inner {}
                      static class StaticNested {}
                    }
                    """), TestUtil.source("com.other.OtherBase", """
                        package com.other;
                        public class OtherBase {
                          void packageMethod() {}
                        }
                        """)),
                processor);

        assertTrue(processor.wasValidated());
    }

    private static final class MemberProcessor extends TestUtil.TestProcessor {
        private boolean _validated;

        @Override
        public boolean process(
                final Set<? extends TypeElement> annotations,
                final javax.annotation.processing.RoundEnvironment roundEnv) {
            if (!_validated && !roundEnv.processingOver()) {
                final TypeElement target = type("com.example.MemberTarget");
                final TypeElement otherBase = type("com.other.OtherBase");
                validatePassingChecks(target);
                validateFailingChecks(target, otherBase);
                validateWarningEmission(target);
                validateReturnTypeAndOverrideHelpers(target);
                validateTypeLevelChecks();
                _validated = true;
            }
            return false;
        }

        boolean wasValidated() {
            return _validated;
        }

        private void validatePassingChecks(final TypeElement target) {
            final ExecutableElement protectedMethod = method(target, "protectedMethod");
            final ExecutableElement staticMethod = method(target, "staticMethod");
            final ExecutableElement packageMethod = method(target, "packageMethod");
            final ExecutableElement allowedOverlap = method(target, "allowedOverlap");

            MemberChecks.mustBeWrappable(target, SCOPE, SCOPE, protectedMethod);
            MemberChecks.mustBeOverridable(target, SCOPE, SCOPE, protectedMethod);
            MemberChecks.mustBeSubclassCallable(target, SCOPE, SCOPE, protectedMethod);
            MemberChecks.mustBeStaticallySubclassCallable(target, SCOPE, SCOPE, staticMethod);
            MemberChecks.mustBeLifecycleHook(target, SCOPE, SCOPE, protectedMethod);
            MemberChecks.mustNotBePackageAccessInDifferentPackage(target, SCOPE, SCOPE, packageMethod);
            MemberChecks.verifyNoOverlappingAnnotations(
                    allowedOverlap,
                    List.of("com.example.One", "com.example.Two", "com.example.Three"),
                    Map.of("com.example.One", List.of("com.example.Three")));
        }

        private void validateFailingChecks(final TypeElement target, final TypeElement otherBase) {
            assertFails(
                    () -> MemberChecks.mustBeStatic(SCOPE, method(target, "publicMethod")),
                    method(target, "publicMethod"),
                    "@Scope target must be static");
            assertFails(
                    () -> MemberChecks.mustNotBeStatic(SCOPE, method(target, "staticMethod")),
                    method(target, "staticMethod"),
                    "@Scope target must not be static");
            assertFails(
                    () -> MemberChecks.mustBeAbstract(SCOPE, method(target, "protectedMethod")),
                    method(target, "protectedMethod"),
                    "@Scope target must be abstract");
            assertFails(
                    () -> MemberChecks.mustNotBeAbstract(SCOPE, method(target, "abstractMethod")),
                    method(target, "abstractMethod"),
                    "@Scope target must not be abstract");
            assertFails(
                    () -> MemberChecks.mustBeFinal(SCOPE, method(target, "protectedMethod")),
                    method(target, "protectedMethod"),
                    "@Scope target must be final");
            assertFails(
                    () -> MemberChecks.mustNotBeFinal(SCOPE, method(target, "finalMethod")),
                    method(target, "finalMethod"),
                    "@Scope target must not be final");
            assertFails(
                    () -> MemberChecks.mustNotBePublic(SCOPE, method(target, "publicMethod")),
                    method(target, "publicMethod"),
                    "@Scope target must not be public");
            assertFails(
                    () -> MemberChecks.mustBeProtected(SCOPE, method(target, "publicMethod")),
                    method(target, "publicMethod"),
                    "@Scope target must be protected");
            assertFails(
                    () -> MemberChecks.mustNotBeProtected(SCOPE, method(target, "protectedMethod")),
                    method(target, "protectedMethod"),
                    "@Scope target must not be protected");
            assertFails(
                    () -> MemberChecks.mustNotBePrivate(SCOPE, method(target, "privateMethod")),
                    method(target, "privateMethod"),
                    "@Scope target must not be private");
            assertFails(
                    () -> MemberChecks.mustNotBePackageAccessInDifferentPackage(
                            target, SCOPE, SCOPE, method(otherBase, "packageMethod")),
                    method(otherBase, "packageMethod"),
                    "@Scope target must not be package access if the method is in a different package from the type "
                            + "annotated with the @Scope annotation");
            assertFails(
                    () -> MemberChecks.mustNotHaveAnyParameters(SCOPE, method(target, "parameterMethod")),
                    method(target, "parameterMethod"),
                    "@Scope target must not have any parameters");
            assertFails(
                    () -> MemberChecks.mustNotHaveAnyTypeParameters(SCOPE, method(target, "typeParameterMethod")),
                    method(target, "typeParameterMethod"),
                    "@Scope target must not have any type parameters");
            assertFails(
                    () -> MemberChecks.mustNotReturnAnyValue(SCOPE, method(target, "valueMethod")),
                    method(target, "valueMethod"),
                    "@Scope target must not return a value");
            assertFails(
                    () -> MemberChecks.mustReturnAValue(SCOPE, method(target, "protectedMethod")),
                    method(target, "protectedMethod"),
                    "@Scope target must return a value");
            assertFails(
                    () -> MemberChecks.mustNotThrowAnyExceptions(SCOPE, method(target, "throwsMethod")),
                    method(target, "throwsMethod"),
                    "@Scope target must not throw any exceptions");
            assertFails(
                    () -> MemberChecks.verifyNoOverlappingAnnotations(
                            method(target, "overlapping"),
                            List.of("com.example.One", "com.example.Two", "com.example.Three"),
                            Collections.emptyMap()),
                    method(target, "overlapping"),
                    "Method can not be annotated with both @One and @Two");
        }

        private void validateWarningEmission(final TypeElement target) {
            final var messager = new CapturingMessager();
            final ProcessingEnvironment env = TestUtil.proxy(ProcessingEnvironment.class, (self, method, args) -> {
                if ("getMessager".equals(method.getName())) {
                    return messager;
                }
                return TestUtil.unsupported(method);
            });

            MemberChecks.shouldNotBePublic(
                    env, method(target, "publicMethod"), SCOPE, Diagnostic.Kind.WARNING, "public-method");
            assertEquals(messager.messages().size(), 1);
            assertEquals(messager.messages().get(0).kind(), Diagnostic.Kind.WARNING);
            assertEquals(
                    messager.messages().get(0).message(),
                    "@Scope target should not be public. This warning can be suppressed by annotating the element "
                            + "with @SuppressWarnings( \"public-method\" )");
            assertSame(messager.messages().get(0).element(), method(target, "publicMethod"));

            MemberChecks.shouldNotBePublic(
                    env, method(target, "publicSuppressed"), SCOPE, Diagnostic.Kind.WARNING, "public-method");
            assertEquals(messager.messages().size(), 1);
        }

        private void validateReturnTypeAndOverrideHelpers(final TypeElement target) {
            MemberChecks.mustReturnAnInstanceOf(
                    processingEnv, method(target, "valueMethod"), SCOPE, "java.lang.String");
            assertFails(
                    () -> MemberChecks.mustReturnAnInstanceOf(
                            processingEnv, method(target, "valueMethod"), SCOPE, "java.lang.Integer"),
                    method(target, "valueMethod"),
                    "@Scope target must return an instance of java.lang.Integer");
            assertFalse(
                    MemberChecks.doesMethodNotOverrideInterfaceMethod(processingEnv, target, method(target, "api")));
            assertTrue(MemberChecks.doesMethodNotOverrideInterfaceMethod(
                    processingEnv, target, method(target, "parameterMethod")));
        }

        private void validateTypeLevelChecks() {
            final TypeElement model = type("com.example.PlainClass");
            final TypeElement contract = type("com.example.Contract");
            final TypeElement mode = type("com.example.Mode");
            final TypeElement generic = type("com.example.Generic");
            final TypeElement outer = type("com.example.Outer");
            final TypeElement inner = nestedType(outer, "Inner");
            final TypeElement staticNested = nestedType(outer, "StaticNested");

            MemberChecks.mustBeClass(SCOPE, model);
            assertFails(() -> MemberChecks.mustBeClass(SCOPE, contract), contract, "@Scope target must be a class");

            MemberChecks.mustBeInterface(SCOPE, contract);
            assertFails(() -> MemberChecks.mustBeInterface(SCOPE, model), model, "@Scope target must be an interface");

            MemberChecks.mustBeClassOrInterface(SCOPE, model);
            MemberChecks.mustBeClassOrInterface(SCOPE, contract);
            assertFails(
                    () -> MemberChecks.mustBeClassOrInterface(SCOPE, mode),
                    mode,
                    "@Scope target must be a class or an interface");

            MemberChecks.mustNotHaveTypeParameters(SCOPE, model);
            assertFails(
                    () -> MemberChecks.mustNotHaveTypeParameters(SCOPE, generic),
                    generic,
                    "@Scope target must not have type parameters");

            MemberChecks.mustNotBeNonStaticNestedType(SCOPE, model);
            MemberChecks.mustNotBeNonStaticNestedType(SCOPE, staticNested);
            assertFails(
                    () -> MemberChecks.mustNotBeNonStaticNestedType(SCOPE, inner),
                    inner,
                    "@Scope target must not be a non-static nested class");
        }

        private TypeElement type(final String classname) {
            final TypeElement type = processingEnv.getElementUtils().getTypeElement(classname);
            assertNotNull(type);
            return type;
        }
    }

    private static ExecutableElement method(final TypeElement type, final String name) {
        return ElementFilter.methodsIn(type.getEnclosedElements()).stream()
                .filter(method -> name.contentEquals(method.getSimpleName()))
                .findFirst()
                .orElseThrow();
    }

    private static TypeElement nestedType(final TypeElement type, final String name) {
        return ElementFilter.typesIn(type.getEnclosedElements()).stream()
                .filter(nested -> name.contentEquals(nested.getSimpleName()))
                .findFirst()
                .orElseThrow();
    }

    private static void assertFails(final CheckedAction action, final Element element, final String message) {
        try {
            action.run();
            fail("Expected ProcessorException");
        } catch (final ProcessorException e) {
            assertEquals(e.getMessage(), message);
            assertSame(e.getElement(), element);
        }
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run();
    }

    private static final class CapturingMessager implements Messager {
        private final List<Message> _messages = new java.util.ArrayList<>();

        @Override
        public void printMessage(final Diagnostic.Kind kind, final CharSequence msg) {
            _messages.add(new Message(kind, msg.toString(), null));
        }

        @Override
        public void printMessage(final Diagnostic.Kind kind, final CharSequence msg, final Element e) {
            _messages.add(new Message(kind, msg.toString(), e));
        }

        @Override
        public void printMessage(
                final Diagnostic.Kind kind, final CharSequence msg, final Element e, final AnnotationMirror a) {
            _messages.add(new Message(kind, msg.toString(), e));
        }

        @Override
        public void printMessage(
                final Diagnostic.Kind kind,
                final CharSequence msg,
                final Element e,
                final AnnotationMirror a,
                final AnnotationValue v) {
            _messages.add(new Message(kind, msg.toString(), e));
        }

        List<Message> messages() {
            return _messages;
        }
    }

    private static final class Message {
        private final Diagnostic.Kind _kind;

        private final String _message;

        @Nullable
        private final Element _element;

        Message(final Diagnostic.Kind kind, final String message, @Nullable final Element element) {
            _kind = kind;
            _message = message;
            _element = element;
        }

        Diagnostic.Kind kind() {
            return _kind;
        }

        String message() {
            return _message;
        }

        @Nullable
        Element element() {
            return _element;
        }
    }
}
