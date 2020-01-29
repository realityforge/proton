package org.realityforge.proton;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

@SuppressWarnings( "unused" )
public final class SuppressWarningsUtil
{
  private SuppressWarningsUtil()
  {
  }

  @Nonnull
  public static AnnotationSpec suppressWarningsAnnotation( @Nonnull final String... warnings )
  {
    return Objects.requireNonNull( maybeSuppressWarningsAnnotation( warnings ) );
  }

  @Nullable
  public static AnnotationSpec maybeSuppressWarningsAnnotation( @Nonnull final String... warnings )
  {
    final List<String> actualWarnings =
      Arrays.stream( warnings ).filter( Objects::nonNull ).sorted().collect( Collectors.toList() );
    if ( actualWarnings.isEmpty() )
    {
      return null;
    }
    else
    {
      final AnnotationSpec.Builder builder = AnnotationSpec.builder( SuppressWarnings.class );
      actualWarnings.forEach( w -> builder.addMember( "value", "$S", w ) );
      return builder.build();
    }
  }

  public static void addSuppressWarningsIfRequired( @Nonnull final ProcessingEnvironment processingEnv,
                                                    @Nonnull final TypeSpec.Builder method,
                                                    @Nonnull final TypeMirror type )
  {
    addSuppressWarningsIfRequired( processingEnv, method, Collections.singleton( type ) );
  }

  public static void addSuppressWarningsIfRequired( @Nonnull final ProcessingEnvironment processingEnv,
                                                    @Nonnull final TypeSpec.Builder method,
                                                    @Nonnull final Collection<TypeMirror> types )
  {
    addSuppressWarningsIfRequired( processingEnv, method, Collections.emptyList(), types );
  }

  public static void addSuppressWarningsIfRequired( @Nonnull final ProcessingEnvironment processingEnv,
                                                    @Nonnull final TypeSpec.Builder method,
                                                    @Nonnull final Collection<String> additionalSuppressions,
                                                    @Nonnull final Collection<TypeMirror> types )
  {
    final AnnotationSpec suppress =
      maybeSuppressWarningsAnnotation( processingEnv, additionalSuppressions, types );
    if ( null != suppress )
    {
      method.addAnnotation( suppress );
    }
  }

  public static void addSuppressWarningsIfRequired( @Nonnull final ProcessingEnvironment processingEnv,
                                                    @Nonnull final MethodSpec.Builder method,
                                                    @Nonnull final TypeMirror type )
  {
    addSuppressWarningsIfRequired( processingEnv, method, Collections.singleton( type ) );
  }

  public static void addSuppressWarningsIfRequired( @Nonnull final ProcessingEnvironment processingEnv,
                                                    @Nonnull final MethodSpec.Builder method,
                                                    @Nonnull final Collection<TypeMirror> types )
  {
    addSuppressWarningsIfRequired( processingEnv, method, Collections.emptyList(), types );
  }

  public static void addSuppressWarningsIfRequired( @Nonnull final ProcessingEnvironment processingEnv,
                                                    @Nonnull final MethodSpec.Builder method,
                                                    @Nonnull final Collection<String> additionalSuppressions,
                                                    @Nonnull final Collection<TypeMirror> types )
  {
    final AnnotationSpec suppress =
      maybeSuppressWarningsAnnotation( processingEnv, additionalSuppressions, types );
    if ( null != suppress )
    {
      method.addAnnotation( suppress );
    }
  }

  public static void addSuppressWarningsIfRequired( @Nonnull final ProcessingEnvironment processingEnv,
                                                    @Nonnull final FieldSpec.Builder field,
                                                    @Nonnull final TypeMirror type )
  {
    addSuppressWarningsIfRequired( processingEnv, field, Collections.singleton( type ) );
  }

  public static void addSuppressWarningsIfRequired( @Nonnull final ProcessingEnvironment processingEnv,
                                                    @Nonnull final FieldSpec.Builder field,
                                                    @Nonnull final Collection<TypeMirror> types )
  {
    addSuppressWarningsIfRequired( processingEnv, field, Collections.emptyList(), types );
  }

  public static void addSuppressWarningsIfRequired( @Nonnull final ProcessingEnvironment processingEnv,
                                                    @Nonnull final FieldSpec.Builder field,
                                                    @Nonnull final Collection<String> additionalSuppressions,
                                                    @Nonnull final Collection<TypeMirror> types )
  {
    final AnnotationSpec suppress = maybeSuppressWarningsAnnotation( processingEnv, additionalSuppressions, types );
    if ( null != suppress )
    {
      field.addAnnotation( suppress );
    }
  }

  public static void addSuppressWarningsIfRequired( @Nonnull final ProcessingEnvironment processingEnv,
                                                    @Nonnull final ParameterSpec.Builder field,
                                                    @Nonnull final TypeMirror type )
  {
    addSuppressWarningsIfRequired( processingEnv, field, Collections.singleton( type ) );
  }

  public static void addSuppressWarningsIfRequired( @Nonnull final ProcessingEnvironment processingEnv,
                                                    @Nonnull final ParameterSpec.Builder field,
                                                    @Nonnull final Collection<TypeMirror> types )
  {
    addSuppressWarningsIfRequired( processingEnv, field, Collections.emptyList(), types );
  }

  public static void addSuppressWarningsIfRequired( @Nonnull final ProcessingEnvironment processingEnv,
                                                    @Nonnull final ParameterSpec.Builder field,
                                                    @Nonnull final Collection<String> additionalSuppressions,
                                                    @Nonnull final Collection<TypeMirror> types )
  {
    final AnnotationSpec suppress = maybeSuppressWarningsAnnotation( processingEnv, additionalSuppressions, types );
    if ( null != suppress )
    {
      field.addAnnotation( suppress );
    }
  }

  /**
   * Generate a suppress warnings annotation if any of the types passed in are either deprecated or rawtypes.
   *
   * @param types the types to analyze to determine if suppressions are required.
   * @return a suppress annotation if required.
   */
  @Nullable
  public static AnnotationSpec maybeSuppressWarningsAnnotation( @Nonnull final ProcessingEnvironment processingEnv,
                                                                @Nonnull final Collection<TypeMirror> types )
  {
    return maybeSuppressWarningsAnnotation( processingEnv, Collections.emptyList(), types );
  }

  /**
   * Generate a suppress warnings annotation if any of the types passed in are either deprecated or rawtypes.
   * The additionalSuppressions parameter will also be added to list of suppressions.
   *
   * @param additionalSuppressions the suppressions that must be added to suppression annotation.
   * @param types                  the types to analyze to determine if suppressions are required.
   * @return a suppress annotation if required.
   */
  @Nullable
  public static AnnotationSpec maybeSuppressWarningsAnnotation( @Nonnull final ProcessingEnvironment processingEnv,
                                                                @Nonnull final Collection<String> additionalSuppressions,
                                                                @Nonnull final Collection<TypeMirror> types )
  {
    // short cut traversing types by checking whether additionalSuppressions match
    final boolean hasRawTypes =
      additionalSuppressions.contains( "rawtypes" ) ||
      types.stream().anyMatch( t -> hasRawTypes( processingEnv, t ) );

    final boolean hasDeprecatedTypes =
      additionalSuppressions.contains( "deprecation" ) ||
      types.stream().anyMatch( t -> hasDeprecatedTypes( processingEnv, t ) );

    if ( hasRawTypes || hasDeprecatedTypes || !additionalSuppressions.isEmpty() )
    {
      final ArrayList<String> suppressions = new ArrayList<>( additionalSuppressions );
      if ( hasRawTypes )
      {
        suppressions.add( "rawtypes" );
      }
      if ( hasDeprecatedTypes )
      {
        suppressions.add( "deprecation" );
      }
      return suppressWarningsAnnotation( suppressions.stream().sorted().distinct().toArray( String[]::new ) );
    }
    else
    {
      return null;
    }
  }

  private static boolean hasDeprecatedTypes( @Nonnull final ProcessingEnvironment processingEnv,
                                             @Nonnull final TypeMirror type )
  {
    final TypeKind kind = type.getKind();
    if ( TypeKind.TYPEVAR == kind )
    {
      final TypeVariable typeVariable = (TypeVariable) type;
      return hasDeprecatedTypes( processingEnv, typeVariable.getLowerBound() ) ||
             hasDeprecatedTypes( processingEnv, typeVariable.getUpperBound() );
    }
    else if ( TypeKind.ARRAY == kind )
    {
      return hasDeprecatedTypes( processingEnv, ( (ArrayType) type ).getComponentType() );
    }
    else if ( TypeKind.DECLARED == kind )
    {
      if ( isElementDeprecated( processingEnv, type ) )
      {
        return true;
      }
      else
      {
        final DeclaredType declaredType = (DeclaredType) type;
        return declaredType
          .getTypeArguments()
          .stream()
          .anyMatch( t -> hasDeprecatedTypes( processingEnv, t ) );
      }
    }
    else if ( TypeKind.EXECUTABLE == kind )
    {
      final ExecutableType executableType = (ExecutableType) type;
      return isElementDeprecated( processingEnv, executableType ) ||
             hasDeprecatedTypes( processingEnv, executableType.getReturnType() ) ||
             executableType.getTypeVariables()
               .stream()
               .anyMatch( t -> hasDeprecatedTypes( processingEnv, t ) ) ||
             executableType.getThrownTypes()
               .stream()
               .anyMatch( t -> hasDeprecatedTypes( processingEnv, t ) ) ||
             executableType.getParameterTypes()
               .stream()
               .anyMatch( t -> hasDeprecatedTypes( processingEnv, t ) );
    }
    else
    {
      return false;
    }
  }

  private static boolean isElementDeprecated( @Nonnull final ProcessingEnvironment processingEnv,
                                              @Nonnull final TypeMirror type )
  {
    final Element element = processingEnv.getTypeUtils().asElement( type );
    return null != element && isElementDeprecated( element );
  }

  private static boolean isElementDeprecated( @Nonnull final Element element )
  {
    if ( element.getAnnotationMirrors().stream().anyMatch( SuppressWarningsUtil::isDeprecated ) )
    {
      return true;
    }
    else if ( ( element.getKind().isClass() || element.getKind().isInterface() ) &&
              ElementKind.PACKAGE != element.getEnclosingElement().getKind() )
    {
      return isElementDeprecated( element.getEnclosingElement() );
    }
    else
    {
      return false;
    }
  }

  private static boolean isDeprecated( @Nonnull final AnnotationMirror a )
  {
    return a.getAnnotationType().toString().equals( Deprecated.class.getName() );
  }

  private static boolean hasRawTypes( @Nonnull final ProcessingEnvironment processingEnv,
                                      @Nonnull final TypeMirror type )
  {
    final TypeKind kind = type.getKind();
    if ( TypeKind.TYPEVAR == kind )
    {
      final TypeVariable typeVariable = (TypeVariable) type;
      return hasRawTypes( processingEnv, typeVariable.getLowerBound() ) ||
             hasRawTypes( processingEnv, typeVariable.getUpperBound() );
    }
    else if ( TypeKind.ARRAY == kind )
    {
      return hasRawTypes( processingEnv, ( (ArrayType) type ).getComponentType() );
    }
    else if ( TypeKind.DECLARED == kind )
    {
      final DeclaredType declaredType = (DeclaredType) type;
      final int typeArgumentCount = declaredType.getTypeArguments().size();
      final TypeElement typeElement = (TypeElement) processingEnv.getTypeUtils().asElement( type );
      if ( typeArgumentCount != typeElement.getTypeParameters().size() )
      {
        return true;
      }
      else
      {
        return declaredType
          .getTypeArguments()
          .stream()
          .anyMatch( t -> hasRawTypes( processingEnv, t ) );
      }
    }
    else if ( TypeKind.EXECUTABLE == kind )
    {
      final ExecutableType executableType = (ExecutableType) type;
      return hasRawTypes( processingEnv, executableType.getReturnType() ) ||
             executableType.getTypeVariables()
               .stream()
               .anyMatch( t -> hasRawTypes( processingEnv, t ) ) ||
             executableType.getThrownTypes()
               .stream()
               .anyMatch( t -> hasRawTypes( processingEnv, t ) ) ||
             executableType.getParameterTypes()
               .stream()
               .anyMatch( t -> hasRawTypes( processingEnv, t ) );
    }
    else
    {
      return false;
    }
  }
}
