package org.realityforge.proton;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

@SuppressWarnings( { "SameParameterValue", "WeakerAccess", "unused", "RedundantSuppression" } )
public final class AnnotationsUtil
{
  private AnnotationsUtil()
  {
  }

  @SuppressWarnings( "unchecked" )
  @Nonnull
  public static List<AnnotationMirror> getRepeatingAnnotations( @Nonnull final Element typeElement,
                                                                @Nonnull final String containerClassName,
                                                                @Nonnull final String annotationClassName )
  {
    final AnnotationValue annotationValue = findAnnotationValue( typeElement, containerClassName, "value" );
    if ( null != annotationValue )
    {
      return ( (List<AnnotationValue>) annotationValue.getValue() ).stream().
        map( v -> (AnnotationMirror) v.getValue() ).collect( Collectors.toList() );
    }
    else
    {
      final AnnotationMirror annotation = findAnnotationByType( typeElement, annotationClassName );
      return null != annotation ? Collections.singletonList( annotation ) : Collections.emptyList();
    }
  }

  @SuppressWarnings( "unchecked" )
  @Nonnull
  public static List<TypeMirror> getTypeMirrorsAnnotationParameter( @Nonnull final AnnotatedConstruct annotated,
                                                                    @Nonnull final String annotationClassName,
                                                                    @Nonnull final String parameterName )
  {
    final AnnotationValue annotationValue =
      getAnnotationValue( annotated, annotationClassName, parameterName );
    return ( (List<AnnotationValue>) annotationValue.getValue() )
      .stream()
      .map( v -> (TypeMirror) v.getValue() ).collect( Collectors.toList() );
  }

  @Nonnull
  public static List<TypeElement> getTypeElementsAnnotationParameter( @Nonnull final ProcessingEnvironment processingEnv,
                                                                      @Nonnull final AnnotatedConstruct annotated,
                                                                      @Nonnull final String annotationClassName,
                                                                      @Nonnull final String parameterName )
  {
    return AnnotationsUtil.getTypeMirrorsAnnotationParameter( annotated,
                                                              annotationClassName,
                                                              parameterName ).stream().
      map( typeMirror -> (TypeElement) processingEnv.getTypeUtils().asElement( typeMirror ) ).
      collect( Collectors.toList() );
  }

  @Nonnull
  public static String getEnumAnnotationParameter( @Nonnull final AnnotatedConstruct annotated,
                                                   @Nonnull final String annotationClassname,
                                                   @Nonnull final String parameterName )
  {
    final VariableElement parameter = (VariableElement)
      getAnnotationValue( annotated, annotationClassname, parameterName ).getValue();
    return parameter.getSimpleName().toString();
  }

  @Nonnull
  public static AnnotationValue getAnnotationValue( @Nonnull final AnnotatedConstruct annotated,
                                                    @Nonnull final String annotationClassName,
                                                    @Nonnull final String parameterName )
  {
    final AnnotationValue value = findAnnotationValue( annotated, annotationClassName, parameterName );
    assert null != value;
    return value;
  }

  @Nullable
  public static AnnotationValue findAnnotationValue( @Nonnull final AnnotatedConstruct annotated,
                                                     @Nonnull final String annotationClassName,
                                                     @Nonnull final String parameterName )
  {
    final AnnotationMirror mirror = findAnnotationByType( annotated, annotationClassName );
    return null == mirror ? null : findAnnotationValue( mirror, parameterName );
  }

  @Nullable
  public static AnnotationValue findAnnotationValue( @Nonnull final AnnotationMirror annotation,
                                                     @Nonnull final String parameterName )
  {
    final Map<ExecutableElement, AnnotationValue> values = getAnnotationValuesWithDefaults( annotation );
    final ExecutableElement annotationKey = values.keySet().stream().
      filter( k -> parameterName.equals( k.getSimpleName().toString() ) ).findFirst().orElse( null );
    return values.get( annotationKey );
  }

  /**
   * Returns the {@link AnnotationMirror}'s map of {@link AnnotationValue} indexed by {@link
   * ExecutableElement}, supplying default values from the annotation if the annotation property has
   * not been set. This is equivalent to {@link
   * Elements#getElementValuesWithDefaults(AnnotationMirror)} but can be called statically without
   * an {@link Elements} instance.
   *
   * <p>The iteration order of elements of the returned map will be the order in which the {@link
   * ExecutableElement}s are defined in {@code annotation}'s {@linkplain
   * AnnotationMirror#getAnnotationType() type}.
   */
  @Nonnull
  public static Map<ExecutableElement, AnnotationValue> getAnnotationValuesWithDefaults( @Nonnull final AnnotationMirror annotation )
  {
    final Map<ExecutableElement, AnnotationValue> values = new LinkedHashMap<>();
    final Map<? extends ExecutableElement, ? extends AnnotationValue> declaredValues = annotation.getElementValues();
    final List<? extends Element> enclosedElements = annotation.getAnnotationType().asElement().getEnclosedElements();
    for ( final Element enclosedElement : enclosedElements )
    {
      if ( ElementKind.METHOD == enclosedElement.getKind() )
      {
        final ExecutableElement method = (ExecutableElement) enclosedElement;
        // Must iterate and put in this order, to ensure consistency in generated code.
        if ( declaredValues.containsKey( method ) )
        {
          values.put( method, declaredValues.get( method ) );
        }
        else
        {
          final AnnotationValue defaultValue = method.getDefaultValue();
          assert null != defaultValue;
          values.put( method, defaultValue );
        }
      }
    }
    return values;
  }

  @Nullable
  public static AnnotationValue findAnnotationValueNoDefaults( @Nonnull final AnnotationMirror annotation,
                                                               @Nonnull final String parameterName )
  {
    final Map<? extends ExecutableElement, ? extends AnnotationValue> values = annotation.getElementValues();
    final ExecutableElement annotationKey = values.keySet().stream().
      filter( k -> parameterName.equals( k.getSimpleName().toString() ) ).findFirst().orElse( null );
    return values.get( annotationKey );
  }

  @SuppressWarnings( "unchecked" )
  @Nonnull
  public static <T> T getAnnotationValue( @Nonnull final AnnotationMirror annotation,
                                          @Nonnull final String parameterName )
  {
    final AnnotationValue value = findAnnotationValue( annotation, parameterName );
    assert null != value;
    return (T) value.getValue();
  }

  @Nonnull
  public static AnnotationMirror getAnnotationByType( @Nonnull final AnnotatedConstruct annotated,
                                                      @Nonnull final String annotationClassName )
  {
    AnnotationMirror mirror = findAnnotationByType( annotated, annotationClassName );
    assert null != mirror;
    return mirror;
  }

  @Nullable
  public static AnnotationMirror findAnnotationByType( @Nonnull final AnnotatedConstruct annotated,
                                                       @Nonnull final String annotationClassName )
  {
    return
      annotated.getAnnotationMirrors().stream()
        .filter( a -> a.getAnnotationType().toString().equals( annotationClassName ) )
        .findFirst()
        .orElse( null );
  }

  public static boolean hasAnnotationOfType( @Nonnull final AnnotatedConstruct annotated,
                                             @Nonnull final String annotationClassName )
  {
    return null != findAnnotationByType( annotated, annotationClassName );
  }

  @Nonnull
  public static String extractName( @Nonnull final ExecutableElement method,
                                    @Nonnull final Function<ExecutableElement, String> defaultExtractor,
                                    @Nonnull final String annotationClassname,
                                    @Nonnull final String parameterName,
                                    @Nonnull final String sentinelValue )
  {
    final String declaredName =
      (String) getAnnotationValue( method, annotationClassname, parameterName ).getValue();
    if ( sentinelValue.equals( declaredName ) )
    {
      final String defaultValue = defaultExtractor.apply( method );
      if ( null == defaultValue )
      {
        throw new ProcessorException( MemberChecks.toSimpleName( annotationClassname ) + " target did not specify " +
                                      "the parameter " + parameterName + " and the default value could not be derived",
                                      method );
      }
      return defaultValue;
    }
    else
    {
      if ( !SourceVersion.isIdentifier( declaredName ) )
      {
        throw new ProcessorException( MemberChecks.toSimpleName( annotationClassname ) + " target specified an " +
                                      "invalid value '" + declaredName + "' for the parameter " + parameterName + ". " +
                                      "The value must be a valid java identifier", method );
      }
      else if ( SourceVersion.isKeyword( declaredName ) )
      {
        throw new ProcessorException( MemberChecks.toSimpleName( annotationClassname ) + " target specified an " +
                                      "invalid value '" + declaredName + "' for the parameter " + parameterName + ". " +
                                      "The value must not be a java keyword", method );
      }
      return declaredName;
    }
  }

  public static boolean hasNonnullAnnotation( @Nonnull final Element element )
  {
    return hasAnnotationOfType( element, GeneratorUtil.NONNULL_ANNOTATION_CLASSNAME );
  }
}
