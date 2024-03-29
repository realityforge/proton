package org.realityforge.proton;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

@SuppressWarnings( { "SameParameterValue", "WeakerAccess", "unused" } )
public final class MemberChecks
{
  private MemberChecks()
  {
  }

  /**
   * Verifies that the method is not final, static, abstract or private.
   * The intent is to verify that it can be overridden and wrapped in a sub-class in the same package.
   */
  public static void mustBeWrappable( @Nonnull final TypeElement targetType,
                                      @Nonnull final String scopeAnnotationName,
                                      @Nonnull final String annotationName,
                                      @Nonnull final Element element )
    throws ProcessorException
  {
    mustBeOverridable( targetType, scopeAnnotationName, annotationName, element );
    mustNotBeAbstract( annotationName, element );
  }

  /**
   * Verifies that the method is not final, static or abstract.
   * The intent is to verify that it can be overridden in sub-class in the same package.
   */
  public static void mustBeOverridable( @Nonnull final TypeElement targetType,
                                        @Nonnull final String scopeAnnotationName,
                                        @Nonnull final String annotationName,
                                        @Nonnull final Element element )
    throws ProcessorException
  {
    mustNotBeFinal( annotationName, element );
    mustBeSubclassCallable( targetType, scopeAnnotationName, annotationName, element );
  }

  /**
   * Verifies that the method is not static, abstract or private.
   * The intent is to verify that it can be instance called by sub-class in the same package as the targetType.
   */
  public static void mustBeSubclassCallable( @Nonnull final TypeElement targetType,
                                             @Nonnull final String scopeAnnotationName,
                                             @Nonnull final String annotationName,
                                             @Nonnull final Element element )
    throws ProcessorException
  {
    mustNotBeStatic( annotationName, element );
    mustNotBePrivate( annotationName, element );
    mustNotBePackageAccessInDifferentPackage( targetType, scopeAnnotationName, annotationName, element );
  }

  public static void mustBeStaticallySubclassCallable( @Nonnull final TypeElement targetType,
                                                       @Nonnull final String scopeAnnotationName,
                                                       @Nonnull final String annotationName,
                                                       @Nonnull final Element method )
    throws ProcessorException
  {
    mustBeStatic( annotationName, method );
    mustNotBePrivate( annotationName, method );
    mustNotBePackageAccessInDifferentPackage( targetType, scopeAnnotationName, annotationName, method );
  }

  /**
   * Verifies that the method follows conventions of a lifecycle hook.
   * The intent is to verify that it can be instance called by sub-class in same
   * package at a lifecycle stage. It should not raise errors, return values or accept
   * parameters.
   *
   * @param targetType          the type being processed.
   * @param scopeAnnotationName the name of the scoping annotation name.
   * @param annotationName      the annotation checking.
   * @param method              the method to check.
   * @throws ProcessorException if verification fails.
   */
  public static void mustBeLifecycleHook( @Nonnull final TypeElement targetType,
                                          @Nonnull final String scopeAnnotationName,
                                          @Nonnull final String annotationName,
                                          @Nonnull final ExecutableElement method )
    throws ProcessorException
  {
    mustNotBeAbstract( annotationName, method );
    mustBeSubclassCallable( targetType, scopeAnnotationName, annotationName, method );
    mustNotHaveAnyParameters( annotationName, method );
    mustNotReturnAnyValue( annotationName, method );
    mustNotThrowAnyExceptions( annotationName, method );
  }

  public static void mustBeStatic( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ProcessorException
  {
    if ( !element.getModifiers().contains( Modifier.STATIC ) )
    {
      throw new ProcessorException( must( annotationName, "be static" ), element );
    }
  }

  public static void mustNotBeStatic( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ProcessorException
  {
    if ( element.getModifiers().contains( Modifier.STATIC ) )
    {
      throw new ProcessorException( mustNot( annotationName, "be static" ), element );
    }
  }

  public static void mustBeAbstract( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ProcessorException
  {
    if ( !element.getModifiers().contains( Modifier.ABSTRACT ) )
    {
      throw new ProcessorException( must( annotationName, "be abstract" ), element );
    }
  }

  public static void mustNotBeAbstract( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ProcessorException
  {
    if ( element.getModifiers().contains( Modifier.ABSTRACT ) )
    {
      throw new ProcessorException( mustNot( annotationName, "be abstract" ), element );
    }
  }

  public static void mustBeFinal( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ProcessorException
  {
    if ( !element.getModifiers().contains( Modifier.FINAL ) )
    {
      throw new ProcessorException( must( annotationName, "be final" ), element );
    }
  }

  public static void mustNotBeFinal( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ProcessorException
  {
    if ( element.getModifiers().contains( Modifier.FINAL ) )
    {
      throw new ProcessorException( mustNot( annotationName, "be final" ), element );
    }
  }

  public static void mustBePublic( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ProcessorException
  {
    if ( !element.getModifiers().contains( Modifier.PUBLIC ) )
    {
      throw new ProcessorException( must( annotationName, "be public" ), element );
    }
  }

  public static void mustNotBePublic( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ProcessorException
  {
    if ( element.getModifiers().contains( Modifier.PUBLIC ) )
    {
      throw new ProcessorException( mustNot( annotationName, "be public" ), element );
    }
  }

  public static void mustBeProtected( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ProcessorException
  {
    if ( !element.getModifiers().contains( Modifier.PROTECTED ) )
    {
      throw new ProcessorException( must( annotationName, "be protected" ), element );
    }
  }

  public static void mustNotBeProtected( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ProcessorException
  {
    if ( element.getModifiers().contains( Modifier.PROTECTED ) )
    {
      throw new ProcessorException( mustNot( annotationName, "be protected" ), element );
    }
  }

  public static void mustBePrivate( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ProcessorException
  {
    if ( !element.getModifiers().contains( Modifier.PRIVATE ) )
    {
      throw new ProcessorException( must( annotationName, "be private" ), element );
    }
  }

  public static void mustNotBePrivate( @Nonnull final String annotationName, @Nonnull final Element element )
    throws ProcessorException
  {
    if ( element.getModifiers().contains( Modifier.PRIVATE ) )
    {
      throw new ProcessorException( mustNot( annotationName, "be private" ), element );
    }
  }

  public static void mustNotBePackageAccessInDifferentPackage( @Nonnull final TypeElement element,
                                                               @Nonnull final String scopeAnnotationName,
                                                               @Nonnull final String annotationName,
                                                               @Nonnull final Element other )
    throws ProcessorException
  {
    final Set<Modifier> modifiers = other.getModifiers();
    final boolean isPackageAccess =
      !modifiers.contains( Modifier.PRIVATE ) &&
      !modifiers.contains( Modifier.PROTECTED ) &&
      !modifiers.contains( Modifier.PUBLIC );

    if ( isPackageAccess )
    {
      if ( ElementsUtil.areTypesInDifferentPackage( element, (TypeElement) other.getEnclosingElement() ) )
      {
        throw new ProcessorException( mustNot( annotationName,
                                               "be package access if the " +
                                               ( ElementKind.METHOD == other.getKind() ? "method" : "field" ) +
                                               " is in a different package from the type annotated with the " +
                                               toSimpleName( scopeAnnotationName ) + " annotation" ),
                                      other );
      }
    }
  }

  public static void mustNotHaveAnyParameters( @Nonnull final String annotationName,
                                               @Nonnull final ExecutableElement method )
    throws ProcessorException
  {
    if ( !method.getParameters().isEmpty() )
    {
      throw new ProcessorException( mustNot( annotationName, "have any parameters" ), method );
    }
  }

  public static void mustNotHaveAnyTypeParameters( @Nonnull final String annotationName,
                                                   @Nonnull final ExecutableElement method )
    throws ProcessorException
  {
    if ( !method.getTypeParameters().isEmpty() )
    {
      throw new ProcessorException( mustNot( annotationName, "have any type parameters" ), method );
    }
  }

  public static void mustNotReturnAnyValue( @Nonnull final String annotationName,
                                            @Nonnull final ExecutableElement method )
    throws ProcessorException
  {
    if ( TypeKind.VOID != method.getReturnType().getKind() )
    {
      throw new ProcessorException( mustNot( annotationName, "return a value" ), method );
    }
  }

  public static void mustReturnAValue( @Nonnull final String annotationName, @Nonnull final ExecutableElement method )
    throws ProcessorException
  {
    if ( TypeKind.VOID == method.getReturnType().getKind() )
    {
      throw new ProcessorException( must( annotationName, "return a value" ), method );
    }
  }

  public static void mustNotThrowAnyExceptions( @Nonnull final String annotationName,
                                                @Nonnull final ExecutableElement method )
    throws ProcessorException
  {
    if ( !method.getThrownTypes().isEmpty() )
    {
      throw new ProcessorException( mustNot( annotationName, "throw any exceptions" ), method );
    }
  }

  /**
   * Ensure that the element is not annotated with multiple annotations from the specified set.
   * The exceptions map exists to allow exceptions to this rule.
   *
   * @param element     the element to check.
   * @param annotations the set of annotation names that must not overlap.
   * @param exceptions  the annotations names that are allowed to overlap.
   */
  public static void verifyNoOverlappingAnnotations( @Nonnull final Element element,
                                                     @Nonnull final Collection<String> annotations,
                                                     @Nonnull final Map<String, Collection<String>> exceptions )
    throws ProcessorException
  {
    final String[] annotationTypes = annotations.toArray( new String[ 0 ] );

    for ( int i = 0; i < annotationTypes.length; i++ )
    {
      final String type1 = annotationTypes[ i ];
      final Object annotation1 = AnnotationsUtil.findAnnotationByType( element, type1 );
      if ( null != annotation1 )
      {
        for ( int j = i + 1; j < annotationTypes.length; j++ )
        {
          final String type2 = annotationTypes[ j ];
          if ( !isException( exceptions, type1, type2 ) )
          {
            final Object annotation2 = AnnotationsUtil.findAnnotationByType( element, type2 );
            if ( null != annotation2 )
            {
              final String message =
                "Method can not be annotated with both " + toSimpleName( type1 ) + " and " + toSimpleName( type2 );
              throw new ProcessorException( message, element );
            }
          }
        }
      }
    }
  }

  private static boolean isException( @Nonnull final Map<String, Collection<String>> exceptions,
                                      @Nonnull final String type1,
                                      @Nonnull final String type2 )
  {
    return ( exceptions.containsKey( type1 ) && exceptions.get( type1 ).contains( type2 ) ) ||
           exceptions.containsKey( type2 ) && exceptions.get( type2 ).contains( type1 );
  }

  @Nonnull
  public static String must( @Nonnull final String annotationName, @Nonnull final String message )
  {
    return toSimpleName( annotationName ) + " target must " + message;
  }

  @Nonnull
  public static String mustNot( @Nonnull final String annotationName, @Nonnull final String message )
  {
    return must( annotationName, "not " + message );
  }

  @Nonnull
  public static String should( @Nonnull final String annotationName, @Nonnull final String message )
  {
    return toSimpleName( annotationName ) + " target should " + message;
  }

  @Nonnull
  public static String shouldNot( @Nonnull final String annotationName, @Nonnull final String message )
  {
    return should( annotationName, "not " + message );
  }

  @Nonnull
  public static String toSimpleName( @Nonnull final String annotationName )
  {
    return "@" + annotationName.replaceAll( ".*\\.", "" );
  }

  @Nonnull
  public static String suppressedBy( @Nonnull final String warning )
  {
    return suppressedBy( warning, null );
  }

  @Nonnull
  public static String suppressedBy( @Nonnull final String warning,
                                     @Nullable final String alternativeSuppressWarnings )
  {
    return "This warning can be suppressed by annotating the element with " +
           "@SuppressWarnings( \"" + warning + "\" )" +
           ( null == alternativeSuppressWarnings ?
             "" :
             " or " + toSimpleName( alternativeSuppressWarnings ) + "( \"" + warning + "\" )" );

  }

  public static void shouldNotBePublic( @Nonnull final ProcessingEnvironment processingEnv,
                                        @Nonnull final ExecutableElement method,
                                        @Nonnull final String annotationName,
                                        @Nonnull final String warning )
  {
    shouldNotBePublic( processingEnv, method, annotationName, warning, null );
  }

  public static void shouldNotBePublic( @Nonnull final ProcessingEnvironment processingEnv,
                                        @Nonnull final ExecutableElement method,
                                        @Nonnull final String annotationName,
                                        @Nonnull final String warning,
                                        @Nullable final String alternativeSuppressWarnings )
  {
    if ( method.getModifiers().contains( Modifier.PUBLIC ) &&
         ElementsUtil.isWarningNotSuppressed( method, warning, alternativeSuppressWarnings ) )
    {
      final String message =
        shouldNot( annotationName, "be public. " + suppressedBy( warning, alternativeSuppressWarnings ) );
      processingEnv.getMessager().printMessage( Diagnostic.Kind.WARNING, message, method );
    }
  }

  public static void shouldNotBeProtected( @Nonnull final ProcessingEnvironment processingEnv,
                                           @Nonnull final ExecutableElement method,
                                           @Nonnull final String annotationName,
                                           @Nonnull final String warning )
  {
    shouldNotBeProtected( processingEnv, method, annotationName, warning, null );
  }

  public static void shouldNotBeProtected( @Nonnull final ProcessingEnvironment processingEnv,
                                           @Nonnull final ExecutableElement method,
                                           @Nonnull final String annotationName,
                                           @Nonnull final String warning,
                                           @Nullable final String alternativeSuppressWarnings )
  {
    if ( method.getModifiers().contains( Modifier.PROTECTED ) &&
         ElementsUtil.isWarningNotSuppressed( method, warning, alternativeSuppressWarnings ) )
    {
      final String message =
        shouldNot( annotationName, "be protected. " + suppressedBy( warning, alternativeSuppressWarnings ) );
      processingEnv.getMessager().printMessage( Diagnostic.Kind.WARNING, message, method );
    }
  }

  public static void mustReturnAnInstanceOf( @Nonnull final ProcessingEnvironment processingEnv,
                                             @Nonnull final ExecutableElement method,
                                             @Nonnull final String annotationClassname,
                                             @Nonnull final String expectedTypename )
  {
    final TypeElement typeElement = processingEnv.getElementUtils().getTypeElement( expectedTypename );
    assert null != typeElement;
    mustReturnAnInstanceOf( processingEnv, method, annotationClassname, typeElement.asType() );
  }

  public static void mustReturnAnInstanceOf( @Nonnull final ProcessingEnvironment processingEnv,
                                             @Nonnull final ExecutableElement method,
                                             @Nonnull final String annotationClassname,
                                             @Nonnull final TypeMirror expectedType )
  {
    final TypeMirror actual = method.getReturnType();
    if ( !processingEnv.getTypeUtils().isSameType( actual, expectedType ) )
    {
      final String message = must( annotationClassname, "return an instance of " + expectedType );
      throw new ProcessorException( message, method );
    }
  }

  public static void shouldBeInternalMethod( @Nonnull final ProcessingEnvironment processingEnv,
                                             @Nonnull final TypeElement typeElement,
                                             @Nonnull final ExecutableElement method,
                                             @Nonnull final String annotationClassname,
                                             @Nonnull final String publicWarning,
                                             @Nonnull final String protectedWarning )
  {
    shouldBeInternalMethod( processingEnv,
                            typeElement,
                            method,
                            annotationClassname,
                            publicWarning,
                            protectedWarning,
                            null );
  }

  public static void shouldBeInternalMethod( @Nonnull final ProcessingEnvironment processingEnv,
                                             @Nonnull final TypeElement typeElement,
                                             @Nonnull final ExecutableElement method,
                                             @Nonnull final String annotationClassname,
                                             @Nonnull final String publicWarning,
                                             @Nonnull final String protectedWarning,
                                             @Nullable final String alternativeSuppressWarnings )
  {
    if ( doesMethodNotOverrideInterfaceMethod( processingEnv, typeElement, method ) )
    {
      shouldNotBePublic( processingEnv,
                         method,
                         annotationClassname,
                         publicWarning,
                         alternativeSuppressWarnings );
    }
    if ( Objects.equals( typeElement, method.getEnclosingElement() ) )
    {
      shouldNotBeProtected( processingEnv,
                            method,
                            annotationClassname,
                            protectedWarning,
                            alternativeSuppressWarnings );
    }
  }

  public static boolean doesMethodNotOverrideInterfaceMethod( @Nonnull final ProcessingEnvironment processingEnv,
                                                              @Nonnull final TypeElement typeElement,
                                                              @Nonnull final ExecutableElement method )
  {
    return !ElementsUtil.doesMethodOverrideInterfaceMethod( processingEnv.getTypeUtils(), typeElement, method );
  }
}
