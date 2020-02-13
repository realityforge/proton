package org.realityforge.proton.qa;

import com.google.common.collect.ImmutableList;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompileTester;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import static com.google.common.truth.Truth.*;
import static org.testng.Assert.*;

public abstract class AbstractProcessorTest
{
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

  protected boolean emitGeneratedFile( @Nonnull final JavaFileObject target )
  {
    return JavaFileObject.Kind.CLASS != target.getKind();
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
                                                @Nonnull final List<String> outputs,
                                                @Nonnull final Predicate<JavaFileObject> filter )
    throws Exception
  {
    if ( outputFiles() )
    {
      final Compilation compilation = compiler().compile( inputs );

      final Compilation.Status status = compilation.status();
      if ( Compilation.Status.SUCCESS != status )
      {
        /*
         * Ugly hackery that marks the compile as successful so we can emit output onto filesystem. This could
         * result in java code that is not compilable emitted to the filesystem. This makes determining problems
         * a little easier even if it does make re-running tests from the IDE a little harder.
         */
        final Field field = compilation.getClass().getDeclaredField( "status" );
        field.setAccessible( true );
        field.set( compilation, Compilation.Status.SUCCESS );
      }

      outputFiles( compilation.generatedFiles(), fixtureDir().resolve( "expected" ), filter );

      if ( Compilation.Status.SUCCESS != status )
      {
        fail( describeFailureDiagnostics( compilation ) );
      }
    }
    final List<String> sourceFiles =
      outputs.stream().filter( o -> o.endsWith( ".java" ) ).collect( Collectors.toList() );
    final List<String> otherFiles =
      outputs.stream().filter( o -> !o.endsWith( ".java" ) ).collect( Collectors.toList() );
    final CompileTester.CleanCompilationClause clause = assertCompilesWithoutWarnings( inputs );
    if ( !sourceFiles.isEmpty() )
    {
      final JavaFileObject firstExpected = fixture( sourceFiles.get( 0 ) );
      final JavaFileObject[] restExpected =
        sourceFiles.stream().skip( 1 ).map( this::fixture ).toArray( JavaFileObject[]::new );
      clause.
        and().
        generatesSources( firstExpected, restExpected );
    }
    if ( !otherFiles.isEmpty() )
    {
      final JavaFileObject firstExpected = fixture( otherFiles.get( 0 ) );
      final JavaFileObject[] restExpected =
        otherFiles.stream().skip( 1 ).map( this::fixture ).toArray( JavaFileObject[]::new );
      clause.
        and().
        generatesFiles( firstExpected, restExpected );
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

  @Nonnull
  protected final Compiler compiler()
  {
    return Compiler.javac()
      .withProcessors( processors() )
      .withOptions( getOptions() );
  }

  /**
   * Verify the supplied Compilation was successful.
   *
   * @param compilation the compilation to verify.
   */
  protected final void assertCompilationSuccessful( @Nonnull final Compilation compilation )
  {
    assertEquals( compilation.status(),
                  Compilation.Status.SUCCESS,
                  compilation.toString() + " - " + describeFailureDiagnostics( compilation ) );
  }

  /**
   * Verify the supplied Compilation was a failure.
   *
   * @param compilation the compilation to verify.
   */
  protected final void assertCompilationUnsuccessful( @Nonnull final Compilation compilation )
  {
    assertEquals( compilation.status(),
                  Compilation.Status.FAILURE,
                  compilation.toString() + " - " + describeFailureDiagnostics( compilation ) );
  }

  /**
   * Returns a description of the why the compilation failed.
   */
  @Nonnull
  protected final String describeFailureDiagnostics( @Nonnull final Compilation compilation )
  {
    final ImmutableList<Diagnostic<? extends JavaFileObject>> diagnostics = compilation.diagnostics();
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
  protected final ImmutableList<File> buildClasspath( @Nonnull final File... paths )
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

    return elements.stream().collect( ImmutableList.toImmutableList() );
  }

  protected final void outputFiles( @Nonnull final Collection<JavaFileObject> fileObjects,
                                    @Nonnull final Path targetDir )
    throws IOException
  {
    outputFiles( fileObjects, targetDir, f -> true );
  }

  protected final void outputFiles( @Nonnull final Collection<JavaFileObject> fileObjects,
                                    @Nonnull final Path targetDir,
                                    @Nonnull final Predicate<JavaFileObject> filter )
    throws IOException
  {
    for ( final JavaFileObject fileObject : fileObjects )
    {
      if ( filter.test( fileObject ) )
      {
        outputFile( fileObject, targetDir );
      }
    }
  }

  /**
   * Output the specified JavaFileObject to target direct.
   * The path relative to SOURCE_OUTPUT and CLASS_OUTPUT is included when emitting the file.
   *
   * @param fileObject the object to emit.
   * @param targetDir  the target directory
   * @throws IOException if an error occurs writing the file or creating the directory.
   */
  protected final void outputFile( @Nonnull final JavaFileObject fileObject, @Nonnull final Path targetDir )
    throws IOException
  {
    final String filename =
      fileObject.getName().replace( "/SOURCE_OUTPUT/", "" ).replace( "/CLASS_OUTPUT/", "" );
    final Path target = targetDir.resolve( filename );
    final File dir = target.getParent().toFile();
    if ( !dir.exists() )
    {
      assertTrue( dir.mkdirs() );
    }
    /*
     * If the data on the filesystem is identical to data generated then do not write
     * to filesystem. The writing can be slow and it can also trigger the IDE or other
     * tools to recompile code which is problematic.
     */
    if ( !Files.exists( target ) || !doesTargetFileMatchGenerated( fileObject, target ) )
    {
      Files.copy( fileObject.openInputStream(), target, StandardCopyOption.REPLACE_EXISTING );
    }
  }

  private boolean doesTargetFileMatchGenerated( @Nonnull final JavaFileObject fileObject, @Nonnull final Path target )
    throws IOException
  {
    final byte[] existing = Files.readAllBytes( target );
    final InputStream generated = fileObject.openInputStream();
    final byte[] data = new byte[ generated.available() ];
    assertEquals( generated.read( data ), data.length );
    return Arrays.equals( existing, data );
  }

  @Nonnull
  protected final CompileTester assertCompiles( @Nonnull final List<JavaFileObject> inputs )
  {
    return assert_().about( JavaSourcesSubjectFactory.javaSources() ).
      that( inputs ).
      withCompilerOptions( getOptions() ).
      processedWith( processors() );
  }

  @Nonnull
  protected final CompileTester.SuccessfulCompilationClause assertCompilesWithoutErrors( @Nonnull final String classname )
  {
    return assertCompilesWithoutErrors( inputs( classname ) );
  }

  @Nonnull
  protected final List<JavaFileObject> inputs( @Nonnull final String... classnames )
  {
    return Stream.of( classnames ).map( classname -> input( "input", classname ) ).collect( Collectors.toList() );
  }

  @Nonnull
  protected final JavaFileObject input( @Nonnull final String dir, @Nonnull final String classname )
  {
    return fixture( toFilename( dir, classname ) );
  }

  @Nonnull
  protected final CompileTester.SuccessfulCompilationClause assertCompilesWithoutErrors( @Nonnull final List<JavaFileObject> inputs )
  {
    return assertCompiles( inputs ).compilesWithoutError();
  }

  @Nonnull
  protected final CompileTester.CleanCompilationClause assertCompilesWithoutWarnings( @Nonnull final String classname )
  {
    return assertCompilesWithoutWarnings( inputs( classname ) );
  }

  @Nonnull
  protected final CompileTester.CleanCompilationClause assertCompilesWithoutWarnings( @Nonnull final List<JavaFileObject> inputs )
  {
    return assertCompiles( inputs ).compilesWithoutWarnings();
  }

  protected final void assertCompilesWithSingleWarning( @Nonnull final String classname,
                                                        @Nonnull final String messageFragment )
  {
    assertCompilesWithoutErrors( classname ).
      withWarningCount( 1 ).
      withWarningContaining( messageFragment );
  }

  protected final void assertFailedCompile( @Nonnull final String classname,
                                            @Nonnull final String errorMessageFragment )
  {
    assertFailedCompileResource( toFilename( "bad_input", classname ), errorMessageFragment );
  }

  @Nonnull
  protected final String toFilename( @Nonnull final String dir, @Nonnull final String classname )
  {
    return toFilename( dir, classname, "", ".java" );
  }

  @Nonnull
  protected final String toFilename( @Nonnull final String dir,
                                     @Nonnull final String classname,
                                     @Nonnull final String prefix,
                                     @Nonnull final String postfix )
  {
    final String[] elements = classname.contains( "." ) ? classname.split( "\\." ) : new String[]{ classname };
    final StringBuilder input = new StringBuilder();
    input.append( dir );
    for ( int i = 0; i < elements.length; i++ )
    {
      final boolean lastElement = i == elements.length - 1;
      input.append( '/' );
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
    assert_().about( JavaSourcesSubjectFactory.javaSources() ).
      that( inputs ).
      withCompilerOptions( getOptions() ).
      processedWith( processors() ).
      failsToCompile().
      withErrorContaining( errorMessageFragment );
  }

  protected final void assertDiagnosticPresent( @Nonnull final Compilation compilation, @Nonnull final String message )
  {
    for ( final Diagnostic<? extends JavaFileObject> diagnostic : compilation.diagnostics() )
    {
      if ( diagnostic.getMessage( Locale.getDefault() ).contains( message ) )
      {
        return;
      }
    }
    fail( "Failed but missing expected message:\n" + message +
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
    try
    {
      return JavaFileObjects.forResource( path.toUri().toURL() );
    }
    catch ( final MalformedURLException e )
    {
      throw new IllegalStateException( e );
    }
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
