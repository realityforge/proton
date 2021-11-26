package org.realityforge.proton;

import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
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
  /**
   * Names of types that have been passed to the processor in the current round or earlier rounds.
   * The set is used to restrict which types the processor will return in <code>getNewTypeElementsToProcess()</code>.
   * This is cleared in when processingOver() returns true.
   */
  @Nonnull
  private final Set<String> _rootTypeNames = new HashSet<>();
  private boolean _verboseOutOfRoundErrors;
  private boolean _deferErrors;
  private boolean _deferUnresolved;
  private boolean _debug;

  @FunctionalInterface
  public interface Action<E extends Element>
  {
    void process( @Nonnull E element )
      throws Exception;
  }

  private int _invalidTypeCount;

  @Override
  public synchronized void init( final ProcessingEnvironment processingEnv )
  {
    super.init( processingEnv );
    _verboseOutOfRoundErrors = readBooleanOption( "verbose_out_of_round.errors", true );
    _deferErrors = readBooleanOption( "defer.errors", true );
    _deferUnresolved = readBooleanOption( "defer.unresolved", true );
    _debug = readBooleanOption( "debug", false );
  }

  protected final void debugAnnotationProcessingRootElements( @Nonnull final RoundEnvironment env )
  {
    if ( isDebugEnabled() )
    {
      for ( final Element element : env.getRootElements() )
      {
        if ( element instanceof TypeElement )
        {
          debug( () -> "Annotation processing root element " + ( (TypeElement) element ).getQualifiedName() );
        }
      }
    }
  }

  protected final void processTypeElements( @Nonnull final Set<? extends TypeElement> annotations,
                                            @Nonnull final RoundEnvironment env,
                                            @Nonnull final String annotationClassname,
                                            @Nonnull final DeferredElementSet deferredTypes,
                                            @Nonnull final String label,
                                            @Nonnull final Action<TypeElement> action )
  {
    final Collection<TypeElement> newElementsToProcess =
      getNewTypeElementsToProcess( annotations, env, annotationClassname );
    if ( !deferredTypes.getDeferred().isEmpty() || !newElementsToProcess.isEmpty() )
    {
      processTypeElements( env, deferredTypes, newElementsToProcess, label, action );
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
  @SuppressWarnings( "unchecked" )
  protected final Collection<TypeElement> getNewTypeElementsToProcess( @Nonnull final Set<? extends TypeElement> annotations,
                                                                       @Nonnull final RoundEnvironment env,
                                                                       @Nonnull final String annotationClassname )
  {
    return annotations
      .stream()
      .filter( a -> a.getQualifiedName().toString().equals( annotationClassname ) )
      .findAny()
      .map( a -> (Collection<TypeElement>) env.getElementsAnnotatedWith( a ) )
      .map( elements -> elements
        .stream()
        .filter( e -> isRootType( (TypeElement) ElementsUtil.getTopLevelElement( e ) ) )
        .collect( Collectors.toList() ) )
      .orElse( Collections.emptyList() );
  }

  private boolean isRootType( @Nonnull final TypeElement typeElement )
  {
    return _rootTypeNames.contains( typeElement.getQualifiedName().toString() );
  }

  private void processTypeElements( @Nonnull final RoundEnvironment env,
                                    @Nonnull final DeferredElementSet deferredSet,
                                    @Nonnull final Collection<TypeElement> elements,
                                    @Nonnull final String label,
                                    @Nonnull final Action<TypeElement> action )
  {
    if ( shouldDeferUnresolved() )
    {
      final Collection<TypeElement> elementsToProcess = deriveElementsToProcess( deferredSet, elements );
      doProcessTypeElements( env, elementsToProcess, label, action );
      errorIfProcessingOverAndDeferredTypesUnprocessed( env, deferredSet );
    }
    else
    {
      doProcessTypeElements( env, new ArrayList<>( elements ), label, action );
    }
  }

  private void errorIfProcessingOverAndDeferredTypesUnprocessed( @Nonnull final RoundEnvironment env,
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

  protected final void collectRootTypeNames( @Nonnull final RoundEnvironment env )
  {
    for ( final Element element : env.getRootElements() )
    {
      if ( element instanceof TypeElement )
      {
        _rootTypeNames.add( ( (TypeElement) element ).getQualifiedName().toString() );
      }
    }
  }

  protected final void clearRootTypeNamesIfProcessingOver( @Nonnull final RoundEnvironment env )
  {
    if ( env.processingOver() )
    {
      _rootTypeNames.clear();
    }
  }

  protected boolean shouldDeferUnresolved()
  {
    return _deferUnresolved;
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
    _invalidTypeCount++;
    final Diagnostic.Kind kind =
      !_deferErrors || env.errorRaised() || env.processingOver() ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING;
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
                                      @Nonnull final String label,
                                      @Nonnull final Action<TypeElement> action )
  {
    for ( final TypeElement element : elements )
    {
      performAction( env, label, action, element );
    }
  }

  protected final <E extends Element> void performAction( @Nonnull final RoundEnvironment env,
                                                          @Nonnull final String label,
                                                          @Nonnull final Action<E> action,
                                                          @Nonnull final E element )
  {
    debug( () -> "Performing '" + label + "' action on element " + element );
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
      if ( _verboseOutOfRoundErrors )
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
        "There was an unexpected error running the " + getClass().getName() + " processor. This has " +
        "resulted in a failure to process the code and has left the compiler in an invalid " +
        "state. If you believe this is an error with the " + getClass().getName() +
        " processor then please report the failure to the developers so that it can be fixed.\n" +
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
    final int scheduledFromThisRound = elementsToProcess.size();
    final int deferredFromThisRound = deferredSet.getDeferred().size();
    debug( () -> scheduledFromThisRound + " elements from this round scheduled for processing, " +
                 deferredFromThisRound + " elements from this round deferred for processing in a later round" );
    collectElementsToProcess( deferred, deferredSet, elementsToProcess );
    final int scheduledFromPreviousRounds = elementsToProcess.size() - scheduledFromThisRound;
    final int deferredFromPreviousRounds = deferredSet.getDeferred().size() - deferredFromThisRound;
    debug( () -> scheduledFromPreviousRounds + " elements from previous rounds scheduled for processing, " +
                 deferredFromPreviousRounds + " elements from previous rounds deferred for processing " +
                 "in a later round" );

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
        debug( () -> "Scheduling element " + element + " for processing" );
        elementsToProcess.add( element );
      }
      else
      {
        debug( () -> "Deferring element " + element + " for processing in a later " +
                     "round as it failed superficial validation" );
        deferredSet.deferElement( element );
      }
    }
  }

  protected final boolean isDebugEnabled()
  {
    return _debug;
  }

  protected final void debug( @Nonnull final Supplier<String> messageSupplier )
  {
    if ( isDebugEnabled() )
    {
      processingEnv.getMessager().printMessage( Diagnostic.Kind.NOTE, messageSupplier.get() );
    }
  }

  protected final void emitTypeSpec( @Nonnull final String packageName, @Nonnull final TypeSpec typeSpec )
    throws IOException
  {
    GeneratorUtil.emitJavaType( packageName, typeSpec, processingEnv.getFiler() );
  }

  protected final boolean readBooleanOption( @Nonnull final String relativeKey, final boolean defaultValue )
  {
    final String optionValue = processingEnv.getOptions().get( getOptionPrefix() + "." + relativeKey );
    return null == optionValue ? defaultValue : "true".equals( optionValue );
  }
}
