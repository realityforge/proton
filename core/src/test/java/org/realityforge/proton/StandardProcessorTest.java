package org.realityforge.proton;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import org.testng.annotations.Test;

public final class StandardProcessorTest {
    @Test
    public void getSupportedOptionsAddsPrefixedCommonOptions() {
        final Processor processor = new Processor();
        final Set<String> options = processor.getSupportedOptions();

        assertTrue(options.contains("test.verbose_out_of_round.errors"));
        assertTrue(options.contains("test.defer.errors"));
        assertTrue(options.contains("test.defer.unresolved"));
        assertTrue(options.contains("test.debug"));
        assertTrue(options.contains("test.profile"));
        assertTrue(options.contains("test.warnings_as_errors"));
        assertTrue(options.contains("test.format_generated_source"));
    }

    @Test
    public void initReadsDefaultBooleanOptions() {
        final Processor processor = new Processor();

        processor.init(processingEnvironment(Map.of(), new CapturingMessager()));

        assertTrue(processor.shouldDeferUnresolvedValue());
        assertFalse(processor.isDebugEnabledValue());
        assertFalse(processor.isProfileEnabledValue());
        assertFalse(processor.isWarningsAsErrorsEnabledValue());
        assertEquals(processor.warningKindValue(), Diagnostic.Kind.WARNING);
    }

    @Test
    public void initReadsConfiguredBooleanOptions() {
        final Processor processor = new Processor();

        processor.init(processingEnvironment(
                Map.of(
                        "test.defer.unresolved",
                        "false",
                        "test.debug",
                        "true",
                        "test.profile",
                        "true",
                        "test.warnings_as_errors",
                        "true",
                        "test.custom",
                        "true",
                        "test.false_value",
                        "yes"),
                new CapturingMessager()));

        assertFalse(processor.shouldDeferUnresolvedValue());
        assertTrue(processor.isDebugEnabledValue());
        assertTrue(processor.isProfileEnabledValue());
        assertTrue(processor.isWarningsAsErrorsEnabledValue());
        assertEquals(processor.warningKindValue(), Diagnostic.Kind.ERROR);
        assertTrue(processor.readBooleanOptionValue("custom", false));
        assertFalse(processor.readBooleanOptionValue("false_value", true));
        assertTrue(processor.readBooleanOptionValue("missing", true));
    }

    @Test
    public void debugAndWarningUseConfiguredDiagnosticKinds() {
        final CapturingMessager messager = new CapturingMessager();
        final Processor processor = new Processor();
        final Element element = TestUtil.proxy(Element.class, (self, method, args) -> TestUtil.unsupported(method));

        processor.init(
                processingEnvironment(Map.of("test.debug", "true", "test.warnings_as_errors", "true"), messager));
        processor.debugMessage("debug message");
        processor.warningMessage("warning message", element);

        assertEquals(messager.messages().size(), 2);
        assertEquals(messager.messages().get(0).kind(), Diagnostic.Kind.NOTE);
        assertEquals(messager.messages().get(0).message(), "debug message");
        assertEquals(messager.messages().get(1).kind(), Diagnostic.Kind.ERROR);
        assertEquals(messager.messages().get(1).message(), "warning message");
        assertEquals(messager.messages().get(1).element(), element);
    }

    @Nonnull
    private static ProcessingEnvironment processingEnvironment(
            @Nonnull final Map<String, String> options, @Nonnull final Messager messager) {
        return TestUtil.proxy(ProcessingEnvironment.class, (self, method, args) -> {
            if ("getOptions".equals(method.getName())) {
                return options;
            } else if ("getMessager".equals(method.getName())) {
                return messager;
            }
            return TestUtil.unsupported(method);
        });
    }

    private static final class Processor extends AbstractStandardProcessor {
        @Override
        public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
            return false;
        }

        @Nonnull
        @Override
        protected String getIssueTrackerURL() {
            return "https://example.com/issues";
        }

        @Nonnull
        @Override
        protected String getOptionPrefix() {
            return "test";
        }

        boolean shouldDeferUnresolvedValue() {
            return shouldDeferUnresolved();
        }

        boolean isProfileEnabledValue() {
            return isProfileEnabled();
        }

        boolean isWarningsAsErrorsEnabledValue() {
            return isWarningsAsErrorsEnabled();
        }

        boolean isDebugEnabledValue() {
            return isDebugEnabled();
        }

        @Nonnull
        Diagnostic.Kind warningKindValue() {
            return warningKind();
        }

        boolean readBooleanOptionValue(@Nonnull final String relativeKey, final boolean defaultValue) {
            return readBooleanOption(relativeKey, defaultValue);
        }

        void debugMessage(@Nonnull final String message) {
            debug(() -> message);
        }

        void warningMessage(@Nonnull final String message, @Nonnull final Element element) {
            warning(message, element);
        }
    }

    private static final class CapturingMessager implements Messager {
        @Nonnull
        private final List<Message> _messages = new ArrayList<>();

        @Override
        public void printMessage(final Diagnostic.Kind kind, final CharSequence msg) {
            _messages.add(new Message(kind, msg.toString(), null));
        }

        @Override
        public void printMessage(final Diagnostic.Kind kind, final CharSequence msg, final Element e) {
            _messages.add(new Message(kind, msg.toString(), e));
        }

        @Override
        public void printMessage(
                final Diagnostic.Kind kind, final CharSequence msg, final Element e, final AnnotationMirror a) {
            _messages.add(new Message(kind, msg.toString(), e));
        }

        @Override
        public void printMessage(
                final Diagnostic.Kind kind,
                final CharSequence msg,
                final Element e,
                final AnnotationMirror a,
                final AnnotationValue v) {
            _messages.add(new Message(kind, msg.toString(), e));
        }

        @Nonnull
        List<Message> messages() {
            return _messages;
        }
    }

    private static final class Message {
        @Nonnull
        private final Diagnostic.Kind _kind;

        @Nonnull
        private final String _message;

        @Nullable
        private final Element _element;

        Message(@Nonnull final Diagnostic.Kind kind, @Nonnull final String message, @Nullable final Element element) {
            _kind = kind;
            _message = message;
            _element = element;
        }

        @Nonnull
        Diagnostic.Kind kind() {
            return _kind;
        }

        @Nonnull
        String message() {
            return _message;
        }

        @Nullable
        Element element() {
            return _element;
        }
    }
}
