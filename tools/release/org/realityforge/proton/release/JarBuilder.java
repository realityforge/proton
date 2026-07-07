package org.realityforge.proton.release;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

public final class JarBuilder {
    private static final long STABLE_TIME = 0L;

    private JarBuilder() {}

    public static void main(final String[] args) throws Exception {
        if (args.length == 0 || !"merge".equals(args[0])) {
            throw new IllegalArgumentException("Usage: merge --output <jar> [--input <jar>...] [--shade-input <jar>...]"
                    + " [--relocate <from=to>...] [--skip-prefix <prefix>...]");
        }
        merge(parse(args));
    }

    private static Options parse(final String[] args) {
        final var inputs = new ArrayList<Path>();
        final var shadeInputs = new ArrayList<Path>();
        final var relocations = new ArrayList<Relocation>();
        final var skipPrefixes = new ArrayList<String>();
        Path output = null;
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--output":
                    output = Path.of(args[++i]);
                    break;
                case "--input":
                    inputs.add(Path.of(args[++i]));
                    break;
                case "--shade-input":
                    shadeInputs.add(Path.of(args[++i]));
                    break;
                case "--relocate":
                    relocations.add(Relocation.parse(args[++i]));
                    break;
                case "--skip-prefix":
                    skipPrefixes.add(args[++i]);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }
        if (output == null) {
            throw new IllegalArgumentException("Missing --output");
        }
        return new Options(output, inputs, shadeInputs, relocations, skipPrefixes);
    }

    private static void merge(final Options options) throws IOException {
        final var entries = new LinkedHashMap<String, byte[]>();
        final var services = new TreeMap<String, LinkedHashSet<String>>();

        for (final Path input : options.inputs()) {
            mergeJar(input, entries, services, List.of(), options.skipPrefixes());
        }
        for (final Path input : options.shadeInputs()) {
            mergeJar(input, entries, services, options.relocations(), options.skipPrefixes());
        }

        Files.createDirectories(options.output().toAbsolutePath().getParent());
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(options.output()), manifest())) {
            writeEntries(out, entries, services);
        }
    }

    private static Manifest manifest() {
        final var manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        return manifest;
    }

    private static void mergeJar(
            final Path input,
            final Map<String, byte[]> entries,
            final Map<String, LinkedHashSet<String>> services,
            final List<Relocation> relocations,
            final List<String> skipPrefixes)
            throws IOException {
        try (JarFile jar = new JarFile(input.toFile())) {
            final var jarEntries = Collections.list(jar.entries());
            jarEntries.sort(Comparator.comparing(ZipEntry::getName));
            for (final JarEntry entry : jarEntries) {
                final String originalName = entry.getName();
                if (entry.isDirectory() || shouldSkip(originalName, skipPrefixes)) {
                    continue;
                }
                final String name = relocateEntryName(originalName, relocations);
                final byte[] content = transformContent(jar, entry, originalName, relocations);
                if (isLineMergedMetadata(name)) {
                    mergeService(services, name, content);
                } else {
                    addEntry(entries, name, content, isLegalMetadata(name));
                }
            }
        }
    }

    private static boolean shouldSkip(final String name, final List<String> skipPrefixes) {
        if ("META-INF/MANIFEST.MF".equalsIgnoreCase(name)) {
            return true;
        }
        if (isModuleDescriptor(name)) {
            return true;
        }
        for (final String prefix : skipPrefixes) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        final var upper = name.toUpperCase(Locale.ROOT);
        return upper.startsWith("META-INF/")
                && (upper.endsWith(".SF") || upper.endsWith(".RSA") || upper.endsWith(".DSA") || upper.endsWith(".EC"));
    }

    private static boolean isModuleDescriptor(final String name) {
        return "module-info.class".equals(name)
                || (name.startsWith("META-INF/versions/") && name.endsWith("/module-info.class"));
    }

    private static boolean isLegalMetadata(final String name) {
        final var upper = name.toUpperCase(Locale.ROOT);
        return upper.startsWith("META-INF/LICENSE")
                || upper.startsWith("META-INF/NOTICE")
                || upper.startsWith("META-INF/DEPENDENCIES");
    }

    private static boolean isLineMergedMetadata(final String name) {
        return name.startsWith("META-INF/services/") || name.startsWith("META-INF/sisu/");
    }

    private static byte[] transformContent(
            final JarFile jar, final JarEntry entry, final String name, final List<Relocation> relocations)
            throws IOException {
        final byte[] content = read(jar, entry);
        if (relocations.isEmpty()) {
            return content;
        }
        if (name.endsWith(".class")) {
            return relocateClass(content, relocations);
        }
        if (isLineMergedMetadata(name)) {
            return relocateText(content, relocations);
        }
        return content;
    }

    private static byte[] read(final JarFile jar, final ZipEntry entry) throws IOException {
        try (InputStream input = jar.getInputStream(entry)) {
            return input.readAllBytes();
        }
    }

    private static byte[] relocateClass(final byte[] content, final List<Relocation> relocations) {
        final var reader = new ClassReader(content);
        final var writer = new ClassWriter(0);
        final ClassVisitor remapper = new ClassRemapper(writer, new PrefixRemapper(relocations));
        reader.accept(remapper, 0);
        return writer.toByteArray();
    }

    private static byte[] relocateText(final byte[] content, final List<Relocation> relocations) {
        String text = new String(content, StandardCharsets.UTF_8);
        for (final Relocation relocation : relocations) {
            text = text.replace(relocation.fromDotted(), relocation.toDotted());
        }
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String relocateEntryName(final String name, final List<Relocation> relocations) {
        String result = name;
        for (final Relocation relocation : relocations) {
            result = relocation.relocatePath(result);
        }
        return result;
    }

    private static void mergeService(
            final Map<String, LinkedHashSet<String>> services, final String name, final byte[] content) {
        final var lines = services.computeIfAbsent(name, ignored -> new LinkedHashSet<>());
        final var text = new String(content, StandardCharsets.UTF_8);
        for (final String line : text.split("\\R")) {
            final String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                lines.add(trimmed);
            }
        }
    }

    private static void addEntry(
            final Map<String, byte[]> entries, final String name, final byte[] content, final boolean keepFirst)
            throws IOException {
        final byte[] existing = entries.get(name);
        if (existing == null) {
            entries.put(name, content);
        } else if (keepFirst || java.util.Arrays.equals(existing, content)) {
            return;
        } else {
            throw new IOException("Duplicate non-identical jar entry: " + name);
        }
    }

    private static void writeEntries(
            final JarOutputStream out,
            final Map<String, byte[]> entries,
            final Map<String, LinkedHashSet<String>> services)
            throws IOException {
        final var sortedEntries = new TreeMap<>(entries);
        for (final Map.Entry<String, byte[]> entry : sortedEntries.entrySet()) {
            writeEntry(out, entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<String, LinkedHashSet<String>> service : services.entrySet()) {
            final var content = new ByteArrayOutputStream();
            for (final String line : service.getValue()) {
                content.write(line.getBytes(StandardCharsets.UTF_8));
                content.write('\n');
            }
            writeEntry(out, service.getKey(), content.toByteArray());
        }
    }

    private static void writeEntry(final JarOutputStream out, final String name, final byte[] content)
            throws IOException {
        final var entry = new JarEntry(name);
        entry.setTime(STABLE_TIME);
        out.putNextEntry(entry);
        out.write(content);
        out.closeEntry();
    }

    private static final class PrefixRemapper extends Remapper {
        private final List<Relocation> _relocations;

        private PrefixRemapper(final List<Relocation> relocations) {
            super(Opcodes.ASM9);
            _relocations = relocations;
        }

        @Override
        public String map(final String internalName) {
            for (final Relocation relocation : _relocations) {
                final String mapped = relocation.relocateInternalName(internalName);
                if (!mapped.equals(internalName)) {
                    return mapped;
                }
            }
            return internalName;
        }

        @Override
        public Object mapValue(final Object value) {
            if (value instanceof final String string) {
                String result = string;
                for (final Relocation relocation : _relocations) {
                    result = relocation.relocateString(result);
                }
                return result;
            }
            return super.mapValue(value);
        }
    }

    private static final class Relocation {
        private final String _fromDotted;
        private final String _toDotted;
        private final String _fromInternal;
        private final String _toInternal;

        private Relocation(final String from, final String to) {
            _fromDotted = from;
            _toDotted = to;
            _fromInternal = from.replace('.', '/');
            _toInternal = to.replace('.', '/');
        }

        private static Relocation parse(final String value) {
            final int index = value.indexOf('=');
            if (index <= 0 || index == value.length() - 1) {
                throw new IllegalArgumentException("Expected relocation in the form <from=to>");
            }
            return new Relocation(value.substring(0, index), value.substring(index + 1));
        }

        private String fromDotted() {
            return _fromDotted;
        }

        private String toDotted() {
            return _toDotted;
        }

        private String relocateInternalName(final String name) {
            if (name.equals(_fromInternal)) {
                return _toInternal;
            }
            final String prefix = _fromInternal + "/";
            return name.startsWith(prefix) ? _toInternal + name.substring(_fromInternal.length()) : name;
        }

        private String relocatePath(final String path) {
            String result = relocateInternalName(path);
            final String servicePrefix = "META-INF/services/" + _fromDotted;
            if (result.equals(servicePrefix) || result.startsWith(servicePrefix + ".")) {
                result = "META-INF/services/" + _toDotted + result.substring(servicePrefix.length());
            }
            return result;
        }

        private String relocateString(final String value) {
            if (value.equals(_fromDotted)) {
                return _toDotted;
            }
            if (value.startsWith(_fromDotted + ".")) {
                return _toDotted + value.substring(_fromDotted.length());
            }
            return relocateInternalName(value);
        }
    }

    private static final class Options {
        private final Path _output;
        private final List<Path> _inputs;
        private final List<Path> _shadeInputs;
        private final List<Relocation> _relocations;
        private final List<String> _skipPrefixes;

        private Options(
                final Path output,
                final List<Path> inputs,
                final List<Path> shadeInputs,
                final List<Relocation> relocations,
                final List<String> skipPrefixes) {
            _output = output;
            _inputs = inputs;
            _shadeInputs = shadeInputs;
            _relocations = relocations;
            _skipPrefixes = skipPrefixes;
        }

        private Path output() {
            return _output;
        }

        private List<Path> inputs() {
            return _inputs;
        }

        private List<Path> shadeInputs() {
            return _shadeInputs;
        }

        private List<Relocation> relocations() {
            return _relocations;
        }

        private List<String> skipPrefixes() {
            return _skipPrefixes;
        }
    }
}
