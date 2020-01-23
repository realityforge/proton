package org.realityforge.proton;

import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
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

  @Nonnull
  private Set<TypeElement> _deferred = new HashSet<>();
  private int _invalidTypeCount;

  protected final void processTypeElements( @Nonnull final RoundEnvironment env,
                                            @Nonnull final Collection<TypeElement> elements,
                                            @Nonnull final Action<TypeElement> action )
  {
    if ( shouldDeferUnresolved() )
    {
      final Collection<TypeElement> elementsToProcess = deriveElementsToProcess( elements );
      doProcessTypeElements( env, elementsToProcess, action );
      if ( env.getRootElements().isEmpty() && !_deferred.isEmpty() )
      {
        _deferred.forEach( e -> processingErrorMessage( env, e ) );
        _deferred.clear();
      }
    }
    else
    {
      doProcessTypeElements( env, new ArrayList<>( elements ), action );
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
    final String deferErrorsValue = processingEnv.getOptions().get( getOptionPrefix() + ".defer.errors" );
    final boolean deferErrors = null == deferErrorsValue || "true".equals( deferErrorsValue );
    _invalidTypeCount++;
    if ( !deferErrors || env.errorRaised() || env.processingOver() )
    {
      processingEnv.getMessager().printMessage( Diagnostic.Kind.ERROR, message, element );
    }
    else
    {
      processingEnv.getMessager().printMessage( Diagnostic.Kind.WARNING, message, element );
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
      reportError( env, e.getMessage(), e.getElement() );
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
  private Collection<TypeElement> deriveElementsToProcess( @Nonnull final Collection<TypeElement> elements )
  {
    final List<TypeElement> deferred = _deferred
      .stream()
      .map( e -> processingEnv.getElementUtils().getTypeElement( e.getQualifiedName() ) )
      .collect( Collectors.toList() );
    _deferred = new HashSet<>();

    final List<TypeElement> elementsToProcess = new ArrayList<>();
    collectElementsToProcess( elements, elementsToProcess );
    collectElementsToProcess( deferred, elementsToProcess );
    return elementsToProcess;
  }

  private void collectElementsToProcess( @Nonnull final Collection<TypeElement> elements,
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
        deferElement( element );
      }
    }
  }

  protected final void deferElement( @Nonnull final TypeElement element )
  {
    _deferred.add( element );
  }

  protected final void emitTypeSpec( @Nonnull final String packageName, @Nonnull final TypeSpec typeSpec )
    throws IOException
  {
    GeneratorUtil.emitJavaType( packageName, typeSpec, processingEnv.getFiler() );
  }
}
