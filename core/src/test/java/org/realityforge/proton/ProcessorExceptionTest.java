package org.realityforge.proton;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import org.testng.annotations.Test;

public final class ProcessorExceptionTest {
    @Test
    public void constructorStoresElement() {
        final Element element = TestUtil.proxy(Element.class, (self, method, args) -> TestUtil.unsupported(method));

        final var exception = new ProcessorException("Bad element", element);

        assertEquals(exception.getMessage(), "Bad element");
        assertSame(exception.getElement(), element);
        assertNull(exception.getAnnotation());
        assertNull(exception.getAnnotationValue());
    }

    @Test
    public void constructorStoresAnnotation() {
        final Element element = TestUtil.proxy(Element.class, (self, method, args) -> TestUtil.unsupported(method));
        final AnnotationMirror annotation =
                TestUtil.proxy(AnnotationMirror.class, (self, method, args) -> TestUtil.unsupported(method));

        final var exception = new ProcessorException("Bad annotation", element, annotation);

        assertEquals(exception.getMessage(), "Bad annotation");
        assertSame(exception.getElement(), element);
        assertSame(exception.getAnnotation(), annotation);
        assertNull(exception.getAnnotationValue());
    }

    @Test
    public void constructorStoresAnnotationValue() {
        final Element element = TestUtil.proxy(Element.class, (self, method, args) -> TestUtil.unsupported(method));
        final AnnotationMirror annotation =
                TestUtil.proxy(AnnotationMirror.class, (self, method, args) -> TestUtil.unsupported(method));
        final AnnotationValue annotationValue =
                TestUtil.proxy(AnnotationValue.class, (self, method, args) -> TestUtil.unsupported(method));

        final var exception = new ProcessorException("Bad annotation value", element, annotation, annotationValue);

        assertEquals(exception.getMessage(), "Bad annotation value");
        assertSame(exception.getElement(), element);
        assertSame(exception.getAnnotation(), annotation);
        assertSame(exception.getAnnotationValue(), annotationValue);
    }
}
