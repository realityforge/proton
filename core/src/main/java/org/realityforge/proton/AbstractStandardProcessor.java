package org.realityforge.proton;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

public abstract class AbstractStandardProcessor extends AbstractProcessor {
    @Nonnull
    private static final String ORIGINAL_FORMATTER_CLASSNAME = "com.palantir.javaformat.java.Formatter";

    @Nonnull
    private static final List<String> COMMON_OPTIONS = Collections.unmodifiableList(Arrays.asList(
            "verbose_out_of_round.errors",
            "defer.errors",
            "defer.unresolved",
            "debug",
            "profile",
            "warnings_as_errors",
            "format_generated_source"));

    @Nonnull
    private static final List<String> FORMATTER_JDK_EXPORTS = Collections.unmodifiableList(Arrays.asList(
            "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"));

    /**
     * Names of types that have been passed to the processor in the current round or earlier rounds.
     * The set is used to restrict which types the processor will return in <code>getNewTypeElementsToProcess()</code>.
     * This is cleared in when processingOver() returns true.
     */
    @Nonnull
    private final Set<String> _rootTypeNames = new HashSet<>();

    @Nonnull
    private final StopWatch _emitJavaTypeStopWatch = new StopWatch("Emit Java Type");

    @Nonnull
    private final StopWatch _validateElementStopWatch = new StopWatch("Validate Element");

    @Nonnull
    private final StopWatch _extractDeferredStopWatch = new StopWatch("Extract Deferred");

    private boolean _verboseOutOfRoundErrors;
    private boolean _deferErrors;
    private boolean _deferUnresolved;
    private boolean _debug;
    private boolean _profile;
    private boolean _warningsAsErrors;
    private boolean _formatGeneratedSource;

    @Nullable
    private FormatterProxy _formatter;

    private int _invalidTypeCount;

    private record FormatterProxy(
            @Nonnull Object formatter, @Nonnull Method formatSourceMethod) {}

    @FunctionalInterface
    public interface Action<E extends Element> {
        void process(@Nonnull E element) throws Exception;
    }

    @Override
    public synchronized void init(@Nonnull final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        _verboseOutOfRoundErrors = readBooleanOption("verbose_out_of_round.errors", true);
        _deferErrors = readBooleanOption("defer.errors", true);
        _deferUnresolved = readBooleanOption("defer.unresolved", true);
        _debug = readBooleanOption("debug", false);
        _profile = readBooleanOption("profile", false);
        _warningsAsErrors = readBooleanOption("warnings_as_errors", false);
        _formatGeneratedSource = readBooleanOption("format_generated_source", true);
    }

    @Override
    @Nonnull
    public Set<String> getSupportedOptions() {
        final Set<String> options = new HashSet<>(super.getSupportedOptions());
        for (final String option : COMMON_OPTIONS) {
            options.add(getOptionPrefix() + "." + option);
        }
        return Collections.unmodifiableSet(options);
    }

    protected final void debugAnnotationProcessingRootElements(@Nonnull final RoundEnvironment env) {
        if (isDebugEnabled()) {
            for (final Element element : env.getRootElements()) {
                if (element instanceof TypeElement) {
                    debug(() -> "Annotation processing root element " + ((TypeElement) element).getQualifiedName());
                }
            }
        }
    }

    protected final void processTypeElements(
            @Nonnull final Set<? extends TypeElement> annotations,
            @Nonnull final RoundEnvironment env,
            @Nonnull final String annotationClassname,
            @Nonnull final DeferredElementSet deferredTypes,
            @Nonnull final String label,
            @Nonnull final Action<TypeElement> action,
            @Nonnull final StopWatch actionStopWatch) {
        processTypeElements(
                annotations,
                env,
                annotationClassname,
                deferredTypes,
                label,
                action,
                actionStopWatch,
                e -> SuperficialValidation.validateElement(processingEnv, e));
    }

    protected final void processTypeElements(
            @Nonnull final Set<? extends TypeElement> annotations,
            @Nonnull final RoundEnvironment env,
            @Nonnull final String annotationClassname,
            @Nonnull final DeferredElementSet deferredTypes,
            @Nonnull final String label,
            @Nonnull final Action<TypeElement> action,
            @Nonnull final StopWatch actionStopWatch,
            @Nonnull final Predicate<TypeElement> isValidPredicate) {
        final Collection<TypeElement> newElementsToProcess =
                getNewTypeElementsToProcess(annotations, env, annotationClassname);
        if (!deferredTypes.getDeferred().isEmpty() || !newElementsToProcess.isEmpty()) {
            processTypeElements(
                    env, deferredTypes, newElementsToProcess, label, action, actionStopWatch, isValidPredicate);
        }
    }

    /**
     * Return the types annotated by specified annotation, processed in the current round.
     *
     * @param annotations         the annotation types requested to be processed.
     * @param env                 environment for information about the current and prior round.
     * @param annotationClassname the annotation classname to search for.
     * @return the types annotated by specified annotation, processed in the current round.
     */
    @SuppressWarnings("unchecked")
    protected final Collection<TypeElement> getNewTypeElementsToProcess(
            @Nonnull final Set<? extends TypeElement> annotations,
            @Nonnull final RoundEnvironment env,
            @Nonnull final String annotationClassname) {
        return annotations.stream()
                .filter(a -> a.getQualifiedName().toString().equals(annotationClassname))
                .findAny()
                .map(a -> (Collection<TypeElement>) env.getElementsAnnotatedWith(a))
                .map(elements -> elements.stream()
                        .filter(e -> isRootType((TypeElement) ElementsUtil.getTopLevelElement(e)))
                        .toList())
                .orElse(Collections.emptyList());
    }

    private boolean isRootType(@Nonnull final TypeElement typeElement) {
        return _rootTypeNames.contains(typeElement.getQualifiedName().toString());
    }

    private void processTypeElements(
            @Nonnull final RoundEnvironment env,
            @Nonnull final DeferredElementSet deferredSet,
            @Nonnull final Collection<TypeElement> elements,
            @Nonnull final String label,
            @Nonnull final Action<TypeElement> action,
            @Nonnull final StopWatch actionStopWatch,
            @Nonnull final Predicate<TypeElement> isValidPredicate) {
        if (shouldDeferUnresolved()) {
            final Collection<TypeElement> elementsToProcess =
                    deriveElementsToProcess(deferredSet, elements, isValidPredicate);
            doProcessTypeElements(env, elementsToProcess, label, action, actionStopWatch);
            errorIfProcessingOverAndDeferredTypesUnprocessed(env, deferredSet);
        } else {
            doProcessTypeElements(env, new ArrayList<>(elements), label, action, actionStopWatch);
        }
    }

    private void errorIfProcessingOverAndDeferredTypesUnprocessed(
            @Nonnull final RoundEnvironment env, @Nonnull final DeferredElementSet deferredSet) {
        final Set<TypeElement> deferred = deferredSet.getDeferred();
        if ((env.processingOver() || env.errorRaised()) && !deferred.isEmpty()) {
            deferred.forEach(e -> processingErrorMessage(env, e));
            deferredSet.clear();
        }
    }

    protected final void errorIfProcessingOverAndInvalidTypesDetected(@Nonnull final RoundEnvironment env) {
        if (env.processingOver()) {
            if (0 != _invalidTypeCount) {
                processingEnv
                        .getMessager()
                        .printMessage(
                                Diagnostic.Kind.ERROR,
                                getClass().getSimpleName() + " failed to process " + _invalidTypeCount
                                        + " types. See earlier warnings for further details.");
            }
            _invalidTypeCount = 0;
        }
    }

    protected final void collectRootTypeNames(@Nonnull final RoundEnvironment env) {
        for (final Element element : env.getRootElements()) {
            if (element instanceof TypeElement) {
                _rootTypeNames.add(((TypeElement) element).getQualifiedName().toString());
            }
        }
    }

    protected final void clearRootTypeNamesIfProcessingOver(@Nonnull final RoundEnvironment env) {
        if (env.processingOver()) {
            _rootTypeNames.clear();
        }
    }

    protected boolean shouldDeferUnresolved() {
        return _deferUnresolved;
    }

    @Nonnull
    protected abstract String getIssueTrackerURL();

    @Nonnull
    protected abstract String getOptionPrefix();

    private void processingErrorMessage(@Nonnull final RoundEnvironment env, @Nonnull final TypeElement target) {
        reportError(
                env,
                getClass().getSimpleName() + " unable to process " + target.getQualifiedName()
                        + " because not all of its dependencies could be resolved. Check for "
                        + "compilation errors or a circular dependency with generated code.",
                target);
    }

    protected final void reportError(
            @Nonnull final RoundEnvironment env, @Nonnull final String message, @Nullable final Element element) {
        reportError(env, message, element, null, null);
    }

    protected final void reportError(
            @Nonnull final RoundEnvironment env,
            @Nonnull final String message,
            @Nullable final Element element,
            @Nullable final AnnotationMirror annotation,
            @Nullable final AnnotationValue annotationValue) {
        _invalidTypeCount++;
        final Diagnostic.Kind kind =
                !_deferErrors || env.errorRaised() || env.processingOver() ? Diagnostic.Kind.ERROR : warningKind();
        final Messager messager = processingEnv.getMessager();
        if (null != annotationValue) {
            messager.printMessage(kind, message, element, annotation, annotationValue);
        } else if (null != annotation) {
            messager.printMessage(kind, message, element, annotation);
        } else {
            messager.printMessage(kind, message, element);
        }
    }

    protected final void reportProfilerTimings() {
        if (isProfileEnabled()) {
            final Messager messager = processingEnv.getMessager();
            final Collection<StopWatch> stopWatches = new ArrayList<>();
            stopWatches.add(_emitJavaTypeStopWatch);
            stopWatches.add(_extractDeferredStopWatch);
            stopWatches.add(_validateElementStopWatch);
            collectStopWatches(stopWatches);
            messager.printMessage(Diagnostic.Kind.NOTE, getClass().getSimpleName() + " profiler timings");
            stopWatches.stream()
                    .sorted(Comparator.comparing(StopWatch::getTotalDuration).reversed())
                    .forEach(stopWatch -> {
                        messager.printMessage(
                                Diagnostic.Kind.NOTE,
                                String.format("  %30s: %20d", stopWatch.getName(), stopWatch.getTotalDuration()));
                    });
        }
    }

    protected void collectStopWatches(@Nonnull final Collection<StopWatch> stopWatches) {}

    private void doProcessTypeElements(
            @Nonnull final RoundEnvironment env,
            @Nonnull final Collection<TypeElement> elements,
            @Nonnull final String label,
            @Nonnull final Action<TypeElement> action,
            @Nonnull final StopWatch actionStopWatch) {
        for (final TypeElement element : elements) {
            performAction(env, label, action, element, actionStopWatch);
        }
    }

    protected final <E extends Element> void performAction(
            @Nonnull final RoundEnvironment env,
            @Nonnull final String label,
            @Nonnull final Action<E> action,
            @Nonnull final E element,
            @Nonnull final StopWatch actionStopWatch) {
        debug(() -> "Performing '" + label + "' action on element " + element);
        try {
            if (_profile) {
                actionStopWatch.start();
            }
            action.process(element);
            if (_profile) {
                actionStopWatch.stop();
            }
        } catch (final IOException ioe) {
            final String message = "IO error running the " + getClass().getName() + " processor. This has "
                    + "resulted in a failure to process the code and has left the compiler in an invalid "
                    + "state.\n"
                    + "\n\n"
                    + printStackTrace(ioe);
            reportError(env, message, element);
        } catch (final ProcessorException e) {
            final Element errorLocation = e.getElement();
            if (_verboseOutOfRoundErrors) {
                final Element outerElement = ElementsUtil.getTopLevelElement(errorLocation);
                if (!env.getRootElements().contains(outerElement)) {
                    final String location;
                    if (errorLocation instanceof final ExecutableElement executableElement) {
                        final TypeElement typeElement = (TypeElement) executableElement.getEnclosingElement();
                        location = typeElement.getQualifiedName() + "." + executableElement.getSimpleName();
                    } else if (errorLocation instanceof final VariableElement variableElement) {
                        final Element enclosingElement = variableElement.getEnclosingElement();
                        if (enclosingElement instanceof final TypeElement typeElement) {
                            location = typeElement.getQualifiedName() + "." + variableElement.getSimpleName();
                        } else {
                            final ExecutableElement executableElement = (ExecutableElement) enclosingElement;
                            final TypeElement typeElement = (TypeElement) executableElement.getEnclosingElement();
                            location = typeElement.getQualifiedName() + "."
                                    + executableElement.getSimpleName()
                                    + "(..."
                                    + variableElement.getSimpleName()
                                    + "...)";
                        }
                    } else {
                        assert errorLocation instanceof TypeElement;
                        final TypeElement typeElement = (TypeElement) errorLocation;
                        location = typeElement.getQualifiedName().toString();
                    }

                    final StringWriter sw = new StringWriter();
                    processingEnv.getElementUtils().printElements(sw, errorLocation);
                    sw.flush();

                    final String message = "An error was generated processing the element " + element.getSimpleName()
                            + " but the error was triggered by code not currently being compiled but inherited or "
                            + "implemented by the element and may not be highlighted by your tooling or IDE. The "
                            + "error occurred at "
                            + location + " and may look like:\n" + sw.toString();

                    reportError(env, message, element);
                }
            }
            reportError(env, e.getMessage(), e.getElement(), e.getAnnotation(), e.getAnnotationValue());
        } catch (final Throwable e) {
            final String message =
                    "There was an unexpected error running the " + getClass().getName() + " processor. This has "
                            + "resulted in a failure to process the code and has left the compiler in an invalid "
                            + "state. If you believe this is an error with the "
                            + getClass().getName()
                            + " processor then please report the failure to the developers so that it can be fixed.\n"
                            + " Report the error at: "
                            + getIssueTrackerURL() + "\n" + "\n\n"
                            + printStackTrace(e);
            reportError(env, message, element);
        }
    }

    @Nonnull
    private String printStackTrace(@Nonnull final Throwable e) {
        final StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        sw.flush();
        return sw.toString();
    }

    @Nonnull
    private Collection<TypeElement> deriveElementsToProcess(
            @Nonnull final DeferredElementSet deferredSet,
            @Nonnull final Collection<TypeElement> elements,
            @Nonnull final Predicate<TypeElement> isValidPredicate) {
        if (_profile) {
            _extractDeferredStopWatch.start();
        }
        final List<TypeElement> deferred = deferredSet.extractDeferred(processingEnv);
        if (_profile) {
            _extractDeferredStopWatch.stop();
        }
        final List<TypeElement> elementsToProcess = new ArrayList<>();
        collectElementsToProcess(elements, deferredSet, elementsToProcess, isValidPredicate);
        final int scheduledFromThisRound = elementsToProcess.size();
        final int deferredFromThisRound = deferredSet.getDeferred().size();
        debug(() -> scheduledFromThisRound + " elements from this round scheduled for processing, "
                + deferredFromThisRound + " elements from this round deferred for processing in a later round");
        collectElementsToProcess(deferred, deferredSet, elementsToProcess, isValidPredicate);
        final int scheduledFromPreviousRounds = elementsToProcess.size() - scheduledFromThisRound;
        final int deferredFromPreviousRounds = deferredSet.getDeferred().size() - deferredFromThisRound;
        debug(() -> scheduledFromPreviousRounds + " elements from previous rounds scheduled for processing, "
                + deferredFromPreviousRounds
                + " elements from previous rounds deferred for processing " + "in a later round");

        return elementsToProcess;
    }

    private void collectElementsToProcess(
            @Nonnull final Collection<TypeElement> elements,
            @Nonnull final DeferredElementSet deferredSet,
            @Nonnull final List<TypeElement> elementsToProcess,
            @Nonnull final Predicate<TypeElement> isValidPredicate) {
        for (final TypeElement element : elements) {
            if (_profile) {
                _validateElementStopWatch.start();
            }
            final boolean valid = isValidPredicate.test(element);
            if (_profile) {
                _validateElementStopWatch.stop();
            }
            if (valid) {
                debug(() -> "Scheduling element " + element + " for processing");
                elementsToProcess.add(element);
            } else {
                debug(() -> "Deferring element " + element + " for processing in a later "
                        + "round as it failed superficial validation");
                deferredSet.deferElement(element);
            }
        }
    }

    protected final boolean isProfileEnabled() {
        return _profile;
    }

    protected final void warning(@Nonnull final CharSequence message, @Nullable final Element element) {
        processingEnv.getMessager().printMessage(warningKind(), message, element);
    }

    protected final void warning(
            @Nonnull final CharSequence message,
            @Nullable final Element element,
            @Nullable final AnnotationMirror annotationMirror) {
        processingEnv.getMessager().printMessage(warningKind(), message, element, annotationMirror);
    }

    protected final void warning(
            @Nonnull final CharSequence message,
            @Nullable final Element element,
            @Nullable final AnnotationMirror annotationMirror,
            @Nullable final AnnotationValue annotationValue) {
        processingEnv.getMessager().printMessage(warningKind(), message, element, annotationMirror, annotationValue);
    }

    @Nonnull
    protected final Diagnostic.Kind warningKind() {
        return isWarningsAsErrorsEnabled() ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING;
    }

    protected final boolean isWarningsAsErrorsEnabled() {
        return _warningsAsErrors;
    }

    protected final boolean isDebugEnabled() {
        return _debug;
    }

    protected final void debug(@Nonnull final Supplier<String> messageSupplier) {
        if (isDebugEnabled()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, messageSupplier.get());
        }
    }

    protected final void emitTypeSpec(@Nonnull final String packageName, @Nonnull final TypeSpec typeSpec)
            throws IOException {
        if (_profile) {
            _emitJavaTypeStopWatch.start();
        }
        final JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
                .skipJavaLangImports(true)
                .build();
        if (_formatGeneratedSource) {
            writeFormattedJavaFile(javaFile, formatSource(javaFile));
        } else {
            javaFile.writeTo(processingEnv.getFiler());
        }
        if (_profile) {
            _emitJavaTypeStopWatch.stop();
        }
    }

    @Nonnull
    private String formatSource(@Nonnull final JavaFile javaFile) throws IOException {
        try {
            final FormatterProxy formatter = formatter();
            return (String) formatter.formatSourceMethod().invoke(formatter.formatter(), javaFile.toString());
        } catch (final ClassNotFoundException e) {
            throw newFormatterFailure("locate source formatter", e);
        } catch (final InvocationTargetException e) {
            final Throwable cause = e.getCause();
            throw newFormatterFailure("format generated source", null == cause ? e : cause);
        } catch (final ReflectiveOperationException | LinkageError e) {
            throw newFormatterFailure("format generated source", e);
        }
    }

    @Nonnull
    private FormatterProxy formatter() throws ClassNotFoundException, ReflectiveOperationException {
        if (null == _formatter) {
            _formatter = createFormatter();
        }
        return _formatter;
    }

    @Nonnull
    private FormatterProxy createFormatter() throws ClassNotFoundException, ReflectiveOperationException {
        try {
            return createFormatter(ORIGINAL_FORMATTER_CLASSNAME);
        } catch (final ClassNotFoundException originalNotFound) {
            try {
                return createFormatter(getVendorFormatterClassname());
            } catch (final ClassNotFoundException vendorNotFound) {
                vendorNotFound.addSuppressed(originalNotFound);
                throw vendorNotFound;
            }
        }
    }

    @Nonnull
    private FormatterProxy createFormatter(@Nonnull final String formatterClassName)
            throws ClassNotFoundException, ReflectiveOperationException {
        final ClassLoader classLoader = AbstractStandardProcessor.class.getClassLoader();
        final Class<?> formatterClass = Class.forName(formatterClassName, true, classLoader);
        final Object formatter = formatterClass.getMethod("create").invoke(null);
        return new FormatterProxy(formatter, formatterClass.getMethod("formatSource", String.class));
    }

    @Nonnull
    private IOException newFormatterFailure(@Nonnull final String action, @Nonnull final Throwable cause) {
        return new IOException(
                "Unable to " + action + " while " + getOptionPrefix() + ".format_generated_source=true. "
                        + "Proton attempted to load source formatter classes "
                        + ORIGINAL_FORMATTER_CLASSNAME + " and " + getVendorFormatterClassname()
                        + ". If these classes are missing, ensure the annotation processor path contains"
                        + " palantir-java-format or a processor jar that includes Proton's bundled vendor formatter"
                        + " classes. Downstream processors that shade Proton must include Proton's bundled"
                        + " org.realityforge.proton.vendor classes in the processor jar. On JDK 16+ the formatter"
                        + " requires the JVM running javac and annotation processing to receive these module exports: "
                        + String.join(", ", FORMATTER_JDK_EXPORTS) + ".",
                cause);
    }

    @Nonnull
    private String getVendorFormatterClassname() {
        return AbstractStandardProcessor.class.getPackageName() + ".vendor.javaformat.java.Formatter";
    }

    private void writeFormattedJavaFile(@Nonnull final JavaFile javaFile, @Nonnull final String formattedSource)
            throws IOException {
        final TypeSpec typeSpec = javaFile.typeSpec();
        final String packageName = javaFile.packageName();
        final String fileName = packageName.isEmpty() ? typeSpec.name() : packageName + "." + typeSpec.name();
        final JavaFileObject sourceFile = processingEnv
                .getFiler()
                .createSourceFile(fileName, typeSpec.originatingElements().toArray(new Element[0]));
        try (final Writer writer = sourceFile.openWriter()) {
            writer.write(formattedSource);
        } catch (final Exception e) {
            try {
                sourceFile.delete();
            } catch (final Exception ignored) {
            }
            throw e;
        }
    }

    protected final boolean readBooleanOption(@Nonnull final String relativeKey, final boolean defaultValue) {
        final String optionValue = processingEnv.getOptions().get(getOptionPrefix() + "." + relativeKey);
        return null == optionValue ? defaultValue : "true".equals(optionValue);
    }
}
