package org.realityforge.proton;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public final class SuppressWarningsUtilTest
{
  @Test
  public void maybeSuppressWarningsAnnotationReturnsNullWhenNoWarningsRemain()
  {
    assertNull( SuppressWarningsUtil.maybeSuppressWarningsAnnotation( (String) null ) );
  }

  @Test
  public void suppressWarningsAnnotationSortsAndFiltersWarnings()
  {
    final AnnotationSpec annotation =
      SuppressWarningsUtil.suppressWarningsAnnotation( "unchecked", null, "deprecation" );

    assertEquals( annotation.toString(), "@java.lang.SuppressWarnings({\"deprecation\", \"unchecked\"})" );
  }

  @Test
  public void processingEnvSuppressWarningsHelpersDetectRawAndDeprecatedTypes()
    throws Exception
  {
    final SuppressProcessor processor = new SuppressProcessor();

    TestUtil.compile( TestUtil.source( "com.example.SuppressTarget", """
      package com.example;
      import java.util.List;
      public final class SuppressTarget {
        @Deprecated
        static final class Old {}
        List rawList;
        List<String> typedList;
        Old old;
        String string;
      }
      """ ), processor );

    assertTrue( processor.wasValidated() );
  }

  private static final class SuppressProcessor
    extends TestUtil.TestProcessor
  {
    private boolean _validated;

    @Override
    public boolean process( final Set<? extends TypeElement> annotations, final javax.annotation.processing.RoundEnvironment roundEnv )
    {
      if ( !_validated && !roundEnv.processingOver() )
      {
        final TypeElement target = processingEnv.getElementUtils().getTypeElement( "com.example.SuppressTarget" );
        assertNotNull( target );
        final Map<String, VariableElement> fields =
          ElementFilter
            .fieldsIn( target.getEnclosedElements() )
            .stream()
            .collect( Collectors.toMap( f -> f.getSimpleName().toString(), f -> f ) );
        validateAnnotationGeneration( fields );
        validateBuilderHelpers( fields );
        _validated = true;
      }
      return false;
    }

    boolean wasValidated()
    {
      return _validated;
    }

    private void validateAnnotationGeneration( @Nonnull final Map<String, VariableElement> fields )
    {
      assertNull( SuppressWarningsUtil.maybeSuppressWarningsAnnotation( processingEnv,
                                                                        List.of( fields.get( "typedList" ).asType(),
                                                                                 fields.get( "string" ).asType() ) ) );
      assertEquals( SuppressWarningsUtil.maybeSuppressWarningsAnnotation( processingEnv,
                                                                         List.of( fields.get( "rawList" ).asType() ) )
                      .toString(),
                    "@java.lang.SuppressWarnings(\"rawtypes\")" );
      assertEquals( SuppressWarningsUtil.maybeSuppressWarningsAnnotation( processingEnv,
                                                                         List.of( fields.get( "old" ).asType() ) )
                      .toString(),
                    "@java.lang.SuppressWarnings(\"deprecation\")" );
      assertEquals( SuppressWarningsUtil.maybeSuppressWarningsAnnotation( processingEnv,
                                                                         List.of( "unchecked", "rawtypes" ),
                                                                         List.of( fields.get( "old" ).asType(),
                                                                                  fields.get( "rawList" ).asType() ) )
                      .toString(),
                    "@java.lang.SuppressWarnings({\"deprecation\", \"rawtypes\", \"unchecked\"})" );
    }

    private void validateBuilderHelpers( @Nonnull final Map<String, VariableElement> fields )
    {
      final TypeMirror rawList = fields.get( "rawList" ).asType();

      final TypeSpec.Builder type = TypeSpec.classBuilder( "Target" );
      SuppressWarningsUtil.addSuppressWarningsIfRequired( processingEnv, type, rawList );
      assertEquals( suppressWarningsAnnotation( type.build().annotations() ),
                    "@java.lang.SuppressWarnings(\"rawtypes\")" );

      final MethodSpec.Builder method = MethodSpec.methodBuilder( "target" );
      SuppressWarningsUtil.addSuppressWarningsIfRequired( processingEnv, method, List.of( "unchecked" ), List.of( rawList ) );
      assertEquals( suppressWarningsAnnotation( method.build().annotations() ),
                    "@java.lang.SuppressWarnings({\"rawtypes\", \"unchecked\"})" );

      final FieldSpec.Builder field = FieldSpec.builder( TypeName.get( String.class ), "target" );
      SuppressWarningsUtil.addSuppressWarningsIfRequired( processingEnv, field, fields.get( "old" ).asType() );
      assertEquals( suppressWarningsAnnotation( field.build().annotations() ),
                    "@java.lang.SuppressWarnings(\"deprecation\")" );

      final ParameterSpec.Builder parameter = ParameterSpec.builder( TypeName.get( String.class ), "target" );
      SuppressWarningsUtil.addSuppressWarningsIfRequired( processingEnv, parameter, List.of( rawList ) );
      assertEquals( suppressWarningsAnnotation( parameter.build().annotations() ),
                    "@java.lang.SuppressWarnings(\"rawtypes\")" );
    }

    @Nonnull
    private static String suppressWarningsAnnotation( @Nonnull final List<AnnotationSpec> annotations )
    {
      return annotations
        .stream()
        .map( AnnotationSpec::toString )
        .filter( annotation -> annotation.contains( "SuppressWarnings" ) )
        .findFirst()
        .orElseThrow();
    }
  }
}
