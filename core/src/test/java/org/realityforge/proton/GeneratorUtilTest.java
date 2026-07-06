package org.realityforge.proton;

import com.palantir.javapoet.MethodSpec;
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
import javax.lang.model.element.ExecutableElement;
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

public final class GeneratorUtilTest
{
  @Test
  public void refMethodAcceptsAdditionalSuppressions()
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
                                  "public class Model extends Base<String> {}\n" +
                                  "class Base<T> {\n" +
                                  "  public T getValue() { return null; }\n" +
                                  "  public int getCount() { return 0; }\n" +
                                  "}\n" );
      final RefMethodProcessor processor = new RefMethodProcessor();
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
  private static ExecutableElement method( @Nonnull final TypeElement type, @Nonnull final String name )
  {
    return type
      .getEnclosedElements()
      .stream()
      .filter( e -> ElementKind.METHOD == e.getKind() && name.equals( e.getSimpleName().toString() ) )
      .map( e -> (ExecutableElement) e )
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

  private static final class RefMethodProcessor
    extends AbstractProcessor
  {
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
        final TypeElement base = getTypeElement( "com.example.Base" );
        final MethodSpec valueMethod =
          GeneratorUtil
            .refMethod( processingEnv, model, method( base, "getValue" ), Collections.singletonList( "unchecked" ) )
            .build();
        final String value = valueMethod.toString();
        assertTrue( value.contains( "@java.lang.Override\n" ), value );
        assertTrue( value.contains( "@java.lang.SuppressWarnings(\"unchecked\")\n" ), value );
        assertTrue( value.contains( "@javax.annotation.Nonnull\n" ), value );
        assertTrue( value.contains( "public java.lang.String getValue()" ), value );

        final MethodSpec countMethod =
          GeneratorUtil.refMethod( processingEnv, model, method( base, "getCount" ) ).build();
        final String count = countMethod.toString();
        assertFalse( count.contains( "@SuppressWarnings" ), count );
        assertFalse( count.contains( "@javax.annotation.Nonnull" ), count );
        assertTrue( count.contains( "public int getCount()" ), count );
        _validated = true;
      }
      return false;
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
}
