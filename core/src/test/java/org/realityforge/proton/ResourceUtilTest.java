package org.realityforge.proton;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.testng.annotations.Test;

public final class ResourceUtilTest {
    @Test
    public void writeResourceWritesUtf8Content() throws Exception {
        final var resource = new CapturingResource();
        final Element element = TestUtil.proxy(Element.class, (self, method, args) -> TestUtil.unsupported(method));

        ResourceUtil.writeResource(
                processingEnvironment("metadata.txt", element, resource.asFileObject()),
                "metadata.txt",
                "Hello \u00B5",
                element);

        assertEquals(resource.content(), "Hello \u00B5");
        assertFalse(resource.isDeleted());
    }

    @Test
    public void writeResourceDeletesResourceWhenWriteFails() {
        final var failure = new IOException("Write failed");
        final var resource = new FailingResource(failure);
        final Element element = TestUtil.proxy(Element.class, (self, method, args) -> TestUtil.unsupported(method));

        try {
            ResourceUtil.writeResource(
                    processingEnvironment("metadata.txt", element, resource.asFileObject()),
                    "metadata.txt",
                    "content",
                    element);
            fail("Expected IOException");
        } catch (final IOException e) {
            assertSame(e, failure);
        }
        assertTrue(resource.isDeleted());
    }

    @Test
    public void writeResourceDeletesResourceWhenOpenFails() {
        final var failure = new IOException("Open failed");
        final var resource = new FailingOpenResource(failure);
        final Element element = TestUtil.proxy(Element.class, (self, method, args) -> TestUtil.unsupported(method));

        try {
            ResourceUtil.writeResource(
                    processingEnvironment("metadata.txt", element, resource.asFileObject()),
                    "metadata.txt",
                    "content",
                    element);
            fail("Expected IOException");
        } catch (final IOException e) {
            assertSame(e, failure);
        }
        assertTrue(resource.isDeleted());
    }

    @SuppressWarnings("SameParameterValue")
    private static ProcessingEnvironment processingEnvironment(
            final String filename, final Element element, final FileObject fileObject) {
        return TestUtil.proxy(ProcessingEnvironment.class, (self, method, args) -> {
            if ("getFiler".equals(method.getName())) {
                return filer(filename, element, fileObject);
            }
            return TestUtil.unsupported(method);
        });
    }

    private static Filer filer(final String filename, final Element element, final FileObject fileObject) {
        return TestUtil.proxy(Filer.class, (self, method, args) -> {
            if ("createResource".equals(method.getName())) {
                assertSame(args[0], StandardLocation.CLASS_OUTPUT);
                assertEquals(args[1], "");
                assertEquals(args[2], filename);
                final var originatingElements = (Element[]) args[3];
                assertEquals(originatingElements.length, 1);
                assertSame(originatingElements[0], element);
                return fileObject;
            }
            return TestUtil.unsupported(method);
        });
    }

    private static final class CapturingResource {
        private final ByteArrayOutputStream _outputStream = new ByteArrayOutputStream();

        private boolean _deleted;

        FileObject asFileObject() {
            return TestUtil.proxy(FileObject.class, (self, method, args) -> {
                if ("openOutputStream".equals(method.getName())) {
                    return _outputStream;
                } else if ("delete".equals(method.getName())) {
                    _deleted = true;
                    return true;
                }
                return TestUtil.unsupported(method);
            });
        }

        String content() {
            return _outputStream.toString(StandardCharsets.UTF_8);
        }

        boolean isDeleted() {
            return _deleted;
        }
    }

    private static final class FailingResource {
        private final IOException _failure;

        private boolean _deleted;

        FailingResource(final IOException failure) {
            _failure = failure;
        }

        FileObject asFileObject() {
            return TestUtil.proxy(FileObject.class, (self, method, args) -> {
                if ("openOutputStream".equals(method.getName())) {
                    return new OutputStream() {
                        @Override
                        public void write(final int b) throws IOException {
                            throw _failure;
                        }

                        @Override
                        public void write(final byte[] b, final int off, final int len) throws IOException {
                            throw _failure;
                        }
                    };
                } else if ("delete".equals(method.getName())) {
                    _deleted = true;
                    return true;
                }
                return TestUtil.unsupported(method);
            });
        }

        boolean isDeleted() {
            return _deleted;
        }
    }

    private static final class FailingOpenResource {
        private final IOException _failure;

        private boolean _deleted;

        FailingOpenResource(final IOException failure) {
            _failure = failure;
        }

        FileObject asFileObject() {
            return TestUtil.proxy(FileObject.class, (self, method, args) -> {
                if ("openOutputStream".equals(method.getName())) {
                    throw _failure;
                } else if ("delete".equals(method.getName())) {
                    _deleted = true;
                    return true;
                }
                return TestUtil.unsupported(method);
            });
        }

        boolean isDeleted() {
            return _deleted;
        }
    }
}
