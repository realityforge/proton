package org.realityforge.proton;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.util.ElementFilter;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public final class GeneratorUtilTest
{
  @Test
  public void generatedClassNameHelpersIncludeNestedClassPrefixes()
  {
    final ClassName nested = ClassName.get( "com.example", "Outer", "Inner" );

    assertEquals( GeneratorUtil.getGeneratedSimpleClassName( nested, "Generated", "Impl" ),
                  "Outer_GeneratedInnerImpl" );
    assertEquals( GeneratorUtil.getGeneratedClassName( nested, "Generated", "Impl" ).toString(),
                  "com.example.Outer_GeneratedInnerImpl" );
  }

  @Test
  public void generatorHelpersCopyModelMetadataIntoJavaPoetBuilders()
    throws Exception
  {
    final GeneratorProcessor processor = new GeneratorProcessor();

    TestUtil.compile( TestUtil.source( "com.example.GeneratorTarget", """
      package com.example;
      import java.io.IOException;
      import java.util.List;
      import javax.annotation.Nonnull;
      class Base {}
      @Deprecated
      @SuppressWarnings("unused")
      public class GeneratorTarget<T extends Number> extends Base {
        @Nonnull
        public <X extends CharSequence> List<T> convert(@Nonnull final String name) throws IOException {
          return null;
        }
        public int primitiveRef() {
          return 0;
        }
        protected void protectedMethod() {}
        @Nonnull
        String nonnullField;
        public static class Nested {}
      }
      """ ), processor );

    assertTrue( processor.wasValidated() );
  }

  private static final class GeneratorProcessor
    extends TestUtil.TestProcessor
  {
    private boolean _validated;

    @Override
    public boolean process( final Set<? extends TypeElement> annotations, final javax.annotation.processing.RoundEnvironment roundEnv )
    {
      if ( !_validated && !roundEnv.processingOver() )
      {
        final TypeElement target = type( "com.example.GeneratorTarget" );
        final TypeElement base = type( "com.example.Base" );
        validateNameHelpers( target );
        validateTypeAndMethodBuilderHelpers( target, base );
        validateAnnotationCopyHelpers( target );
        validateOverrideAndRefMethodHelpers( target );
        _validated = true;
      }
      return false;
    }

    boolean wasValidated()
    {
      return _validated;
    }

    private void validateNameHelpers( @Nonnull final TypeElement target )
    {
      final TypeElement nested = nestedType( target, "Nested" );

      assertEquals( GeneratorUtil.getQualifiedPackageName( target ), "com.example" );
      assertEquals( GeneratorUtil.getGeneratedSimpleClassName( target, "Generated", "Impl" ),
                    "GeneratedGeneratorTargetImpl" );
      assertEquals( GeneratorUtil.getGeneratedClassName( target, "Generated", "Impl" ).toString(),
                    "com.example.GeneratedGeneratorTargetImpl" );
      assertEquals( GeneratorUtil.getGeneratedSimpleClassName( nested, "Generated", "Impl" ),
                    "GeneratorTarget_GeneratedNestedImpl" );
      assertEquals( GeneratorUtil.getGeneratedClassName( nested, "Generated", "Impl" ).toString(),
                    "com.example.GeneratorTarget_GeneratedNestedImpl" );
      assertEquals( GeneratorUtil.getTypeArgumentsAsNames( (DeclaredType) target.asType() ).get( 0 ).name(), "T" );
    }

    private void validateTypeAndMethodBuilderHelpers( @Nonnull final TypeElement target,
                                                      @Nonnull final TypeElement base )
    {
      final TypeSpec.Builder publicType = TypeSpec.classBuilder( "Copy" );
      GeneratorUtil.copyAccessModifiers( target, publicType );
      assertTrue( publicType.build().modifiers().contains( Modifier.PUBLIC ) );

      final TypeSpec.Builder packageType = TypeSpec.classBuilder( "Copy" );
      GeneratorUtil.copyAccessModifiers( base, packageType );
      assertFalse( packageType.build().modifiers().contains( Modifier.PUBLIC ) );

      final MethodSpec.Builder publicMethod = MethodSpec.methodBuilder( "copy" );
      GeneratorUtil.copyAccessModifiers( target, publicMethod );
      assertTrue( publicMethod.build().modifiers().contains( Modifier.PUBLIC ) );

      final MethodSpec.Builder protectedMethod = MethodSpec.methodBuilder( "copy" );
      GeneratorUtil.copyAccessModifiers( method( target, "protectedMethod" ), protectedMethod );
      assertTrue( protectedMethod.build().modifiers().contains( Modifier.PROTECTED ) );

      final TypeSpec.Builder typedSpec = TypeSpec.classBuilder( "Typed" );
      GeneratorUtil.copyTypeParameters( target, typedSpec );
      assertEquals( typedSpec.build().typeVariables().get( 0 ).name(), "T" );

      final MethodSpec.Builder typedMethod = MethodSpec.methodBuilder( "typed" );
      GeneratorUtil.copyTypeParameters( target, typedMethod );
      assertEquals( typedMethod.build().typeVariables().get( 0 ).name(), "T" );

      final TypeSpec.Builder generated = TypeSpec.classBuilder( "Generated" );
      GeneratorUtil.addGeneratedAnnotation( processingEnv, generated, "GeneratorUtilTest" );
      assertTrue( annotationStrings( generated.build().annotations() )
                    .contains( "@javax.annotation.processing.Generated(\"GeneratorUtilTest\")" ) );

      final TypeSpec.Builder originating = TypeSpec.classBuilder( "Originating" );
      GeneratorUtil.addOriginatingTypes( target, originating );
      final List<Element> originatingElements = originating.build().originatingElements();
      assertTrue( originatingElements.contains( target ) );
      assertTrue( originatingElements.contains( base ) );
    }

    private void validateAnnotationCopyHelpers( @Nonnull final TypeElement target )
    {
      final TypeSpec.Builder typeBuilder = TypeSpec.classBuilder( "Annotated" );
      GeneratorUtil.copyWhitelistedAnnotations( target, typeBuilder );
      final List<String> typeAnnotations = annotationStrings( typeBuilder.build().annotations() );
      assertTrue( typeAnnotations.contains( "@java.lang.Deprecated" ) );
      assertFalse( typeAnnotations.stream().anyMatch( a -> a.contains( "SuppressWarnings" ) ) );

      final ExecutableElement convert = method( target, "convert" );
      final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder( "copy" );
      GeneratorUtil.copyWhitelistedAnnotations( convert, methodBuilder );
      assertTrue( annotationStrings( methodBuilder.build().annotations() ).contains( "@javax.annotation.Nonnull" ) );

      final VariableElement parameter = convert.getParameters().get( 0 );
      final ParameterSpec.Builder parameterBuilder = ParameterSpec.builder( TypeName.get( String.class ), "name" );
      GeneratorUtil.copyWhitelistedAnnotations( parameter, parameterBuilder );
      assertTrue( annotationStrings( parameterBuilder.build().annotations() ).contains( "@javax.annotation.Nonnull" ) );

      final FieldSpec.Builder fieldBuilder = FieldSpec.builder( TypeName.get( String.class ), "name" );
      GeneratorUtil.copyWhitelistedAnnotations( field( target, "nonnullField" ), fieldBuilder );
      assertTrue( annotationStrings( fieldBuilder.build().annotations() ).contains( "@javax.annotation.Nonnull" ) );
    }

    private void validateOverrideAndRefMethodHelpers( @Nonnull final TypeElement target )
    {
      final ExecutableElement convert = method( target, "convert" );
      final ExecutableType convertType =
        (ExecutableType) processingEnv.getTypeUtils().asMemberOf( (DeclaredType) target.asType(), convert );

      final MethodSpec.Builder copied = MethodSpec.methodBuilder( "copied" );
      GeneratorUtil.copyTypeParameters( convertType, copied );
      GeneratorUtil.copyExceptions( convertType, copied );
      GeneratorUtil.copyParameters( convert, convertType, copied );
      final MethodSpec copiedMethod = copied.build();
      assertEquals( copiedMethod.typeVariables().get( 0 ).name(), "X" );
      assertEquals( copiedMethod.exceptions().get( 0 ).toString(), "java.io.IOException" );
      assertEquals( copiedMethod.parameters().get( 0 ).name(), "name" );
      assertTrue( copiedMethod.parameters().get( 0 ).modifiers().contains( Modifier.FINAL ) );
      assertTrue( annotationStrings( copiedMethod.parameters().get( 0 ).annotations() )
                    .contains( "@javax.annotation.Nonnull" ) );

      final MethodSpec override =
        GeneratorUtil.overrideMethod( processingEnv, target, convert, List.of( "unchecked" ), true ).build();
      final List<String> overrideAnnotations = annotationStrings( override.annotations() );
      assertTrue( overrideAnnotations.contains( "@java.lang.Override" ) );
      assertTrue( overrideAnnotations.contains( "@javax.annotation.Nonnull" ) );
      assertTrue( overrideAnnotations.stream().anyMatch( a -> a.contains( "SuppressWarnings" ) &&
                                                           a.contains( "unchecked" ) ) );
      assertTrue( override.modifiers().contains( Modifier.PUBLIC ) );
      assertEquals( override.typeVariables().get( 0 ).name(), "X" );
      assertEquals( override.returnType().toString(), "java.util.List<T>" );
      assertEquals( override.exceptions().get( 0 ).toString(), "java.io.IOException" );
      assertEquals( override.parameters().get( 0 ).name(), "name" );
      assertTrue( override.parameters().get( 0 ).modifiers().contains( Modifier.FINAL ) );

      final MethodSpec ref = GeneratorUtil.refMethod( processingEnv, target, convert ).build();
      assertTrue( annotationStrings( ref.annotations() ).contains( "@javax.annotation.Nonnull" ) );

      final MethodSpec primitiveRef = GeneratorUtil.refMethod( processingEnv, target, method( target, "primitiveRef" ) ).build();
      assertFalse( annotationStrings( primitiveRef.annotations() ).contains( "@javax.annotation.Nonnull" ) );
    }

    @Nonnull
    private TypeElement type( @Nonnull final String classname )
    {
      final TypeElement type = processingEnv.getElementUtils().getTypeElement( classname );
      assertNotNull( type );
      return type;
    }

    @Nonnull
    private static TypeElement nestedType( @Nonnull final TypeElement type, @Nonnull final String name )
    {
      return ElementFilter
        .typesIn( type.getEnclosedElements() )
        .stream()
        .filter( nested -> name.contentEquals( nested.getSimpleName() ) )
        .findFirst()
        .orElseThrow();
    }

    @Nonnull
    private static ExecutableElement method( @Nonnull final TypeElement type, @Nonnull final String name )
    {
      return ElementFilter
        .methodsIn( type.getEnclosedElements() )
        .stream()
        .filter( method -> name.contentEquals( method.getSimpleName() ) )
        .findFirst()
        .orElseThrow();
    }

    @Nonnull
    private static VariableElement field( @Nonnull final TypeElement type, @Nonnull final String name )
    {
      return ElementFilter
        .fieldsIn( type.getEnclosedElements() )
        .stream()
        .filter( field -> name.contentEquals( field.getSimpleName() ) )
        .findFirst()
        .orElseThrow();
    }

    @Nonnull
    private static List<String> annotationStrings( @Nonnull final List<AnnotationSpec> annotations )
    {
      return annotations.stream().map( AnnotationSpec::toString ).collect( Collectors.toList() );
    }
  }
}
