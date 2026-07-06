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
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
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

public final class SuppressWarningsUtilTest
{
  @Test
  public void suppressionReadHelpersInspectRealElements()
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
                                  "import java.lang.annotation.ElementType;\n" +
                                  "import java.lang.annotation.Target;\n" +
                                  "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})\n" +
                                  "@interface SuppressCustomWarnings { String[] value(); }\n" +
                                  "@SuppressWarnings(\"fromType\")\n" +
                                  "@SuppressCustomWarnings(\"customFromType\")\n" +
                                  "class Model {\n" +
                                  "  @SuppressWarnings({\"fromField\", \"shared\"})\n" +
                                  "  String field;\n" +
                                  "  @SuppressCustomWarnings({\"customFromMethod\", \"sharedCustom\"})\n" +
                                  "  void action() {}\n" +
                                  "}\n" );
      final SuppressionProcessor processor = new SuppressionProcessor();
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
  private static VariableElement field( @Nonnull final TypeElement type, @Nonnull final String name )
  {
    return type
      .getEnclosedElements()
      .stream()
      .filter( e -> ElementKind.FIELD == e.getKind() && name.equals( e.getSimpleName().toString() ) )
      .map( e -> (VariableElement) e )
      .findFirst()
      .orElseThrow();
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

  private static final class SuppressionProcessor
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
        final TypeElement type = getTypeElement( "com.example.Model" );
        final VariableElement field = field( type, "field" );
        final ExecutableElement method = method( type, "action" );
        final String customSuppressWarnings = "com.example.SuppressCustomWarnings";

        assertTrue( SuppressWarningsUtil.isSuppressed( (AnnotatedConstruct) type, "fromType" ) );
        assertFalse( SuppressWarningsUtil.isSuppressed( (AnnotatedConstruct) method, "fromType" ) );
        assertTrue( SuppressWarningsUtil.isSuppressed( method, "fromType" ) );
        assertTrue( SuppressWarningsUtil.isSuppressed( field, "fromField" ) );
        assertTrue( SuppressWarningsUtil.isSuppressed( field, "shared" ) );
        assertFalse( SuppressWarningsUtil.isSuppressed( field, "missing" ) );
        assertTrue( SuppressWarningsUtil.isNotSuppressed( field, "missing" ) );
        assertFalse( SuppressWarningsUtil.isNotSuppressed( field, "shared" ) );

        assertTrue( SuppressWarningsUtil.isSuppressed( method, "customFromMethod", customSuppressWarnings ) );
        assertTrue( SuppressWarningsUtil.isSuppressed( method, "customFromType", customSuppressWarnings ) );
        assertFalse( SuppressWarningsUtil.isSuppressed( (AnnotatedConstruct) method,
                                                        "customFromType",
                                                        customSuppressWarnings ) );
        assertTrue( SuppressWarningsUtil.isNotSuppressed( method, "missing", customSuppressWarnings ) );
        assertTrue( ElementsUtil.isWarningSuppressed( method, "customFromMethod", customSuppressWarnings ) );
        assertTrue( ElementsUtil.isWarningNotSuppressed( method, "missing", customSuppressWarnings ) );

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
