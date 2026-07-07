package org.realityforge.proton;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import org.testng.annotations.Test;

public final class ElementsUtilTest {
    @Test
    public void elementHelpersTraverseTypeHierarchiesAndElementMetadata() throws Exception {
        final var processor = new ElementProcessor();

        TestUtil.compile(
                List.of(
                        TestUtil.source("com.example.ElementsTarget", """
                            package com.example;
                            import java.util.List;
                            @interface AltSuppress {
                              String[] value();
                            }
                            interface RootInterface {
                              void root();
                            }
                            interface ChildInterface extends RootInterface {
                              void child();
                            }
                            class Base {
                              int baseField;
                              String shadowedField;
                              protected Number convert(String value) { return 1; }
                              public void baseOnly() {}
                            }
                            @SuppressWarnings("class-warning")
                            public class ElementsTarget extends Base implements ChildInterface {
                              String shadowedField;
                              List<String> names;
                              public ElementsTarget() {}
                              @Override
                              public Integer convert(String value) { return 1; }
                              @Override
                              public void root() {}
                              @Override
                              public void child() {}
                              @SuppressWarnings("method-warning")
                              @AltSuppress("alt-warning")
                              public void own() {}
                              void packageMethod() {}
                              private void privateMethod() {}
                              public class Inner {}
                              public static class StaticInner {}
                              public static class PublicNested {}
                              static class PackageStaticNested {}
                              private static class PrivateNested {}
                            }
                            @Deprecated
                            class DeprecatedOuter {
                              static class Nested {}
                            }
                            class PackagePrivate {
                              public static class PublicNested {}
                            }
                            class SamePackage {}
                            class PackageCtor { PackageCtor() {} }
                            class PrivateCtor { private PrivateCtor() {} }
                            class DefaultCtor {}
                            """),
                        TestUtil.source("com.other.OtherType", """
                            package com.other;
                            public final class OtherType {}
                            """)),
                processor);

        assertTrue(processor.wasValidated());
    }

    private static final class ElementProcessor extends TestUtil.TestProcessor {
        private boolean _validated;

        @Override
        public boolean process(
                final Set<? extends TypeElement> annotations,
                final javax.annotation.processing.RoundEnvironment roundEnv) {
            if (!_validated && !roundEnv.processingOver()) {
                final TypeElement target = type("com.example.ElementsTarget");
                final TypeElement base = type("com.example.Base");
                final TypeElement samePackage = type("com.example.SamePackage");
                final TypeElement otherType = type("com.other.OtherType");
                validateHierarchyHelpers(target, base);
                validateElementMetadataHelpers(target, samePackage, otherType);
                validateWarningSuppression(target);
                _validated = true;
            }
            return false;
        }

        boolean wasValidated() {
            return _validated;
        }

        private void validateHierarchyHelpers(final TypeElement target, final TypeElement base) {
            assertEquals(simpleNames(ElementsUtil.getSuperTypes(target)), List.of("Base", "Object"));
            assertEquals(simpleNames(ElementsUtil.getInterfaces(target)), List.of("ChildInterface", "RootInterface"));

            final List<VariableElement> fields = ElementsUtil.getFields(target);
            assertEquals(
                    fields.stream().map(f -> f.getSimpleName().toString()).collect(Collectors.toList()),
                    List.of("baseField", "shadowedField", "names"));
            assertSame(fields.get(1).getEnclosingElement(), target);

            final Map<String, List<ExecutableElement>> methodsByName =
                    ElementsUtil.getMethods(target, processingEnv.getElementUtils(), processingEnv.getTypeUtils())
                            .stream()
                            .collect(Collectors.groupingBy(
                                    method -> method.getSimpleName().toString()));
            assertTrue(methodsByName.keySet().containsAll(List.of("baseOnly", "convert", "root", "child", "own")));
            final List<ExecutableElement> convertMethods = Objects.requireNonNull(methodsByName.get("convert"));
            assertEquals(convertMethods.size(), 1);
            assertSame(convertMethods.get(0).getEnclosingElement(), target);
            assertEquals(convertMethods.get(0).getReturnType().toString(), "java.lang.Integer");

            assertEquals(ElementsUtil.getConstructors(target).size(), 1);
            assertTrue(ElementsUtil.doesMethodOverrideInterfaceMethod(
                    processingEnv.getTypeUtils(), target, method(target, "root")));
            assertFalse(ElementsUtil.doesMethodOverrideInterfaceMethod(
                    processingEnv.getTypeUtils(), target, method(target, "convert")));
            assertEquals(ElementsUtil.toRawType(field(target, "names").asType()).toString(), "java.util.List");
            assertSame(
                    ElementsUtil.getOverriddenMethod(processingEnv, target, method(target, "convert")),
                    method(base, "convert"));
            assertNull(ElementsUtil.getOverriddenMethod(processingEnv, target, method(target, "own")));
        }

        private void validateElementMetadataHelpers(
                final TypeElement target, final TypeElement samePackage, final TypeElement otherType) {
            final TypeElement inner = nestedType(target, "Inner");
            final TypeElement staticInner = nestedType(target, "StaticInner");
            final TypeElement publicNested = nestedType(target, "PublicNested");
            final TypeElement packageStaticNested = nestedType(target, "PackageStaticNested");
            final TypeElement privateNested = nestedType(target, "PrivateNested");
            final TypeElement deprecatedOuter = type("com.example.DeprecatedOuter");
            final TypeElement deprecatedNested = nestedType(deprecatedOuter, "Nested");
            final TypeElement packagePrivate = type("com.example.PackagePrivate");
            final ExecutableElement own = method(target, "own");
            final ExecutableElement packageMethod = method(target, "packageMethod");
            final ExecutableElement privateMethod = method(target, "privateMethod");

            assertSame(ElementsUtil.getTopLevelElement(inner), target);
            assertTrue(ElementsUtil.isNonStaticNestedClass(inner));
            assertFalse(ElementsUtil.isNonStaticNestedClass(staticInner));
            assertFalse(ElementsUtil.isNonStaticNestedClass(target));
            assertSame(ElementsUtil.getOwningType(target), target);
            assertSame(ElementsUtil.getOwningType(own), target);
            assertTrue(ElementsUtil.isNonStaticNestedType(inner));
            assertFalse(ElementsUtil.isNonStaticNestedType(staticInner));
            assertFalse(ElementsUtil.isNonStaticNestedType(target));

            assertTrue(ElementsUtil.hasDeprecatedAnnotation(deprecatedOuter));
            assertFalse(ElementsUtil.hasDeprecatedAnnotation(deprecatedNested));
            assertTrue(ElementsUtil.isDeprecated(deprecatedNested));

            assertTrue(ElementsUtil.isEffectivelyPublic(target));
            assertTrue(ElementsUtil.isEffectivelyPublic(publicNested));
            assertFalse(ElementsUtil.isEffectivelyPublic(packagePrivate));
            assertFalse(ElementsUtil.isEffectivelyPublic(nestedType(packagePrivate, "PublicNested")));

            assertEquals(
                    ElementsUtil.getPackageElement(inner).getQualifiedName().toString(), "com.example");
            assertTrue(ElementsUtil.areTypesInSamePackage(target, samePackage));
            assertFalse(ElementsUtil.areTypesInSamePackage(target, otherType));
            assertTrue(ElementsUtil.areTypesInDifferentPackage(target, otherType));

            assertTrue(ElementsUtil.isPackageAccess(packageMethod));
            assertFalse(ElementsUtil.isPackageAccess(own));
            assertFalse(ElementsUtil.isPackageAccess(privateMethod));
            assertTrue(ElementsUtil.isElementAccessibleFrom(target, packageMethod));
            assertFalse(ElementsUtil.isElementAccessibleFrom(otherType, packageMethod));
            assertTrue(ElementsUtil.isElementAccessibleFrom(otherType, own));
            assertFalse(ElementsUtil.isElementAccessibleFrom(target, privateMethod));

            assertTrue(ElementsUtil.isTypeAccessibleFrom(otherType, publicNested));
            assertTrue(ElementsUtil.isTypeAccessibleFrom(target, packageStaticNested));
            assertFalse(ElementsUtil.isTypeAccessibleFrom(otherType, packageStaticNested));
            assertFalse(ElementsUtil.isTypeAccessibleFrom(target, privateNested));
            assertSame(ElementsUtil.asTypeElement(processingEnv, target.asType()), target);
            final Element parameter = processingEnv
                    .getElementUtils()
                    .getTypeElement(List.class.getName())
                    .getTypeParameters()
                    .get(0);
            assertNull(ElementsUtil.asTypeElement(processingEnv, parameter.asType()));
            assertTrue(ElementsUtil.isAssignableTo(processingEnv, target.asType(), Object.class.getName()));
            assertFalse(ElementsUtil.isAssignableTo(processingEnv, otherType.asType(), "com.example.ElementsTarget"));
            assertFalse(ElementsUtil.isAssignableTo(processingEnv, otherType.asType(), "com.example.Missing"));
            assertTrue(ElementsUtil.hasAccessibleNoArgConstructor(otherType, target));
            assertTrue(ElementsUtil.hasAccessibleNoArgConstructor(target, type("com.example.PackageCtor")));
            assertFalse(ElementsUtil.hasAccessibleNoArgConstructor(otherType, type("com.example.PackageCtor")));
            assertFalse(ElementsUtil.hasAccessibleNoArgConstructor(target, type("com.example.PrivateCtor")));
            assertTrue(ElementsUtil.hasAccessibleNoArgConstructor(target, type("com.example.DefaultCtor")));
        }

        private static void validateWarningSuppression(final TypeElement target) {
            final ExecutableElement own = method(target, "own");

            assertTrue(ElementsUtil.isWarningSuppressed(own, "method-warning"));
            assertTrue(ElementsUtil.isWarningSuppressed(own, "class-warning"));
            assertTrue(ElementsUtil.isWarningSuppressed(own, "alt-warning", "com.example.AltSuppress"));
            assertFalse(ElementsUtil.isWarningSuppressed(own, "missing-warning"));
            assertFalse(ElementsUtil.isWarningNotSuppressed(own, "class-warning"));
            assertTrue(ElementsUtil.isWarningNotSuppressed(own, "missing-warning"));
        }

        private TypeElement type(final String classname) {
            final TypeElement type = processingEnv.getElementUtils().getTypeElement(classname);
            assertNotNull(type);
            return type;
        }

        private static TypeElement nestedType(final TypeElement type, final String name) {
            return ElementFilter.typesIn(type.getEnclosedElements()).stream()
                    .filter(nested -> name.contentEquals(nested.getSimpleName()))
                    .findFirst()
                    .orElseThrow();
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

        private static List<String> simpleNames(final List<TypeElement> types) {
            return types.stream().map(type -> type.getSimpleName().toString()).collect(Collectors.toList());
        }
    }
}
