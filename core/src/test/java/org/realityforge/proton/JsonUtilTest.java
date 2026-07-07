package org.realityforge.proton;

import static org.testng.Assert.*;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.testng.annotations.Test;

public final class JsonUtilTest {
    @Test
    public void formatJsonReducesJsonGeneratorIndentation() {
        assertEquals(JsonUtil.formatJson("""
            {
                "name":"Widget",
                "nested":{
                    "enabled":true,
                    "values":[
                        1
                    ]
                }
            }\
            """), """
            {
              "name":"Widget",
              "nested":{
                "enabled":true,
                "values":[
                  1
                ]
              }
            }
            """);
    }

    @Test
    public void formatJsonRemovesLeadingBlankLineBeforeRootObject() {
        assertEquals(JsonUtil.formatJson("""

            {
                "name":"Widget"
            }\
            """), """
            {
              "name":"Widget"
            }
            """);
    }

    @Test
    public void formatJsonRemovesLeadingBlankLineBeforeRootArray() {
        assertEquals(JsonUtil.formatJson("""

            [
                "Widget"
            ]\
            """), """
            [
              "Widget"
            ]
            """);
    }

    @Test
    public void writeJsonResourceFormatsAndWritesResource() throws Exception {
        final var resource = new CapturingResource();
        final Element element = proxy(Element.class, (self, method, args) -> unsupported(method));
        final ProcessingEnvironment processingEnv =
                processingEnvironment(filer("metadata.json", element, resource.asFileObject()));

        JsonUtil.writeJsonResource(
                processingEnv,
                element,
                "metadata.json",
                g -> g.writeStartObject()
                        .write("name", "Widget")
                        .writeStartArray("values")
                        .write(1)
                        .write(2)
                        .writeEnd()
                        .writeEnd());

        assertEquals(resource.content(), """
            {
              "name": "Widget",
              "values": [
                1,
                2
              ]
            }
            """);
        assertFalse(resource.isDeleted());
    }

    private static ProcessingEnvironment processingEnvironment(final Filer filer) {
        return proxy(
                ProcessingEnvironment.class,
                (self, method, args) -> "getFiler".equals(method.getName()) ? filer : unsupported(method));
    }

    @SuppressWarnings("SameParameterValue")
    private static Filer filer(final String filename, final Element element, final FileObject fileObject) {
        return proxy(Filer.class, (self, method, args) -> {
            if ("createResource".equals(method.getName())) {
                assertSame(args[0], StandardLocation.CLASS_OUTPUT);
                assertEquals(args[1], "");
                assertEquals(args[2], filename);
                final var originatingElements = (Element[]) args[3];
                assertEquals(originatingElements.length, 1);
                assertSame(originatingElements[0], element);
                return fileObject;
            }
            return unsupported(method);
        });
    }

    private static <T> T proxy(final Class<T> type, final ProxyInvocation invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (self, method, args) -> {
            if ("equals".equals(method.getName())) {
                return self == args[0];
            } else if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(self);
            } else if ("toString".equals(method.getName())) {
                return type.getSimpleName() + "Proxy";
            } else {
                return invocation.invoke(self, method, args);
            }
        }));
    }

    private static Object unsupported(final Method method) {
        throw new UnsupportedOperationException(method.toString());
    }

    private interface ProxyInvocation {
        Object invoke(Object self, Method method, Object[] args) throws Throwable;
    }

    private static final class CapturingResource {
        private final ByteArrayOutputStream _outputStream = new ByteArrayOutputStream();
        private boolean _deleted;

        FileObject asFileObject() {
            return proxy(FileObject.class, (self, method, args) -> {
                if ("openOutputStream".equals(method.getName())) {
                    return _outputStream;
                } else if ("delete".equals(method.getName())) {
                    _deleted = true;
                    return true;
                } else {
                    return unsupported(method);
                }
            });
        }

        String content() {
            return _outputStream.toString(StandardCharsets.UTF_8);
        }

        boolean isDeleted() {
            return _deleted;
        }
    }
}
