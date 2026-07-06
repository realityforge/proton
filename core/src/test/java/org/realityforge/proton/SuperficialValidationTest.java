package org.realityforge.proton;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.NullType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.ElementFilter;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.testng.annotations.Test;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public final class SuperficialValidationTest
{
  @Test
  public void validateElementReturnsTrueForTypeWithEnclosedRecordComponents()
    throws Exception
  {
    final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull( compiler );
    final DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
    final Path classOutput = Files.createTempDirectory( "compile" );
    try ( StandardJavaFileManager fileManager =
            compiler.getStandardFileManager( diagnosticCollector, Locale.getDefault(), UTF_8 ) )
    {
      fileManager.setLocationFromPaths( StandardLocation.CLASS_OUTPUT, Collections.singletonList( classOutput ) );
      final JavaFileObject source =
        new SourceJavaFileObject( "com.example.Container",
                                  """
                                    package com.example;
                                    public final class Container {
                                      private record Heading(String text) {}
                                      private record HeadingNode(Heading heading) {}
                                    }
                                    """ );
      final ValidationProcessor processor = new ValidationProcessor();
      final JavaCompiler.CompilationTask task =
        compiler.getTask( null,
                          fileManager,
                          diagnosticCollector,
                          List.of( "--release", "17" ),
                          null,
                          Collections.singletonList( source ) );
      task.setProcessors( Collections.singletonList( processor ) );
      assertTrue( task.call(), asMessage( diagnosticCollector ) );
      assertTrue( processor.wasValidated() );
    }
    finally
    {
      deleteDir( classOutput );
    }
  }

  @Test
  public void isTypeOf_returnsFalseForNullType()
    throws Throwable
  {
    assertFalse( invokeIsTypeOf( String.class, new FakeNullType() ) );
  }

  @SuppressWarnings( "SameParameterValue" )
  private static boolean invokeIsTypeOf( final Class<?> clazz, final TypeMirror type )
    throws Throwable
  {
    final Method method = SuperficialValidation.class.getDeclaredMethod( "isTypeOf", Class.class, TypeMirror.class );
    method.setAccessible( true );
    try
    {
      return (boolean) method.invoke( null, clazz, type );
    }
    catch ( final InvocationTargetException ite )
    {
      throw ite.getCause();
    }
  }

  private static final class FakeNullType
    implements NullType
  {
    @Override
    public TypeKind getKind()
    {
      return TypeKind.NULL;
    }

    @Override
    public <R, P> R accept( final TypeVisitor<R, P> visitor, final P parameter )
    {
      return visitor.visitNull( this, parameter );
    }

    @Override
    @Nonnull
    public List<? extends AnnotationMirror> getAnnotationMirrors()
    {
      return Collections.emptyList();
    }

    @Override
    public <A extends Annotation> A getAnnotation( final Class<A> annotationType )
    {
      return null;
    }

    @Override
    @Nonnull
    public <A extends Annotation> A[] getAnnotationsByType( final Class<A> annotationType )
    {
      @SuppressWarnings( "unchecked" )
      final A[] annotations = (A[]) Array.newInstance( annotationType, 0 );
      return annotations;
    }

    @Override
    public String toString()
    {
      return "<nulltype>";
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

  private static boolean hasEnclosedRecordComponents( @Nonnull final TypeElement type )
  {
    for ( final TypeElement nestedType : ElementFilter.typesIn( type.getEnclosedElements() ) )
    {
      if ( ElementKind.RECORD == nestedType.getKind() && !nestedType.getRecordComponents().isEmpty() )
      {
        return true;
      }
    }
    return false;
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

  private static final class ValidationProcessor
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
        final TypeElement type = processingEnv.getElementUtils().getTypeElement( "com.example.Container" );
        assertNotNull( type );
        assertTrue( hasEnclosedRecordComponents( type ) );
        assertTrue( SuperficialValidation.validateElement( processingEnv, type ) );
        _validated = true;
      }
      return false;
    }

    boolean wasValidated()
    {
      return _validated;
    }
  }
}
