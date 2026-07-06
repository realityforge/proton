package org.realityforge.proton;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

final class TestUtil
{
  private TestUtil()
  {
  }

  @Nonnull
  static CompilationResult compile( @Nonnull final Source source, @Nonnull final Processor processor )
    throws Exception
  {
    return compile( Collections.singletonList( source ), processor );
  }

  @Nonnull
  static CompilationResult compile( @Nonnull final List<Source> sources, @Nonnull final Processor processor )
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
      final List<JavaFileObject> sourceObjects =
        sources
          .stream()
          .map( source -> new SourceJavaFileObject( source.classname(), source.source() ) )
          .collect( Collectors.toList() );
      final JavaCompiler.CompilationTask task =
        compiler.getTask( null,
                          fileManager,
                          diagnosticCollector,
                          List.of( "--release", "17", "-classpath", System.getProperty( "java.class.path" ) ),
                          null,
                          sourceObjects );
      task.setProcessors( Collections.singletonList( processor ) );
      final CompilationResult result = new CompilationResult( diagnosticCollector.getDiagnostics() );
      assertTrue( task.call(), result.diagnostics() );
      return result;
    }
    finally
    {
      deleteDir( classOutput );
    }
  }

  @Nonnull
  static Source source( @Nonnull final String classname, @Nonnull final String source )
  {
    return new Source( classname, source );
  }

  @Nonnull
  static <T> T proxy( @Nonnull final Class<T> type, @Nonnull final ProxyInvocation invocation )
  {
    return type.cast( Proxy.newProxyInstance( type.getClassLoader(), new Class<?>[]{ type }, ( self, method, args ) -> {
      if ( "equals".equals( method.getName() ) && 1 == method.getParameterCount() )
      {
        return self == args[ 0 ];
      }
      else if ( "hashCode".equals( method.getName() ) && 0 == method.getParameterCount() )
      {
        return System.identityHashCode( self );
      }
      else if ( "toString".equals( method.getName() ) && 0 == method.getParameterCount() )
      {
        return type.getSimpleName() + "Proxy";
      }
      else
      {
        return invocation.invoke( self, method, args );
      }
    } ) );
  }

  @Nonnull
  static Name name( @Nonnull final String value )
  {
    return new NameImpl( value );
  }

  static Object unsupported( @Nonnull final Method method )
  {
    throw new UnsupportedOperationException( method.toString() );
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

  @FunctionalInterface
  interface ProxyInvocation
  {
    Object invoke( @Nonnull Object self, @Nonnull Method method, Object[] args )
      throws Throwable;
  }

  abstract static class TestProcessor
    extends AbstractProcessor
  {
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
    public abstract boolean process( Set<? extends TypeElement> annotations, RoundEnvironment roundEnv );
  }

  static final class Source
  {
    @Nonnull
    private final String _classname;
    @Nonnull
    private final String _source;

    Source( @Nonnull final String classname, @Nonnull final String source )
    {
      _classname = classname;
      _source = source;
    }

    @Nonnull
    String classname()
    {
      return _classname;
    }

    @Nonnull
    String source()
    {
      return _source;
    }
  }

  static final class CompilationResult
  {
    @Nonnull
    private final List<Diagnostic<? extends JavaFileObject>> _diagnostics;

    CompilationResult( @Nonnull final List<Diagnostic<? extends JavaFileObject>> diagnostics )
    {
      _diagnostics = diagnostics;
    }

    @Nonnull
    List<Diagnostic<? extends JavaFileObject>> diagnosticsList()
    {
      return _diagnostics;
    }

    @Nonnull
    String diagnostics()
    {
      return _diagnostics.stream().map( Object::toString ).collect( Collectors.joining( "\n" ) );
    }
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

  private static final class NameImpl
    implements Name
  {
    @Nonnull
    private final String _value;

    NameImpl( @Nonnull final String value )
    {
      _value = value;
    }

    @Override
    public boolean contentEquals( final CharSequence cs )
    {
      return _value.contentEquals( cs );
    }

    @Override
    public int length()
    {
      return _value.length();
    }

    @Override
    public char charAt( final int index )
    {
      return _value.charAt( index );
    }

    @Override
    public CharSequence subSequence( final int start, final int end )
    {
      return _value.subSequence( start, end );
    }

    @Override
    public String toString()
    {
      return _value;
    }
  }
}
