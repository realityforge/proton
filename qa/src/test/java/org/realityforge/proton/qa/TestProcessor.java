package org.realityforge.proton.qa;

import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import java.io.IOException;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import org.realityforge.proton.AbstractStandardProcessor;
import org.realityforge.proton.StopWatch;

@SupportedAnnotationTypes("org.realityforge.proton.qa.GenerateType")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedOptions("proton_test.custom_option")
public final class TestProcessor extends AbstractStandardProcessor {
    @Nonnull
    private final StopWatch _generateStopWatch = new StopWatch("Generate Test Type");

    private boolean _generated;

    @Override
    @Nonnull
    protected String getIssueTrackerURL() {
        return "https://github.com/realityforge/proton/issues";
    }

    @Override
    @Nonnull
    protected String getOptionPrefix() {
        return "proton_test";
    }

    @Override
    public boolean process(@Nonnull final Set<? extends TypeElement> annotations, @Nonnull final RoundEnvironment env) {
        if (!_generated
                && !env.processingOver()
                && !env.getElementsAnnotatedWith(GenerateType.class).isEmpty()) {
            _generated = true;
            for (final Element element : env.getElementsAnnotatedWith(GenerateType.class)) {
                performAction(env, _generateStopWatch.getName(), e -> generateType(), element, _generateStopWatch);
            }
        }
        return true;
    }

    private void generateType() throws IOException {
        emitTypeSpec(
                "com.example.generated",
                TypeSpec.classBuilder("GeneratedModel")
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addMethod(MethodSpec.methodBuilder("message")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(String.class)
                                .addStatement(
                                        "return $S + $S + $S + $S + $S + $S + $S + $S + $S",
                                        "alpha",
                                        "beta",
                                        "gamma",
                                        "delta",
                                        "epsilon",
                                        "zeta",
                                        "eta",
                                        "theta",
                                        "iota")
                                .build())
                        .build());
    }
}
