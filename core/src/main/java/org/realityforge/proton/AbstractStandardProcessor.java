package org.realityforge.proton;

import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

public abstract class AbstractStandardProcessor
  extends AbstractProcessor
{
  @FunctionalInterface
  public interface Action<E extends Element>
  {
    void process( @Nonnull E element )
      throws Exception;
  }

  private int _invalidTypeCount;

  protected final void processTypeElements( @Nonnull final RoundEnvironment env,
                                            @Nonnull final DeferredElementSet deferredSet,
                                            @Nonnull final Collection<TypeElement> elements,
                                            @Nonnull final Action<TypeElement> action )
  {
    if ( shouldDeferUnresolved() )
    {
      final Collection<TypeElement> elementsToProcess = deriveElementsToProcess( deferredSet, elements );
      doProcessTypeElements( env, elementsToProcess, action );
      errorIfProcessingOverAndDeferredTypesUnprocessed( env, deferredSet );
    }
    else
    {
      doProcessTypeElements( env, new ArrayList<>( elements ), action );
    }
  }

  protected final void errorIfProcessingOverAndDeferredTypesUnprocessed( @Nonnull final RoundEnvironment env,
                                                                         @Nonnull final DeferredElementSet deferredSet )
  {
    final Set<TypeElement> deferred = deferredSet.getDeferred();
    if ( ( env.processingOver() || env.errorRaised() ) && !deferred.isEmpty() )
    {
      deferred.forEach( e -> processingErrorMessage( env, e ) );
      deferredSet.clear();
    }
  }

  protected final void errorIfProcessingOverAndInvalidTypesDetected( @Nonnull final RoundEnvironment env )
  {
    if ( env.processingOver() )
    {
      if ( 0 != _invalidTypeCount )
      {
        processingEnv
          .getMessager()
          .printMessage( Diagnostic.Kind.ERROR,
                         getClass().getSimpleName() + " failed to process " + _invalidTypeCount +
                         " types. See earlier warnings for further details." );
      }
      _invalidTypeCount = 0;
    }
  }

  protected boolean shouldDeferUnresolved()
  {
    final Map<String, String> options = processingEnv.getOptions();
    final String deferUnresolvedValue = options.get( getOptionPrefix() + ".defer.unresolved" );
    return null == deferUnresolvedValue || "true".equals( deferUnresolvedValue );
  }

  @Nonnull
  protected abstract String getIssueTrackerURL();

  @Nonnull
  protected abstract String getOptionPrefix();

  private void processingErrorMessage( @Nonnull final RoundEnvironment env, @Nonnull final TypeElement target )
  {
    reportError( env,
                 getClass().getSimpleName() + " unable to process " + target.getQualifiedName() +
                 " because not all of its dependencies could be resolved. Check for " +
                 "compilation errors or a circular dependency with generated code.",
                 target );
  }

  protected final void reportError( @Nonnull final RoundEnvironment env,
                                    @Nonnull final String message,
                                    @Nullable final Element element )
  {
    reportError( env, message, element, null, null );
  }

  protected final void reportError( @Nonnull final RoundEnvironment env,
                                    @Nonnull final String message,
                                    @Nullable final Element element,
                                    @Nullable final AnnotationMirror annotation,
                                    @Nullable final AnnotationValue annotationValue )
  {
    final String deferErrorsValue = processingEnv.getOptions().get( getOptionPrefix() + ".defer.errors" );
    final boolean deferErrors = null == deferErrorsValue || "true".equals( deferErrorsValue );
    _invalidTypeCount++;
    final Diagnostic.Kind kind =
      !deferErrors || env.errorRaised() || env.processingOver() ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING;
    if ( null != annotationValue )
    {
      processingEnv.getMessager().printMessage( kind, message, element, annotation, annotationValue );
    }
    else if ( null != annotation )
    {
      processingEnv.getMessager().printMessage( kind, message, element, annotation );
    }
    else
    {
      processingEnv.getMessager().printMessage( kind, message, element );
    }
  }

  private void doProcessTypeElements( @Nonnull final RoundEnvironment env,
                                      @Nonnull final Collection<TypeElement> elements,
                                      @Nonnull final Action<TypeElement> action )
  {
    for ( final TypeElement element : elements )
    {
      performAction( env, action, element );
    }
  }

  protected final <E extends Element> void performAction( @Nonnull final RoundEnvironment env,
                                                          @Nonnull final Action<E> action,
                                                          @Nonnull final E element )
  {
    try
    {
      action.process( element );
    }
    catch ( final IOException ioe )
    {
      final String message =
        "IO error running the " + getClass().getName() + " processor. This has " +
        "resulted in a failure to process the code and has left the compiler in an invalid " +
        "state.\n" +
        "\n\n" +
        printStackTrace( ioe );
      reportError( env, message, element );
    }
    catch ( final ProcessorException e )
    {
      final Element errorLocation = e.getElement();
      final String verboseOutOfRoundErrors =
        processingEnv.getOptions().get( getOptionPrefix() + ".verbose_out_of_round.errors" );
      if ( null == verboseOutOfRoundErrors || "true".equals( verboseOutOfRoundErrors ) )
      {
        final Element outerElement = ElementsUtil.getTopLevelElement( errorLocation );
        if ( !env.getRootElements().contains( outerElement ) )
        {
          final String location;
          if ( errorLocation instanceof ExecutableElement )
          {
            final ExecutableElement executableElement = (ExecutableElement) errorLocation;
            final TypeElement typeElement = (TypeElement) executableElement.getEnclosingElement();
            location = typeElement.getQualifiedName() + "." + executableElement.getSimpleName();
          }
          else if ( errorLocation instanceof VariableElement )
          {
            final VariableElement variableElement = (VariableElement) errorLocation;
            final Element enclosingElement = variableElement.getEnclosingElement();
            if ( enclosingElement instanceof TypeElement )
            {
              final TypeElement typeElement = (TypeElement) enclosingElement;
              location = typeElement.getQualifiedName() + "." + variableElement.getSimpleName();
            }
            else
            {
              final ExecutableElement executableElement = (ExecutableElement) enclosingElement;
              final TypeElement typeElement = (TypeElement) executableElement.getEnclosingElement();
              location = typeElement.getQualifiedName() +
                         "." +
                         executableElement.getSimpleName() +
                         "(..." +
                         variableElement.getSimpleName() +
                         "...)";
            }
          }
          else
          {
            assert errorLocation instanceof TypeElement;
            final TypeElement typeElement = (TypeElement) errorLocation;
            location = typeElement.getQualifiedName().toString();
          }

          final StringWriter sw = new StringWriter();
          processingEnv.getElementUtils().printElements( sw, errorLocation );
          sw.flush();

          final String message =
            "An error was generated processing the element " + element.getSimpleName() +
            " but the error was triggered by code not currently being compiled but inherited or " +
            "implemented by the element and may not be highlighted by your tooling or IDE. The " +
            "error occurred at " + location + " and may look like:\n" + sw.toString();

          reportError( env, message, element );
        }
      }
      reportError( env, e.getMessage(), e.getElement(), e.getAnnotation(), e.getAnnotationValue() );
    }
    catch ( final Throwable e )
    {
      final String message =
        "Unexpected error running the " + getClass().getName() + " processor. This has " +
        "resulted in a failure to process the code and has left the compiler in an invalid " +
        "state. Please report the failure to the developers so that it can be fixed.\n" +
        " Report the error at: " + getIssueTrackerURL() + "\n" +
        "\n\n" +
        printStackTrace( e );
      reportError( env, message, element );
    }
  }

  @Nonnull
  private String printStackTrace( @Nonnull final Throwable e )
  {
    final StringWriter sw = new StringWriter();
    e.printStackTrace( new PrintWriter( sw ) );
    sw.flush();
    return sw.toString();
  }

  @Nonnull
  private Collection<TypeElement> deriveElementsToProcess( @Nonnull final DeferredElementSet deferredSet,
                                                           @Nonnull final Collection<TypeElement> elements )
  {
    final List<TypeElement> deferred = deferredSet.extractDeferred( processingEnv );
    final List<TypeElement> elementsToProcess = new ArrayList<>();
    collectElementsToProcess( elements, deferredSet, elementsToProcess );
    collectElementsToProcess( deferred, deferredSet, elementsToProcess );
    return elementsToProcess;
  }

  private void collectElementsToProcess( @Nonnull final Collection<TypeElement> elements,
                                         @Nonnull final DeferredElementSet deferredSet,
                                         @Nonnull final List<TypeElement> elementsToProcess )
  {
    for ( final TypeElement element : elements )
    {
      if ( SuperficialValidation.validateElement( processingEnv, element ) )
      {
        elementsToProcess.add( element );
      }
      else
      {
        deferredSet.deferElement( element );
      }
    }
  }

  protected final void emitTypeSpec( @Nonnull final String packageName, @Nonnull final TypeSpec typeSpec )
    throws IOException
  {
    GeneratorUtil.emitJavaType( packageName, typeSpec, processingEnv.getFiler() );
  }
}
