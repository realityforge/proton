package org.realityforge.proton;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("unused")
public final class SuppressWarningsUtil {
    private SuppressWarningsUtil() {}

    public static boolean isNotSuppressed(final AnnotatedConstruct annotated, final String warning) {
        return !isSuppressed(annotated, warning);
    }

    public static boolean isNotSuppressed(
            final AnnotatedConstruct annotated,
            final String warning,
            @Nullable final String alternativeSuppressWarnings) {
        return !isSuppressed(annotated, warning, alternativeSuppressWarnings);
    }

    public static boolean isNotSuppressed(final Element element, final String warning) {
        return !isSuppressed(element, warning);
    }

    public static boolean isNotSuppressed(
            final Element element, final String warning, @Nullable final String alternativeSuppressWarnings) {
        return !isSuppressed(element, warning, alternativeSuppressWarnings);
    }

    public static boolean isSuppressed(final AnnotatedConstruct annotated, final String warning) {
        return isSuppressed(annotated, warning, null);
    }

    public static boolean isSuppressed(
            final AnnotatedConstruct annotated,
            final String warning,
            @Nullable final String alternativeSuppressWarnings) {
        return null != alternativeSuppressWarnings
                        && isSuppressedByAnnotation(annotated, warning, alternativeSuppressWarnings)
                || isSuppressedBySuppressWarnings(annotated, warning);
    }

    public static boolean isSuppressed(final Element element, final String warning) {
        return isSuppressed(element, warning, null);
    }

    public static boolean isSuppressed(
            final Element element, final String warning, @Nullable final String alternativeSuppressWarnings) {
        if (isSuppressed((AnnotatedConstruct) element, warning, alternativeSuppressWarnings)) {
            return true;
        } else {
            final Element enclosingElement = element.getEnclosingElement();
            return null != enclosingElement && isSuppressed(enclosingElement, warning, alternativeSuppressWarnings);
        }
    }

    private static boolean isSuppressedBySuppressWarnings(final AnnotatedConstruct annotated, final String warning) {
        final var annotation = annotated.getAnnotation(SuppressWarnings.class);
        if (null != annotation) {
            for (final String suppression : annotation.value()) {
                if (warning.equals(suppression)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static boolean isSuppressedByAnnotation(
            final AnnotatedConstruct annotated, final String warning, final String annotationClassname) {
        final AnnotationMirror suppress = AnnotationsUtil.findAnnotationByType(annotated, annotationClassname);
        if (null != suppress) {
            final AnnotationValue value = AnnotationsUtil.findAnnotationValueNoDefaults(suppress, "value");
            if (null != value) {
                final var warnings = (List<AnnotationValue>) value.getValue();
                for (final AnnotationValue suppression : warnings) {
                    if (warning.equals(suppression.getValue())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static AnnotationSpec suppressWarningsAnnotation(final @Nullable String... warnings) {
        return Objects.requireNonNull(maybeSuppressWarningsAnnotation(warnings));
    }

    @Nullable
    public static AnnotationSpec maybeSuppressWarningsAnnotation(final @Nullable String... warnings) {
        final List<String> actualWarnings =
                Arrays.stream(warnings).filter(Objects::nonNull).sorted().toList();
        if (actualWarnings.isEmpty()) {
            return null;
        } else {
            final AnnotationSpec.Builder builder = AnnotationSpec.builder(SuppressWarnings.class);
            actualWarnings.forEach(w -> builder.addMember("value", "$S", w));
            return builder.build();
        }
    }

    public static void addSuppressWarningsIfRequired(
            final ProcessingEnvironment processingEnv, final TypeSpec.Builder method, final TypeMirror type) {
        addSuppressWarningsIfRequired(processingEnv, method, Collections.singleton(type));
    }

    public static void addSuppressWarningsIfRequired(
            final ProcessingEnvironment processingEnv,
            final TypeSpec.Builder method,
            final Collection<TypeMirror> types) {
        addSuppressWarningsIfRequired(processingEnv, method, Collections.emptyList(), types);
    }

    public static void addSuppressWarningsIfRequired(
            final ProcessingEnvironment processingEnv,
            final TypeSpec.Builder method,
            final Collection<String> additionalSuppressions,
            final Collection<TypeMirror> types) {
        final AnnotationSpec suppress = maybeSuppressWarningsAnnotation(processingEnv, additionalSuppressions, types);
        if (null != suppress) {
            method.addAnnotation(suppress);
        }
    }

    public static void addSuppressWarningsIfRequired(
            final ProcessingEnvironment processingEnv, final MethodSpec.Builder method, final TypeMirror type) {
        addSuppressWarningsIfRequired(processingEnv, method, Collections.singleton(type));
    }

    public static void addSuppressWarningsIfRequired(
            final ProcessingEnvironment processingEnv,
            final MethodSpec.Builder method,
            final Collection<TypeMirror> types) {
        addSuppressWarningsIfRequired(processingEnv, method, Collections.emptyList(), types);
    }

    public static void addSuppressWarningsIfRequired(
            final ProcessingEnvironment processingEnv,
            final MethodSpec.Builder method,
            final Collection<String> additionalSuppressions,
            final Collection<TypeMirror> types) {
        final AnnotationSpec suppress = maybeSuppressWarningsAnnotation(processingEnv, additionalSuppressions, types);
        if (null != suppress) {
            method.addAnnotation(suppress);
        }
    }

    public static void addSuppressWarningsIfRequired(
            final ProcessingEnvironment processingEnv, final FieldSpec.Builder field, final TypeMirror type) {
        addSuppressWarningsIfRequired(processingEnv, field, Collections.singleton(type));
    }

    public static void addSuppressWarningsIfRequired(
            final ProcessingEnvironment processingEnv,
            final FieldSpec.Builder field,
            final Collection<TypeMirror> types) {
        addSuppressWarningsIfRequired(processingEnv, field, Collections.emptyList(), types);
    }

    public static void addSuppressWarningsIfRequired(
            final ProcessingEnvironment processingEnv,
            final FieldSpec.Builder field,
            final Collection<String> additionalSuppressions,
            final Collection<TypeMirror> types) {
        final AnnotationSpec suppress = maybeSuppressWarningsAnnotation(processingEnv, additionalSuppressions, types);
        if (null != suppress) {
            field.addAnnotation(suppress);
        }
    }

    public static void addSuppressWarningsIfRequired(
            final ProcessingEnvironment processingEnv, final ParameterSpec.Builder field, final TypeMirror type) {
        addSuppressWarningsIfRequired(processingEnv, field, Collections.singleton(type));
    }

    public static void addSuppressWarningsIfRequired(
            final ProcessingEnvironment processingEnv,
            final ParameterSpec.Builder field,
            final Collection<TypeMirror> types) {
        addSuppressWarningsIfRequired(processingEnv, field, Collections.emptyList(), types);
    }

    public static void addSuppressWarningsIfRequired(
            final ProcessingEnvironment processingEnv,
            final ParameterSpec.Builder field,
            final Collection<String> additionalSuppressions,
            final Collection<TypeMirror> types) {
        final AnnotationSpec suppress = maybeSuppressWarningsAnnotation(processingEnv, additionalSuppressions, types);
        if (null != suppress) {
            field.addAnnotation(suppress);
        }
    }

    /**
     * Generate a suppress warnings annotation if any of the types passed in are either deprecated or rawtypes.
     *
     * @param processingEnv the processing environment.
     * @param types         the types to analyze to determine if suppressions are required.
     * @return a suppress annotation if required.
     */
    @Nullable
    public static AnnotationSpec maybeSuppressWarningsAnnotation(
            final ProcessingEnvironment processingEnv, final Collection<TypeMirror> types) {
        return maybeSuppressWarningsAnnotation(processingEnv, Collections.emptyList(), types);
    }

    /**
     * Generate a suppress warnings annotation if any of the types passed in are either deprecated or rawtypes.
     * The additionalSuppressions parameter will also be added to list of suppressions.
     *
     * @param processingEnv          the processing environment.
     * @param additionalSuppressions the suppressions that must be added to suppression annotation.
     * @param types                  the types to analyze to determine if suppressions are required.
     * @return a suppress annotation if required.
     */
    @Nullable
    public static AnnotationSpec maybeSuppressWarningsAnnotation(
            final ProcessingEnvironment processingEnv,
            final Collection<String> additionalSuppressions,
            final Collection<TypeMirror> types) {
        // short cut traversing types by checking whether additionalSuppressions match
        final boolean hasRawTypes = additionalSuppressions.contains("rawtypes")
                || types.stream().anyMatch(t -> TypesUtil.hasRawTypes(processingEnv, t));

        final boolean hasDeprecatedTypes = additionalSuppressions.contains("deprecation")
                || types.stream().anyMatch(t -> TypesUtil.isDeprecated(processingEnv, t));

        if (hasRawTypes || hasDeprecatedTypes || !additionalSuppressions.isEmpty()) {
            final ArrayList<String> suppressions = new ArrayList<>(additionalSuppressions);
            if (hasRawTypes) {
                suppressions.add("rawtypes");
            }
            if (hasDeprecatedTypes) {
                suppressions.add("deprecation");
            }
            return suppressWarningsAnnotation(
                    suppressions.stream().sorted().distinct().toArray(String[]::new));
        } else {
            return null;
        }
    }
}
