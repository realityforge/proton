package org.realityforge.proton;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

public final class ResourceUtil {
    private ResourceUtil() {}

    public static void writeResource(
            final ProcessingEnvironment processingEnv,
            final String filename,
            final String content,
            final Element element)
            throws IOException {
        final FileObject resource =
                processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", filename, element);
        try (final OutputStream outputStream = resource.openOutputStream()) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (final IOException e) {
            resource.delete();
            throw e;
        }
    }
}
