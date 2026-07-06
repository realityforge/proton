package org.realityforge.proton;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.testng.annotations.Test;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.*;

public final class MemberChecksTest
{
  @Test
  public void typeLevelChecksInspectRealElements()
    throws Exception
  {
    final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull( compiler );
    final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    final Path classOutput = Files.createTempDirectory( "compile" );
    try ( StandardJavaFileManager fileManager =
            compiler.getStandardFileManager( diagnostics, Locale.getDefault(), UTF_8 ) )
    {
      fileManager.setLocationFromPaths( StandardLocation.CLASS_OUTPUT, Collections.singletonList( classOutput ) );
      final JavaFileObject source =
        new SourceJavaFileObject( "com.example.Model",
                                  "package com.example;\n" +
                                  "class Model {}\n" +
                                  "interface Contract {}\n" +
                                  "enum Mode { ACTIVE }\n" +
                                  "class Generic<T> {}\n" +
                                  "class Outer {\n" +
                                  "  class Inner {}\n" +
                                  "  static class StaticNested {}\n" +
                                  "}\n" );
      final TypeCheckProcessor processor = new TypeCheckProcessor();
      final JavaCompiler.CompilationTask task =
        compiler.getTask( null,
                          fileManager,
                          diagnostics,
                          List.of( "--release", "17" ),
                          null,
                          Collections.singletonList( source ) );
      task.setProcessors( Collections.singletonList( processor ) );
      assertTrue( task.call(), asMessage( diagnostics ) );
      assertTrue( processor.wasValidated() );
    }
    finally
    {
      deleteDir( classOutput );
    }
  }

  @Nonnull
  private static String asMessage( @Nonnull final DiagnosticCollector<JavaFileObject> diagnostics )
  {
    return diagnostics.getDiagnostics().stream().map( Object::toString ).collect( Collectors.joining( "\n" ) );
  }

  private static void deleteDir( @Nonnull final Path dir )
    throws IOException
  {
    try ( Stream<Path> paths = Files.walk( dir ) )
    {
      for ( final Path path : paths.sorted( Comparator.reverseOrder() ).toList() )
      {
        Files.deleteIfExists( path );
      }
    }
  }

  @Nonnull
  private static TypeElement nestedType( @Nonnull final TypeElement type, @Nonnull final String name )
  {
    return type
      .getEnclosedElements()
      .stream()
      .filter( e -> ( ElementKind.CLASS == e.getKind() || ElementKind.INTERFACE == e.getKind() ) &&
                    name.equals( e.getSimpleName().toString() ) )
      .map( e -> (TypeElement) e )
      .findFirst()
      .orElseThrow();
  }

  private static final class SourceJavaFileObject
    extends SimpleJavaFileObject
  {
    @Nonnull
    private final String _source;

    SourceJavaFileObject( @Nonnull final String classname, @Nonnull final String source )
    {
      super( URI.create( "string:///" + classname.replace( '.', '/' ) + Kind.SOURCE.extension ), Kind.SOURCE );
      _source = source;
    }

    @Override
    public CharSequence getCharContent( final boolean ignoreEncodingErrors )
    {
      return _source;
    }
  }

  private static final class TypeCheckProcessor
    extends AbstractProcessor
  {
    private static final String ANNOTATION = "com.example.Test";

    private boolean _validated;

    @Nonnull
    @Override
    public Set<String> getSupportedAnnotationTypes()
    {
      return Collections.singleton( "*" );
    }

    @Nonnull
    @Override
    public SourceVersion getSupportedSourceVersion()
    {
      return SourceVersion.latestSupported();
    }

    @Override
    public boolean process( final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv )
    {
      if ( !_validated && !roundEnv.processingOver() )
      {
        final TypeElement model = getTypeElement( "com.example.Model" );
        final TypeElement contract = getTypeElement( "com.example.Contract" );
        final TypeElement mode = getTypeElement( "com.example.Mode" );
        final TypeElement generic = getTypeElement( "com.example.Generic" );
        final TypeElement outer = getTypeElement( "com.example.Outer" );
        final TypeElement inner = nestedType( outer, "Inner" );
        final TypeElement staticNested = nestedType( outer, "StaticNested" );

        MemberChecks.mustBeClass( ANNOTATION, model );
        assertMessage( () -> MemberChecks.mustBeClass( ANNOTATION, contract ), "@Test target must be a class" );

        MemberChecks.mustBeInterface( ANNOTATION, contract );
        assertMessage( () -> MemberChecks.mustBeInterface( ANNOTATION, model ), "@Test target must be an interface" );

        MemberChecks.mustBeClassOrInterface( ANNOTATION, model );
        MemberChecks.mustBeClassOrInterface( ANNOTATION, contract );
        assertMessage( () -> MemberChecks.mustBeClassOrInterface( ANNOTATION, mode ),
                       "@Test target must be a class or an interface" );

        MemberChecks.mustNotHaveTypeParameters( ANNOTATION, model );
        assertMessage( () -> MemberChecks.mustNotHaveTypeParameters( ANNOTATION, generic ),
                       "@Test target must not have type parameters" );

        MemberChecks.mustNotBeNonStaticNestedType( ANNOTATION, model );
        MemberChecks.mustNotBeNonStaticNestedType( ANNOTATION, staticNested );
        assertMessage( () -> MemberChecks.mustNotBeNonStaticNestedType( ANNOTATION, inner ),
                       "@Test target must not be a non-static nested class" );
        _validated = true;
      }
      return false;
    }

    private static void assertMessage( @Nonnull final ThrowingRunnable action, @Nonnull final String message )
    {
      try
      {
        action.run();
        fail( "Expected ProcessorException" );
      }
      catch ( final ProcessorException pe )
      {
        assertEquals( pe.getMessage(), message );
      }
    }

    @Nonnull
    private TypeElement getTypeElement( @Nonnull final String classname )
    {
      final TypeElement typeElement = processingEnv.getElementUtils().getTypeElement( classname );
      assertNotNull( typeElement );
      return typeElement;
    }

    boolean wasValidated()
    {
      return _validated;
    }
  }

  private interface ThrowingRunnable
  {
    void run();
  }
}
