package org.realityforge.proton;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"SameParameterValue", "WeakerAccess", "unused", "RedundantSuppression"})
public final class AnnotationsUtil {
    public static final String NULLABLE_CLASSNAME = "javax.annotation.Nullable";

    public static final String NONNULL_CLASSNAME = "javax.annotation.Nonnull";

    private AnnotationsUtil() {}

    @SuppressWarnings("unchecked")
    public static List<AnnotationMirror> getRepeatingAnnotations(
            final Element typeElement, final String containerClassName, final String annotationClassName) {
        final AnnotationValue annotationValue = findAnnotationValue(typeElement, containerClassName, "value");
        if (null != annotationValue) {
            return ((List<AnnotationValue>) annotationValue.getValue())
                    .stream().map(v -> (AnnotationMirror) v.getValue()).toList();
        } else {
            final AnnotationMirror annotation = findAnnotationByType(typeElement, annotationClassName);
            return null != annotation ? Collections.singletonList(annotation) : Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<TypeMirror> getTypeMirrorsAnnotationParameter(
            final AnnotatedConstruct annotated, final String annotationClassName, final String parameterName) {
        return ((List<AnnotationValue>) getAnnotationValue(annotated, annotationClassName, parameterName)
                        .getValue())
                .stream().map(v -> (TypeMirror) v.getValue()).toList();
    }

    public static String getEnumAnnotationParameter(
            final AnnotatedConstruct annotated, final String annotationClassname, final String parameterName) {
        final var parameter = (VariableElement) getAnnotationValue(annotated, annotationClassname, parameterName)
                .getValue();
        return parameter.getSimpleName().toString();
    }

    public static AnnotationValue getAnnotationValue(
            final AnnotatedConstruct annotated, final String annotationClassName, final String parameterName) {
        final AnnotationValue value = findAnnotationValue(annotated, annotationClassName, parameterName);
        assert null != value;
        return Objects.requireNonNull(value);
    }

    @Nullable
    public static AnnotationValue findAnnotationValue(
            final AnnotatedConstruct annotated, final String annotationClassName, final String parameterName) {
        final AnnotationMirror annotation = findAnnotationByType(annotated, annotationClassName);
        return null == annotation ? null : findAnnotationValue(annotation, parameterName);
    }

    @Nullable
    public static AnnotationValue findAnnotationValue(final AnnotationMirror annotation, final String parameterName) {
        final Map<ExecutableElement, AnnotationValue> values = getAnnotationValuesWithDefaults(annotation);
        final ExecutableElement annotationKey = values.keySet().stream()
                .filter(k -> parameterName.equals(k.getSimpleName().toString()))
                .findFirst()
                .orElse(null);
        return null == annotationKey ? null : values.get(annotationKey);
    }

    /**
     * Returns the {@link AnnotationMirror}'s map of {@link AnnotationValue} indexed by {@link
     * ExecutableElement}, supplying default values from the annotation if the annotation property has
     * not been set. This is equivalent to {@link
     * Elements#getElementValuesWithDefaults(AnnotationMirror)} but can be called statically without
     * an {@link Elements} instance.
     *
     * <p>The iteration order of elements of the returned map will be the order in which the {@link
     * ExecutableElement}s are defined in {@code annotation}'s {@linkplain
     * AnnotationMirror#getAnnotationType() type}.
     */
    public static Map<ExecutableElement, AnnotationValue> getAnnotationValuesWithDefaults(
            final AnnotationMirror annotation) {
        final Map<ExecutableElement, AnnotationValue> values = new LinkedHashMap<>();
        final Map<? extends ExecutableElement, ? extends AnnotationValue> declaredValues =
                annotation.getElementValues();
        final List<? extends Element> enclosedElements =
                annotation.getAnnotationType().asElement().getEnclosedElements();
        for (final Element enclosedElement : enclosedElements) {
            if (ElementKind.METHOD == enclosedElement.getKind()) {
                final var method = (ExecutableElement) enclosedElement;
                // Must iterate and put in this order, to ensure consistency in generated code.
                if (declaredValues.containsKey(method)) {
                    values.put(method, declaredValues.get(method));
                } else {
                    final AnnotationValue defaultValue = method.getDefaultValue();
                    assert null != defaultValue;
                    values.put(method, defaultValue);
                }
            }
        }
        return values;
    }

    @Nullable
    public static AnnotationValue findAnnotationValueNoDefaults(
            final AnnotationMirror annotation, final String parameterName) {
        final Map<? extends ExecutableElement, ? extends AnnotationValue> values = annotation.getElementValues();
        final ExecutableElement annotationKey = values.keySet().stream()
                .filter(k -> parameterName.equals(k.getSimpleName().toString()))
                .findFirst()
                .orElse(null);
        return null == annotationKey ? null : values.get(annotationKey);
    }

    public static AnnotationValue getAnnotationValue(final AnnotationMirror annotation, final String parameterName) {
        final AnnotationValue value = findAnnotationValue(annotation, parameterName);
        assert null != value;
        return Objects.requireNonNull(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAnnotationValueValue(final AnnotationMirror annotation, final String parameterName) {
        return (T) getAnnotationValue(annotation, parameterName).getValue();
    }

    public static AnnotationMirror getAnnotationByType(
            final AnnotatedConstruct annotated, final String annotationClassName) {
        final AnnotationMirror annotation = findAnnotationByType(annotated, annotationClassName);
        assert null != annotation;
        return Objects.requireNonNull(annotation);
    }

    @Nullable
    public static AnnotationMirror findAnnotationByType(
            final AnnotatedConstruct annotated, final String annotationClassName) {
        return annotated.getAnnotationMirrors().stream()
                .filter(a -> a.getAnnotationType().toString().equals(annotationClassName))
                .findFirst()
                .orElse(null);
    }

    public static boolean hasAnnotationOfType(final AnnotatedConstruct annotated, final String annotationClassName) {
        return null != findAnnotationByType(annotated, annotationClassName);
    }

    public static String extractName(
            final ExecutableElement method,
            final Function<ExecutableElement, String> defaultExtractor,
            final String annotationClassname,
            final String parameterName,
            final String sentinelValue) {
        final var declaredName = (String)
                getAnnotationValue(method, annotationClassname, parameterName).getValue();
        return NamesUtil.extractName(
                method, defaultExtractor, annotationClassname, parameterName, sentinelValue, declaredName);
    }

    public static boolean hasNonnullAnnotation(final Element element) {
        return hasAnnotationOfType(element, NONNULL_CLASSNAME);
    }

    public static boolean hasNullableAnnotation(final Element element) {
        return hasAnnotationOfType(element, NULLABLE_CLASSNAME);
    }
}
