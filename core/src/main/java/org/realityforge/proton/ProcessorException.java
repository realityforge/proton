package org.realityforge.proton;

import java.util.Objects;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import org.jspecify.annotations.Nullable;

public final class ProcessorException extends RuntimeException {
    private final Element _element;

    @Nullable
    private final AnnotationMirror _annotation;

    @Nullable
    private final AnnotationValue _annotationValue;

    public ProcessorException(final String message, final Element element) {
        this(message, element, null);
    }

    public ProcessorException(
            final String message, final Element element, @Nullable final AnnotationMirror annotation) {
        this(message, element, annotation, null);
    }

    public ProcessorException(
            final String message,
            final Element element,
            @Nullable final AnnotationMirror annotation,
            @Nullable final AnnotationValue annotationValue) {
        super(message);
        assert null == annotationValue || null != annotation;
        _element = Objects.requireNonNull(element);
        _annotation = annotation;
        _annotationValue = annotationValue;
    }

    public Element getElement() {
        return _element;
    }

    @Nullable
    public AnnotationMirror getAnnotation() {
        return _annotation;
    }

    @Nullable
    public AnnotationValue getAnnotationValue() {
        return _annotationValue;
    }
}
