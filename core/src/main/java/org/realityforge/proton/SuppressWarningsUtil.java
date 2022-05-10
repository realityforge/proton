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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

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
      Arrays.stream( warnings ).filter( Objects::nonNull ).sorted().toList();
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
   * @param processingEnv the processing environment.
   * @param types         the types to analyze to determine if suppressions are required.
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
   * @param processingEnv          the processing environment.
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
      types.stream().anyMatch( t -> TypesUtil.hasRawTypes( processingEnv, t ) );

    final boolean hasDeprecatedTypes =
      additionalSuppressions.contains( "deprecation" ) ||
      types.stream().anyMatch( t -> TypesUtil.isDeprecated( processingEnv, t ) );

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
}
