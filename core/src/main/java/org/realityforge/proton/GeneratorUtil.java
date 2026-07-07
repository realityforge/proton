package org.realityforge.proton;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.processing.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

@SuppressWarnings({
    "SameParameterValue",
    "unused",
    "WeakerAccess",
    "RedundantSuppression",
    "BooleanMethodIsAlwaysInverted"
})
public final class GeneratorUtil {
    public static final ClassName NONNULL_CLASSNAME = ClassName.get("javax.annotation", "Nonnull");
    public static final ClassName NULLABLE_CLASSNAME = ClassName.get("javax.annotation", "Nullable");

    public static final List<String> ANNOTATION_WHITELIST = Collections.unmodifiableList(Arrays.asList(
            AnnotationsUtil.NONNULL_CLASSNAME, AnnotationsUtil.NULLABLE_CLASSNAME, Deprecated.class.getName()));

    private GeneratorUtil() {}

    public static ClassName getGeneratedClassName(
            final ClassName className, final String prefix, final String postfix) {
        return ClassName.get(className.packageName(), getGeneratedSimpleClassName(className, prefix, postfix));
    }

    public static String getGeneratedSimpleClassName(
            final ClassName className, final String prefix, final String postfix) {
        return getNestedClassPrefix(className) + prefix + className.simpleName() + postfix;
    }

    private static String getNestedClassPrefix(final ClassName className) {
        final var name = new StringBuilder();
        final List<String> simpleNames = className.simpleNames();
        if (simpleNames.size() > 1) {
            for (final String simpleName : simpleNames.subList(0, simpleNames.size() - 1)) {
                name.append(simpleName);
                name.append("_");
            }
        }
        return name.toString();
    }

    public static ClassName getGeneratedClassName(
            final TypeElement element, final String prefix, final String postfix) {
        return ClassName.get(getQualifiedPackageName(element), getGeneratedSimpleClassName(element, prefix, postfix));
    }

    public static String getQualifiedPackageName(final TypeElement element) {
        return ElementsUtil.getPackageElement(element).getQualifiedName().toString();
    }

    public static String getGeneratedSimpleClassName(
            final TypeElement element, final String prefix, final String postfix) {
        return getNestedClassPrefix(element) + prefix + element.getSimpleName() + postfix;
    }

    private static String getNestedClassPrefix(final TypeElement element) {
        final var name = new StringBuilder();
        TypeElement t = element;
        while (NestingKind.TOP_LEVEL != t.getNestingKind()) {
            t = (TypeElement) Objects.requireNonNull(t.getEnclosingElement());
            name.insert(0, t.getSimpleName() + "_");
        }
        return name.toString();
    }

    public static List<TypeVariableName> getTypeArgumentsAsNames(final DeclaredType declaredType) {
        final List<TypeVariableName> variables = new ArrayList<>();
        for (final TypeMirror argument : declaredType.getTypeArguments()) {
            variables.add(TypeVariableName.get((TypeVariable) argument));
        }
        return variables;
    }

    public static void copyAccessModifiers(final TypeElement element, final TypeSpec.Builder builder) {
        if (element.getModifiers().contains(Modifier.PUBLIC)) {
            builder.addModifiers(Modifier.PUBLIC);
        }
    }

    public static void copyAccessModifiers(final TypeElement element, final MethodSpec.Builder builder) {
        if (element.getModifiers().contains(Modifier.PUBLIC)) {
            builder.addModifiers(Modifier.PUBLIC);
        }
    }

    public static void copyAccessModifiers(final ExecutableElement element, final MethodSpec.Builder builder) {
        if (element.getModifiers().contains(Modifier.PUBLIC)) {
            builder.addModifiers(Modifier.PUBLIC);
        } else if (element.getModifiers().contains(Modifier.PROTECTED)) {
            builder.addModifiers(Modifier.PROTECTED);
        }
    }

    public static void copyExceptions(final ExecutableType method, final MethodSpec.Builder builder) {
        for (final TypeMirror thrownType : method.getThrownTypes()) {
            builder.addException(TypeName.get(thrownType));
        }
    }

    public static void copyTypeParameters(final ExecutableType action, final MethodSpec.Builder builder) {
        for (final TypeVariable typeParameter : action.getTypeVariables()) {
            builder.addTypeVariable(TypeVariableName.get(typeParameter));
        }
    }

    public static void copyTypeParameters(final TypeElement element, final MethodSpec.Builder builder) {
        for (final TypeParameterElement typeParameter : element.getTypeParameters()) {
            builder.addTypeVariable(TypeVariableName.get(typeParameter));
        }
    }

    public static void copyTypeParameters(final TypeElement element, final TypeSpec.Builder builder) {
        for (final TypeParameterElement typeParameter : element.getTypeParameters()) {
            builder.addTypeVariable(TypeVariableName.get(typeParameter));
        }
    }

    public static void copyWhitelistedAnnotations(final AnnotatedConstruct element, final TypeSpec.Builder builder) {
        copyWhitelistedAnnotations(element, builder, ANNOTATION_WHITELIST);
    }

    public static void copyWhitelistedAnnotations(
            final AnnotatedConstruct element, final TypeSpec.Builder builder, final List<String> whitelist) {
        for (final AnnotationMirror annotation : element.getAnnotationMirrors()) {
            if (whitelist.contains(annotation.getAnnotationType().toString())) {
                builder.addAnnotation(AnnotationSpec.get(annotation));
            }
        }
    }

    public static void copyWhitelistedAnnotations(final AnnotatedConstruct element, final MethodSpec.Builder builder) {
        copyWhitelistedAnnotations(element, builder, ANNOTATION_WHITELIST);
    }

    public static void copyWhitelistedAnnotations(
            final AnnotatedConstruct element, final MethodSpec.Builder builder, final List<String> whitelist) {
        for (final AnnotationMirror annotation : element.getAnnotationMirrors()) {
            if (whitelist.contains(annotation.getAnnotationType().toString())) {
                builder.addAnnotation(AnnotationSpec.get(annotation));
            }
        }
    }

    public static void copyWhitelistedAnnotations(
            final AnnotatedConstruct element, final ParameterSpec.Builder builder) {
        copyWhitelistedAnnotations(element, builder, ANNOTATION_WHITELIST);
    }

    public static void copyWhitelistedAnnotations(
            final AnnotatedConstruct element, final ParameterSpec.Builder builder, final List<String> whitelist) {
        for (final AnnotationMirror annotation : element.getAnnotationMirrors()) {
            if (whitelist.contains(annotation.getAnnotationType().toString())) {
                builder.addAnnotation(AnnotationSpec.get(annotation));
            }
        }
    }

    public static void copyWhitelistedAnnotations(final AnnotatedConstruct element, final FieldSpec.Builder builder) {
        copyWhitelistedAnnotations(element, builder, ANNOTATION_WHITELIST);
    }

    public static void copyWhitelistedAnnotations(
            final AnnotatedConstruct element, final FieldSpec.Builder builder, final List<String> whitelist) {
        for (final AnnotationMirror annotation : element.getAnnotationMirrors()) {
            if (whitelist.contains(annotation.getAnnotationType().toString())) {
                builder.addAnnotation(AnnotationSpec.get(annotation));
            }
        }
    }

    public static void addOriginatingTypes(final TypeElement element, final TypeSpec.Builder builder) {
        builder.addOriginatingElement(element);
        ElementsUtil.getSuperTypes(element).forEach(builder::addOriginatingElement);
    }

    public static void addGeneratedAnnotation(
            final ProcessingEnvironment processingEnv, final TypeSpec.Builder builder, final String classname) {
        builder.addAnnotation(AnnotationSpec.builder(ClassName.get(Generated.class))
                .addMember("value", "$S", classname)
                .build());
    }

    public static MethodSpec.Builder overrideMethod(
            final ProcessingEnvironment processingEnv,
            final TypeElement typeElement,
            final ExecutableElement executableElement) {
        return overrideMethod(processingEnv, typeElement, executableElement, Collections.emptyList(), true);
    }

    public static MethodSpec.Builder overrideMethod(
            final ProcessingEnvironment processingEnv,
            final TypeElement typeElement,
            final ExecutableElement executableElement,
            final Collection<String> additionalSuppressions,
            final boolean copyNullabilityAnnotations) {
        final var declaredType = (DeclaredType) typeElement.asType();
        final var executableType =
                (ExecutableType) processingEnv.getTypeUtils().asMemberOf(declaredType, executableElement);

        final MethodSpec.Builder method =
                MethodSpec.methodBuilder(executableElement.getSimpleName().toString());
        method.addAnnotation(Override.class);

        SuppressWarningsUtil.addSuppressWarningsIfRequired(
                processingEnv, method, additionalSuppressions, Collections.singletonList(executableType));
        copyAccessModifiers(executableElement, method);
        copyTypeParameters(executableType, method);
        if (copyNullabilityAnnotations) {
            copyWhitelistedAnnotations(executableElement, method);
        } else {
            if (AnnotationsUtil.hasAnnotationOfType(executableElement, Deprecated.class.getName())) {
                method.addAnnotation(Deprecated.class);
            }
        }

        method.varargs(executableElement.isVarArgs());

        // Copy all the parameters across
        copyParameters(executableElement, executableType, method);

        copyExceptions(executableType, method);

        // Copy return type
        method.returns(TypeName.get(executableType.getReturnType()));
        return method;
    }

    public static void copyParameters(
            final ExecutableElement executableElement,
            final ExecutableType executableType,
            final MethodSpec.Builder method) {
        int paramIndex = 0;
        for (final TypeMirror parameterType : executableType.getParameterTypes()) {
            final TypeName typeName = TypeName.get(parameterType);
            final VariableElement variableElement =
                    executableElement.getParameters().get(paramIndex);
            final String name = variableElement.getSimpleName().toString();
            final ParameterSpec.Builder parameter = ParameterSpec.builder(typeName, name, Modifier.FINAL);
            copyWhitelistedAnnotations(variableElement, parameter);
            method.addParameter(parameter.build());
            paramIndex++;
        }
    }

    public static MethodSpec.Builder refMethod(
            final ProcessingEnvironment processingEnv,
            final TypeElement typeElement,
            final ExecutableElement executableElement) {
        return refMethod(processingEnv, typeElement, executableElement, Collections.emptyList());
    }

    public static MethodSpec.Builder refMethod(
            final ProcessingEnvironment processingEnv,
            final TypeElement typeElement,
            final ExecutableElement executableElement,
            final Collection<String> additionalSuppressions) {
        final MethodSpec.Builder method =
                overrideMethod(processingEnv, typeElement, executableElement, additionalSuppressions, false);
        if (!executableElement.getReturnType().getKind().isPrimitive()) {
            method.addAnnotation(NONNULL_CLASSNAME);
        }
        return method;
    }
}
