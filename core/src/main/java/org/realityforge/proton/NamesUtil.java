package org.realityforge.proton;

import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class NamesUtil {
    private NamesUtil() {}

    public static boolean isJavaIdentifier(final String name) {
        return SourceVersion.isIdentifier(name) && !SourceVersion.isKeyword(name);
    }

    public static String mustBeJavaIdentifier(
            final String annotationClassname, final String parameterName, final String name, final Element element) {
        if (!SourceVersion.isIdentifier(name)) {
            throw new ProcessorException(
                    MemberChecks.toSimpleName(annotationClassname) + " target specified an " + "invalid value '"
                            + name + "' for the parameter " + parameterName + ". "
                            + "The value must be a valid java identifier",
                    element);
        } else if (SourceVersion.isKeyword(name)) {
            throw new ProcessorException(
                    MemberChecks.toSimpleName(annotationClassname) + " target specified an " + "invalid value '"
                            + name + "' for the parameter " + parameterName + ". "
                            + "The value must not be a java keyword",
                    element);
        } else {
            return name;
        }
    }

    public static String firstCharacterToLowerCase(final String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    @Nullable
    public static String deriveName(
            final Element element, final Pattern pattern, final String name, final String sentinelValue) {
        if (sentinelValue.equals(name)) {
            final Matcher matcher = pattern.matcher(element.getSimpleName().toString());
            return matcher.find() ? firstCharacterToLowerCase(matcher.group(1)) : null;
        } else {
            return name;
        }
    }

    public static String getPropertyAccessorName(
            final ExecutableElement method,
            final Pattern getterPattern,
            final Pattern booleanGetterPattern,
            final String specifiedName,
            final String sentinelValue) {
        String name = deriveName(method, getterPattern, specifiedName, sentinelValue);
        if (null != name) {
            return name;
        } else if (TypeKind.BOOLEAN == method.getReturnType().getKind()) {
            name = deriveName(method, booleanGetterPattern, specifiedName, sentinelValue);
            if (null != name) {
                return name;
            }
        }
        return method.getSimpleName().toString();
    }

    public static String constantCaseToLowerCamel(final String name) {
        final String[] parts = name.split("_");
        final var sb = new StringBuilder();
        for (final String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            final String lower = part.toLowerCase(Locale.ENGLISH);
            if (sb.isEmpty()) {
                sb.append(lower);
            } else {
                sb.append(Character.toUpperCase(lower.charAt(0)));
                if (lower.length() > 1) {
                    sb.append(lower.substring(1));
                }
            }
        }
        return sb.toString();
    }

    public static String extractName(
            final ExecutableElement method,
            final Function<ExecutableElement, String> defaultExtractor,
            final String annotationClassname,
            final String parameterName,
            final String sentinelValue,
            final String declaredName) {
        if (sentinelValue.equals(declaredName)) {
            final String defaultValue = defaultExtractor.apply(method);
            if (null == defaultValue) {
                throw new ProcessorException(
                        MemberChecks.toSimpleName(annotationClassname) + " target did not specify " + "the parameter "
                                + parameterName + " and the default value could not be " + "derived",
                        method);
            }
            return defaultValue;
        } else {
            return mustBeJavaIdentifier(annotationClassname, parameterName, declaredName, method);
        }
    }
}
