package org.realityforge.proton.qa;

import static org.testng.Assert.*;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

public record Compilation(
        boolean success,
        Path sourceOutput,
        List<String> sourceOutputFilenames,
        Path classOutput,
        List<String> classOutputFilenames,
        List<Diagnostic<? extends JavaFileObject>> diagnostics) {
    public Compilation {
        assertNotNull(sourceOutput);
        assertNotNull(sourceOutputFilenames);
        assertNotNull(classOutput);
        assertNotNull(classOutputFilenames);
        assertNotNull(diagnostics);
    }

    public void assertJavaFileCount(final long count) {
        assertSourceOutputFilenameCount(f -> f.endsWith(".java"), count);
    }

    public void assertSourceOutputFilenameCount(final Predicate<String> predicate, final long count) {
        assertEquals(sourceOutputFilenames.stream().filter(predicate).count(), count);
    }

    public void assertJavaSourcePresent(final String classname) {
        final String filename = classname.replace(".", "/") + ".java";
        assertSourceOutputFilenamePresent(filename);
    }

    public void assertSourceOutputFilenamePresent(final String filename) {
        assertTrue(
                sourceOutputFilenames.stream().anyMatch(f -> f.equals(filename)),
                "Missing source output filename " + filename);
    }

    public void assertClassFileCount(final long count) {
        assertClassOutputFilenameCount(f -> f.endsWith(".class"), count);
    }

    public void assertClassOutputFilenameCount(final Predicate<String> predicate, final long count) {
        assertEquals(classOutputFilenames.stream().filter(predicate).count(), count);
    }

    public void assertJavaClassPresent(final String classname) {
        final String filename = classname.replace(".", "/") + ".class";
        assertClassOutputFilenamePresent(filename);
    }

    public void assertClassOutputFilenamePresent(final String filename) {
        assertTrue(
                classOutputFilenames.stream().anyMatch(f -> f.equals(filename)),
                "Missing source output filename " + filename);
    }
}
