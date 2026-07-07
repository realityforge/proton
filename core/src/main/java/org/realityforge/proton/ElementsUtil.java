package org.realityforge.proton;

import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.jspecify.annotations.Nullable;

public final class ElementsUtil {
    private ElementsUtil() {}

    public static boolean isWarningNotSuppressed(final Element element, final String warning) {
        return !isWarningSuppressed(element, warning);
    }

    public static boolean isWarningNotSuppressed(
            final Element element, final String warning, @Nullable final String alternativeSuppressWarnings) {
        return !isWarningSuppressed(element, warning, alternativeSuppressWarnings);
    }

    public static boolean isWarningSuppressed(final Element element, final String warning) {
        return isWarningSuppressed(element, warning, null);
    }

    public static boolean isWarningSuppressed(
            final Element element, final String warning, @Nullable final String alternativeSuppressWarnings) {
        return SuppressWarningsUtil.isSuppressed(element, warning, alternativeSuppressWarnings);
    }

    public static List<TypeElement> getSuperTypes(final TypeElement element) {
        final List<TypeElement> superTypes = new ArrayList<>();
        enumerateSuperTypes(element, superTypes);
        return superTypes;
    }

    private static void enumerateSuperTypes(final TypeElement element, final List<TypeElement> superTypes) {
        final TypeMirror superclass = element.getSuperclass();
        if (TypeKind.NONE != superclass.getKind()) {
            final var superclassElement = (TypeElement) ((DeclaredType) superclass).asElement();
            superTypes.add(superclassElement);
            enumerateSuperTypes(superclassElement, superTypes);
        }
        for (final TypeMirror interfaceType : element.getInterfaces()) {
            final var interfaceElement = (TypeElement) ((DeclaredType) interfaceType).asElement();
            enumerateSuperTypes(interfaceElement, superTypes);
        }
    }

    public static List<TypeElement> getInterfaces(final TypeElement element) {
        final List<TypeElement> superTypes = new ArrayList<>();
        enumerateInterfaces(element, superTypes);
        return superTypes;
    }

    private static void enumerateInterfaces(final TypeElement element, final List<TypeElement> superTypes) {
        final TypeMirror superclass = element.getSuperclass();
        if (TypeKind.NONE != superclass.getKind()) {
            final var superclassElement = (TypeElement) ((DeclaredType) superclass).asElement();
            enumerateInterfaces(superclassElement, superTypes);
        }
        for (final TypeMirror interfaceType : element.getInterfaces()) {
            final var interfaceElement = (TypeElement) ((DeclaredType) interfaceType).asElement();
            superTypes.add(interfaceElement);
            enumerateInterfaces(interfaceElement, superTypes);
        }
    }

    public static List<VariableElement> getFields(final TypeElement element) {
        final Map<String, VariableElement> methodMap = new LinkedHashMap<>();
        enumerateFields(element, methodMap);
        return new ArrayList<>(methodMap.values());
    }

    private static void enumerateFields(final TypeElement element, final Map<String, VariableElement> fields) {
        final TypeMirror superclass = element.getSuperclass();
        if (TypeKind.NONE != superclass.getKind()) {
            enumerateFields((TypeElement) ((DeclaredType) superclass).asElement(), fields);
        }
        for (final Element member : element.getEnclosedElements()) {
            if (ElementKind.FIELD == member.getKind()) {
                fields.put(member.getSimpleName().toString(), (VariableElement) member);
            }
        }
    }

    public static List<ExecutableElement> getMethods(
            final TypeElement element, final Elements elementUtils, final Types typeUtils) {
        return getMethods(element, elementUtils, typeUtils, false);
    }

    public static List<ExecutableElement> getMethods(
            final TypeElement element,
            final Elements elementUtils,
            final Types typeUtils,
            final boolean collectInterfaceMethodsAtEnd) {
        final Map<String, ArrayList<ExecutableElement>> methodMap = new LinkedHashMap<>();
        enumerateMethods(element, elementUtils, typeUtils, element, methodMap, collectInterfaceMethodsAtEnd);
        if (collectInterfaceMethodsAtEnd) {
            // Collect the interfaces at the end. Usually this is done
            enumerateMethodsFromInterfaces(element, elementUtils, typeUtils, element, methodMap);
        }
        return methodMap.values().stream().flatMap(Collection::stream).toList();
    }

    private static void enumerateMethods(
            final TypeElement scope,
            final Elements elementUtils,
            final Types typeUtils,
            final TypeElement element,
            final Map<String, ArrayList<ExecutableElement>> methods,
            final boolean collectInterfaceMethodsAtEnd) {
        final TypeMirror superclass = element.getSuperclass();
        if (TypeKind.NONE != superclass.getKind()) {
            final var superclassElement = (TypeElement) ((DeclaredType) superclass).asElement();
            enumerateMethods(scope, elementUtils, typeUtils, superclassElement, methods, collectInterfaceMethodsAtEnd);
        }
        if (!collectInterfaceMethodsAtEnd) {
            for (final TypeMirror interfaceType : element.getInterfaces()) {
                final var interfaceElement = (TypeElement) ((DeclaredType) interfaceType).asElement();
                enumerateMethodsFromInterfaces(scope, elementUtils, typeUtils, interfaceElement, methods);
            }
        }
        for (final Element member : element.getEnclosedElements()) {
            if (ElementKind.METHOD == member.getKind()) {
                final var method = (ExecutableElement) member;
                processMethod(elementUtils, typeUtils, scope, methods, method);
            }
        }
    }

    private static void enumerateMethodsFromInterfaces(
            final TypeElement scope,
            final Elements elementUtils,
            final Types typeUtils,
            final TypeElement element,
            final Map<String, ArrayList<ExecutableElement>> methods) {
        final TypeMirror superclass = element.getSuperclass();
        if (TypeKind.NONE != superclass.getKind()) {
            final var superclassElement = (TypeElement) ((DeclaredType) superclass).asElement();
            enumerateMethodsFromInterfaces(scope, elementUtils, typeUtils, superclassElement, methods);
        }
        for (final TypeMirror interfaceType : element.getInterfaces()) {
            final var interfaceElement = (TypeElement) ((DeclaredType) interfaceType).asElement();
            enumerateMethodsFromInterfaces(scope, elementUtils, typeUtils, interfaceElement, methods);
        }
        // Only collect methods from interfaces
        if (ElementKind.INTERFACE == element.getKind()) {
            for (final Element member : element.getEnclosedElements()) {
                if (ElementKind.METHOD == member.getKind()) {
                    final var method = (ExecutableElement) member;
                    processMethod(elementUtils, typeUtils, scope, methods, method);
                }
            }
        }
    }

    private static void processMethod(
            final Elements elementUtils,
            final Types typeUtils,
            final TypeElement typeElement,
            final Map<String, ArrayList<ExecutableElement>> methods,
            final ExecutableElement method) {
        final var methodType = (ExecutableType) typeUtils.asMemberOf((DeclaredType) typeElement.asType(), method);

        final String key = method.getSimpleName().toString();
        final ArrayList<ExecutableElement> elements = methods.computeIfAbsent(key, ignored -> new ArrayList<>());
        boolean found = false;
        final int size = elements.size();
        for (int i = 0; i < size; i++) {
            final ExecutableElement executableElement = elements.get(i);
            if (method.equals(executableElement)) {
                found = true;
                break;
            } else if (isSubsignature(typeUtils, typeElement, methodType, executableElement)) {
                if (!isAbstractInterfaceMethod(method) || isAbstractInterfaceMethod(executableElement)) {
                    elements.set(i, method);
                }
                found = true;
                break;
            } else if (elementUtils.overrides(method, executableElement, typeElement)) {
                elements.set(i, method);
                found = true;
                break;
            }
        }
        if (!found) {
            elements.add(method);
        }
    }

    private static boolean isAbstractInterfaceMethod(final ExecutableElement method) {
        return method.getModifiers().contains(Modifier.ABSTRACT)
                && ElementKind.INTERFACE == requireEnclosingElement(method).getKind();
    }

    private static boolean isSubsignature(
            final Types typeUtils,
            final TypeElement typeElement,
            final ExecutableType methodType,
            final ExecutableElement candidate) {
        final var candidateType = (ExecutableType) typeUtils.asMemberOf((DeclaredType) typeElement.asType(), candidate);
        final boolean isEqual = methodType.equals(candidateType);
        final boolean isSubsignature = typeUtils.isSubsignature(methodType, candidateType);
        return isSubsignature || isEqual;
    }

    public static List<ExecutableElement> getConstructors(final TypeElement element) {
        return element.getEnclosedElements().stream()
                .filter(m -> ElementKind.CONSTRUCTOR == m.getKind())
                .map(m -> (ExecutableElement) m)
                .collect(Collectors.toList());
    }

    public static boolean doesMethodOverrideInterfaceMethod(
            final Types typeUtils, final TypeElement typeElement, final ExecutableElement method) {
        return getInterfaces(typeElement).stream()
                .flatMap(i -> i.getEnclosedElements().stream())
                .filter(e -> ElementKind.METHOD == e.getKind())
                .map(e -> (ExecutableElement) e)
                .anyMatch(e -> isSubsignature(
                        typeUtils,
                        typeElement,
                        (ExecutableType) typeUtils.asMemberOf((DeclaredType) typeElement.asType(), e),
                        method));
    }

    public static TypeName toRawType(final TypeMirror type) {
        final TypeName typeName = TypeName.get(type);
        if (typeName instanceof final ParameterizedTypeName parameterizedTypeName) {
            return parameterizedTypeName.rawType();
        } else {
            return typeName;
        }
    }

    /**
     * Return the outer enclosing element.
     * This is either the top-level class, interface, enum, etc within a package.
     * This helps identify the top level compilation units.
     */
    public static Element getTopLevelElement(final Element element) {
        Element result = element;
        Element enclosing = requireEnclosingElement(result);
        while (ElementKind.PACKAGE != enclosing.getKind()) {
            result = enclosing;
            enclosing = requireEnclosingElement(result);
        }
        return result;
    }

    public static boolean isNonStaticNestedClass(final TypeElement element) {
        return isNonStaticNestedType(element);
    }

    public static boolean isNonStaticNestedType(final TypeElement element) {
        return NestingKind.TOP_LEVEL != element.getNestingKind()
                && !element.getModifiers().contains(Modifier.STATIC);
    }

    public static boolean isPackageAccess(final Element element) {
        final Set<Modifier> modifiers = element.getModifiers();
        return !modifiers.contains(Modifier.PRIVATE)
                && !modifiers.contains(Modifier.PROTECTED)
                && !modifiers.contains(Modifier.PUBLIC);
    }

    public static TypeElement getOwningType(final Element element) {
        Element current = element;
        while (!(current instanceof TypeElement)) {
            current = requireEnclosingElement(current);
        }
        return (TypeElement) current;
    }

    public static boolean isElementAccessibleFrom(final TypeElement scope, final Element element) {
        final Set<Modifier> modifiers = element.getModifiers();
        return !modifiers.contains(Modifier.PRIVATE)
                && (modifiers.contains(Modifier.PUBLIC) || areTypesInSamePackage(getOwningType(element), scope));
    }

    public static boolean isTypeAccessibleFrom(final TypeElement scope, final TypeElement element) {
        TypeElement current = element;
        while (true) {
            if (!isElementAccessibleFrom(scope, current)) {
                return false;
            }
            final Element enclosing = requireEnclosingElement(current);
            if (enclosing instanceof TypeElement) {
                current = (TypeElement) enclosing;
            } else {
                return true;
            }
        }
    }

    public static boolean hasAccessibleNoArgConstructor(final TypeElement scope, final TypeElement element) {
        final List<ExecutableElement> constructors = getConstructors(element);
        return constructors.isEmpty()
                || constructors.stream()
                        .anyMatch(c -> c.getParameters().isEmpty() && isElementAccessibleFrom(scope, c));
    }

    @Nullable
    public static TypeElement asTypeElement(final ProcessingEnvironment processingEnv, final TypeMirror type) {
        final Element element = processingEnv.getTypeUtils().asElement(type);
        return element instanceof TypeElement ? (TypeElement) element : null;
    }

    public static boolean isAssignableTo(
            final ProcessingEnvironment processingEnv, final TypeMirror type, final TypeElement targetType) {
        return processingEnv.getTypeUtils().isAssignable(type, targetType.asType());
    }

    public static boolean isAssignableTo(
            final ProcessingEnvironment processingEnv, final TypeMirror type, final String targetType) {
        final TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(targetType);
        return null != typeElement && isAssignableTo(processingEnv, type, typeElement);
    }

    public static boolean isAssignableTo(
            final ProcessingEnvironment processingEnv, final Element element, final String targetType) {
        return isAssignableTo(processingEnv, element.asType(), targetType);
    }

    public static boolean hasDeprecatedAnnotation(final Element element) {
        return element.getAnnotationMirrors().stream()
                .anyMatch(a -> a.getAnnotationType().toString().equals(Deprecated.class.getName()));
    }

    public static boolean isDeprecated(final Element element) {
        if (hasDeprecatedAnnotation(element)) {
            return true;
        } else if (element.getKind().isClass() || element.getKind().isInterface()) {
            final Element enclosing = requireEnclosingElement(element);
            return ElementKind.PACKAGE != enclosing.getKind() && isDeprecated(enclosing);
        } else {
            return false;
        }
    }

    public static boolean isEffectivelyPublic(final TypeElement element) {
        if (!element.getModifiers().contains(Modifier.PUBLIC)) {
            return false;
        } else {
            final Element enclosing = requireEnclosingElement(element);
            return ElementKind.PACKAGE == enclosing.getKind() || isEffectivelyPublic((TypeElement) enclosing);
        }
    }

    public static PackageElement getPackageElement(final Element outerElement) {
        Element element = outerElement;
        while (ElementKind.PACKAGE != element.getKind()) {
            element = requireEnclosingElement(element);
        }
        return (PackageElement) element;
    }

    private static Element requireEnclosingElement(final Element element) {
        return Objects.requireNonNull(element.getEnclosingElement());
    }

    public static boolean areTypesInDifferentPackage(final TypeElement typeElement1, final TypeElement typeElement2) {
        return !areTypesInSamePackage(typeElement1, typeElement2);
    }

    public static boolean areTypesInSamePackage(final TypeElement typeElement1, final TypeElement typeElement2) {
        final PackageElement packageElement1 = getPackageElement(typeElement1);
        final PackageElement packageElement2 = getPackageElement(typeElement2);
        return Objects.equals(packageElement1.getQualifiedName(), packageElement2.getQualifiedName());
    }

    /**
     * Return the method that the specified method is overriding if any.
     *
     * @param processingEnv the processing environment.
     * @param typeElement   the enclosing type.
     * @param method        the method.
     * @return the method that the specified method overrides, else null.
     */
    @Nullable
    public static ExecutableElement getOverriddenMethod(
            final ProcessingEnvironment processingEnv, final TypeElement typeElement, final ExecutableElement method) {
        final TypeMirror superclass = typeElement.getSuperclass();
        if (TypeKind.NONE == superclass.getKind()) {
            return null;
        } else {
            final var parent = (TypeElement)
                    Objects.requireNonNull(processingEnv.getTypeUtils().asElement(superclass));
            final List<? extends Element> enclosedElements = parent.getEnclosedElements();
            for (final Element enclosedElement : enclosedElements) {
                if (ElementKind.METHOD == enclosedElement.getKind()
                        && processingEnv
                                .getElementUtils()
                                .overrides(method, (ExecutableElement) enclosedElement, typeElement)) {
                    return (ExecutableElement) enclosedElement;
                }
            }
            return getOverriddenMethod(processingEnv, parent, method);
        }
    }
}
