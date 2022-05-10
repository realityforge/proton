package org.realityforge.proton.qa;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.testng.annotations.AfterTest;
import static org.testng.Assert.*;

public abstract class AbstractProcessorTest
{
  @Nonnull
  private final List<Path> _dirsToDelete = new ArrayList<>();

  @AfterTest
  public void afterTest()
  {
    _dirsToDelete.forEach( this::deleteDir );
    _dirsToDelete.clear();
  }

  @SuppressWarnings( { "resource", "ResultOfMethodCallIgnored" } )
  private void deleteDir( @Nonnull final Path directory )
  {
    try
    {
      Files.walk( directory ).sorted( Comparator.reverseOrder() ).map( Path::toFile ).forEach( File::delete );
    }
    catch ( final IOException e )
    {
      throw new IllegalStateException( "Failure to delete directory: " + directory, e );
    }
  }

  @Nonnull
  protected abstract String getOptionPrefix();

  @Nonnull
  protected abstract Processor processor();

  @Nonnull
  protected List<String> getOptions()
  {
    return Arrays.asList( "-Xlint:all,-processing",
                          "-implicit:none",
                          "-A" + getOptionPrefix() + ".defer.errors=false" );
  }

  @Nonnull
  protected Processor[] additionalProcessors()
  {
    return new Processor[ 0 ];
  }

  @Nonnull
  protected String getFixtureKeyPart()
  {
    return "";
  }

  protected boolean emitGeneratedFile( @Nonnull final String target )
  {
    return !target.endsWith( ".class" );
  }

  protected final void assertSuccessfulCompile( @Nonnull final String classname,
                                                @Nonnull final String... expectedOutputResources )
    throws Exception
  {
    assertSuccessfulCompile( inputs( classname ), Arrays.asList( expectedOutputResources ) );
  }

  protected final void assertSuccessfulCompile( @Nonnull final List<JavaFileObject> inputs,
                                                @Nonnull final List<String> outputs )
    throws Exception
  {
    assertSuccessfulCompile( inputs, outputs, this::emitGeneratedFile );
  }

  protected final void assertSuccessfulCompile( @Nonnull final List<JavaFileObject> inputs,
                                                @Nonnull final List<String> expectedOutputs,
                                                @Nonnull final Predicate<String> filter )
    throws Exception
  {
    final Compilation compilation =
      CompileTestUtil.assertCompilesWithoutWarnings( inputs, getOptions(), processors(), Collections.emptyList() );

    _dirsToDelete.add( compilation.sourceOutput() );
    _dirsToDelete.add( compilation.classOutput() );

    outputFilesIfEnabled( compilation, filter );

    for ( final String expectedOutput : expectedOutputs )
    {
      final Path fixture = fixtureDir().resolve( "expected" ).resolve( expectedOutput );
      assertTrue( Files.exists( fixture ),
                  "Expected fixture to exist for " + expectedOutput + " but no such fixture present" );
      final Path output1 = compilation.sourceOutput().resolve( expectedOutput );
      final Path output2 = compilation.classOutput().resolve( expectedOutput );
      final Path output = Files.exists( output1 ) ? output1 : output2;
      assertTrue( Files.exists( output ),
                  "Expected output to exist for " + expectedOutput + " but no such output present" );
      CompileTestUtil.assertSourceMatchesTarget( fixture, output );
    }
  }

  protected final void outputFilesIfEnabled( @Nonnull final Compilation compilation,
                                             @Nonnull final Predicate<String> filter )
    throws IOException
  {
    final List<String> createdSourceFiles =
      compilation.sourceOutputFilenames().stream().filter( filter ).toList();
    final List<String> createdOutputFiles =
      compilation.classOutputFilenames().stream().filter( p -> !p.endsWith( ".class" ) ).filter( filter ).toList();

    outputFilesIfEnabled( compilation, createdSourceFiles, createdOutputFiles );
  }

  protected final void outputFilesIfEnabled( @Nonnull final Compilation results,
                                             @Nonnull final List<String> createdSourceFiles,
                                             @Nonnull final List<String> createdOutputFiles )
    throws IOException
  {
    if ( outputFiles() )
    {
      CompileTestUtil.outputFiles( createdSourceFiles, results.sourceOutput(), fixtureDir().resolve( "expected" ) );
      CompileTestUtil.outputFiles( createdOutputFiles, results.classOutput(), fixtureDir().resolve( "expected" ) );
    }
  }

  @Nonnull
  protected final List<Processor> processors()
  {
    final List<Processor> processors = new ArrayList<>();
    processors.add( processor() );
    processors.addAll( Arrays.asList( additionalProcessors() ) );
    return processors;
  }

  /**
   * Verify the supplied Compilation was successful.
   *
   * @param compilation the compilation to verify.
   */
  protected final void assertCompilationSuccessful( @Nonnull final Compilation compilation )
  {
    assertTrue( compilation.success(), compilation + " - " + describeFailureDiagnostics( compilation ) );
  }

  /**
   * Verify the supplied Compilation was a failure.
   *
   * @param compilation the compilation to verify.
   */
  protected final void assertCompilationUnsuccessful( @Nonnull final Compilation compilation )
  {
    assertFalse( compilation.success(), compilation + " - " + describeFailureDiagnostics( compilation ) );
  }

  /**
   * Returns a description of the why the compilation failed.
   *
   * @param compilation the compilation.
   * @return a description of diagnostics for compilation.
   */
  @Nonnull
  protected final String describeFailureDiagnostics( @Nonnull final Compilation compilation )
  {
    final List<Diagnostic<? extends JavaFileObject>> diagnostics = compilation.diagnostics();
    if ( diagnostics.isEmpty() )
    {
      return "Compilation produced no diagnostics.\n";
    }
    final StringBuilder message = new StringBuilder( "Compilation produced the following diagnostics:\n" );
    diagnostics.forEach( diagnostic -> message.append( diagnostic ).append( '\n' ) );
    return message.toString();
  }

  /**
   * Build a classpath including the paths specified as well as the current classpath.
   * The current classpath is discovered by inspecting the current classloader, assuming it is a URLClassLoader
   * and walking back to the system classloader adding paths as required.
   *
   * @param paths the additional user supplied paths to add to classpath.
   * @return an list of directories that define the created classpath.
   */
  @Nonnull
  protected final List<File> buildClasspath( @Nonnull final File... paths )
  {
    final Set<File> elements = new LinkedHashSet<>( Arrays.asList( paths ) );
    ClassLoader classloader = getClass().getClassLoader();
    while ( true )
    {
      if ( classloader == ClassLoader.getSystemClassLoader() )
      {
        final String[] baseClassPathElements =
          System.getProperty( "java.class.path" ).split( System.getProperty( "path.separator" ) );
        for ( final String element : baseClassPathElements )
        {
          elements.add( new File( element ) );
        }
        break;
      }
      assert classloader instanceof URLClassLoader;
      // We only know how to extract elements from URLClassloaders.
      for ( final URL url : ( (URLClassLoader) classloader ).getURLs() )
      {
        assert url.getProtocol().equals( "file" );
        elements.add( new File( url.getPath() ) );
      }
      classloader = classloader.getParent();
    }

    return elements.stream().toList();
  }

  @Nonnull
  protected final Compilation assertCompilesWithoutErrors( @Nonnull final List<JavaFileObject> inputs )
  {
    final Compilation compilation = compile( inputs );
    assertCompilationSuccessful( compilation );
    assertDiagnosticCount( compilation, Diagnostic.Kind.ERROR, 0 );
    return compilation;
  }

  @Nonnull
  protected final Compilation assertCompilesWithoutErrors( @Nonnull final String classname )
  {
    return assertCompilesWithoutErrors( inputs( classname ) );
  }

  @Nonnull
  protected final List<JavaFileObject> inputs( @Nonnull final String... classnames )
  {
    return Stream.of( classnames ).map( this::input ).toList();
  }

  @Nonnull
  protected final JavaFileObject input( @Nonnull final String classname )
  {
    return input( "input", classname );
  }

  @Nonnull
  protected final JavaFileObject input( @Nonnull final String dir, @Nonnull final String classname )
  {
    return fixture( dir + File.separator + toFilename( classname ) );
  }

  @Nonnull
  protected final Compilation assertCompilesWithoutWarnings( @Nonnull final String classname )
  {
    return assertCompilesWithoutWarnings( inputs( classname ) );
  }

  @Nonnull
  protected final Compilation assertCompilesWithoutWarnings( @Nonnull final List<JavaFileObject> inputs )
  {
    return CompileTestUtil.assertCompilesWithoutWarnings( inputs, getOptions(), processors(), Collections.emptyList() );
  }

  @SuppressWarnings( "SameParameterValue" )
  @Nonnull
  protected final Processor newSynthesizingProcessor( @Nonnull final String classname, final int targetRound )
    throws IOException
  {
    return newSynthesizingProcessor( "input", classname, targetRound );
  }

  @Nonnull
  protected final Processor newSynthesizingProcessor( @Nonnull final String dir,
                                                      @Nonnull final String classname,
                                                      final int targetRound )
    throws IOException
  {
    final Path path = fixtureDir().resolve( dir ).resolve( toFilename( classname ) );
    final String source = Files.readString( path );
    return new SynthesizingProcessor( classname, source, targetRound );
  }

  protected final void assertCompilesWithSingleWarning( @Nonnull final String classname,
                                                        @Nonnull final String messageFragment )
  {
    final Compilation compilation = assertCompilesWithoutErrors( classname );
    assertWarningDiagnostic( compilation, messageFragment );
    assertDiagnosticCount( compilation, Diagnostic.Kind.WARNING, 1 );
  }

  @Nonnull
  protected final List<Diagnostic<? extends JavaFileObject>> assertDiagnosticCount( @Nonnull final Compilation compilation,
                                                                                    @Nonnull final Diagnostic.Kind kind,
                                                                                    final int count )
  {
    final List<Diagnostic<? extends JavaFileObject>> diagnostics = getDiagnostics( compilation, kind );
    assertEquals( diagnostics.size(), count );
    return diagnostics;
  }

  @Nonnull
  private List<Diagnostic<? extends JavaFileObject>> getDiagnostics( @Nonnull final Compilation compilation,
                                                                     @Nonnull final Diagnostic.Kind kind )
  {
    return compilation.diagnostics().stream().filter( d -> d.getKind() == kind ).toList();
  }

  protected final void assertFailedCompile( @Nonnull final String classname,
                                            @Nonnull final String errorMessageFragment )
  {
    assertFailedCompileResource( "bad_input/" + toFilename( classname ), errorMessageFragment );
  }

  @Nonnull
  protected final String toFilename( @Nonnull final String classname )
  {
    return toFilename( classname, "", ".java" );
  }

  @Nonnull
  protected final String toFilename( @Nonnull final String classname,
                                     @Nonnull final String prefix,
                                     @Nonnull final String postfix )
  {
    final String[] elements = classname.contains( "." ) ? classname.split( "\\." ) : new String[]{ classname };
    final StringBuilder input = new StringBuilder();
    for ( int i = 0; i < elements.length; i++ )
    {
      final boolean lastElement = i == elements.length - 1;
      if ( 0 != i )
      {
        input.append( '/' );
      }
      if ( lastElement )
      {
        input.append( prefix );
      }
      input.append( elements[ i ] );
      if ( lastElement )
      {
        input.append( postfix );
      }
    }
    return input.toString();
  }

  protected final void assertFailedCompileResource( @Nonnull final String inputResource,
                                                    @Nonnull final String errorMessageFragment )
  {
    assertFailedCompileResource( Collections.singletonList( fixture( inputResource ) ), errorMessageFragment );
  }

  protected final void assertFailedCompileResource( @Nonnull final List<JavaFileObject> inputs,
                                                    @Nonnull final String errorMessageFragment )
  {
    final Compilation compilation = compile( inputs );
    assertFalse( compilation.success() );
    assertDiagnostic( compilation, Diagnostic.Kind.ERROR, errorMessageFragment );
  }

  /**
   * Assert a diagnostic message is present for the compilation.
   *
   * @param compilation the compilation to check for the diagnostic.
   * @param message     the diagnostic message.
   * @deprecated Use assertDiagnostic instead.
   */
  @Deprecated
  protected final void assertDiagnosticPresent( @Nonnull final Compilation compilation,
                                                @Nonnull final String message )
  {
    for ( final Diagnostic<? extends JavaFileObject> diagnostic : compilation.diagnostics() )
    {
      if ( diagnostic.getMessage( Locale.getDefault() ).contains( message ) )
      {
        return;
      }
    }
    fail( "Failed to find diagnostic containing message:\n" + message +
          "\nActual diagnostics:\n" + describeFailureDiagnostics( compilation ) );
  }

  /**
   * Assert an error diagnostic message is present for the compilation.
   *
   * @param compilation the compilation to check for the diagnostic.
   * @param message     the diagnostic message.
   */
  protected final void assertErrorDiagnostic( @Nonnull final Compilation compilation, @Nonnull final String message )
  {
    assertDiagnostic( compilation, Diagnostic.Kind.ERROR, message );
  }

  /**
   * Assert a warning diagnostic message is present for the compilation.
   *
   * @param compilation the compilation to check for the diagnostic.
   * @param message     the diagnostic message.
   */
  protected final void assertWarningDiagnostic( @Nonnull final Compilation compilation,
                                                @Nonnull final String message )
  {
    assertDiagnostic( compilation, Diagnostic.Kind.WARNING, message );
  }

  /**
   * Assert a diagnostic message is present for the compilation.
   *
   * @param compilation the compilation to check for the diagnostic.
   * @param kind        the kind of diagnostic.
   * @param message     the diagnostic message.
   */
  protected final void assertDiagnostic( @Nonnull final Compilation compilation,
                                         @Nonnull final Diagnostic.Kind kind,
                                         @Nonnull final String message )
  {
    for ( final Diagnostic<? extends JavaFileObject> diagnostic : compilation.diagnostics() )
    {
      if ( diagnostic.getKind() == kind && diagnostic.getMessage( Locale.getDefault() ).contains( message ) )
      {
        return;
      }
    }
    fail( "Failed to find diagnostic of kind " + kind + " containing message:\n" + message +
          "\nActual diagnostics:\n" + describeFailureDiagnostics( compilation ) );
  }

  @Nonnull
  protected final JavaFileObject fixture( @Nonnull final String filename )
  {
    final Path path = fixtureDir().resolve( filename );
    if ( !Files.exists( path ) )
    {
      fail( "Fixture " + path + " does not exist." );
    }
    final StandardJavaFileManager standardFileManager =
      ToolProvider.getSystemJavaCompiler().getStandardFileManager( null, null, null );
    return standardFileManager.getJavaFileObjects( path ).iterator().next();
  }

  @Nonnull
  protected final Compilation compile( @Nonnull final List<JavaFileObject> inputs )
  {
    return compile( inputs, Collections.emptyList() );
  }

  @Nonnull
  protected final Compilation compile( @Nonnull final List<JavaFileObject> inputs,
                                       @Nonnull final Collection<? extends File> classpath )
  {
    return CompileTestUtil.compile( inputs, getOptions(), processors(), classpath );
  }

  @Nonnull
  protected final Path fixtureDir()
  {
    final String key = getOptionPrefix() + getFixtureKeyPart() + ".fixture_dir";
    final String fixtureDir = System.getProperty( key );
    assertNotNull( fixtureDir, "Expected System.getProperty( \"" + key + "\" ) to return fixture directory" );
    return new File( fixtureDir ).toPath();
  }

  protected final boolean outputFiles()
  {
    return System.getProperty( getOptionPrefix() + ".output_fixture_data", "false" ).equals( "true" );
  }
}
