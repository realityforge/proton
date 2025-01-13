package org.realityforge.proton.qa;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import static java.nio.charset.StandardCharsets.*;
import static org.testng.Assert.*;

public final class CompileTestUtil
{
  private CompileTestUtil()
  {
  }

  public static void assertNoWarnings( @Nonnull final List<Diagnostic<? extends JavaFileObject>> diagnostics )
  {
    final List<Diagnostic<? extends JavaFileObject>> warnings =
      diagnostics.stream()
        .filter( d -> Diagnostic.Kind.WARNING == d.getKind() || Diagnostic.Kind.MANDATORY_WARNING == d.getKind() )
        .toList();
    if ( !warnings.isEmpty() )
    {
      fail( "Unexpected warnings:\n" + asMessage( warnings ) );
    }
  }

  public static void assertNoErrors( @Nonnull final List<Diagnostic<? extends JavaFileObject>> diagnostics )
  {
    final List<Diagnostic<? extends JavaFileObject>> errors =
      diagnostics.stream().filter( d -> Diagnostic.Kind.ERROR == d.getKind() ).toList();
    if ( !errors.isEmpty() )
    {
      fail( "Unexpected errors:\n" + asMessage( errors ) );
    }
  }

  @Nonnull
  public static String asMessage( @Nonnull final List<Diagnostic<? extends JavaFileObject>> diagnostics )
  {
    return diagnostics.stream().map( Object::toString ).collect( Collectors.joining( "\n" ) );
  }

  @SuppressWarnings( "resource" )
  @Nonnull
  public static Compilation compile( @Nonnull final List<JavaFileObject> inputs,
                                     @Nonnull final Iterable<String> options,
                                     @Nonnull final Iterable<? extends Processor> processors,
                                     @Nonnull final Collection<? extends File> classpath )
  {
    try
    {
      final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
      final DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
      final StandardJavaFileManager fileManager =
        javaCompiler.getStandardFileManager( diagnosticCollector, Locale.getDefault(), UTF_8 );

      final Path sourceOutput = Files.createTempDirectory( "compile" );
      final Path classOutput = Files.createTempDirectory( "compile" );
      fileManager.setLocation( StandardLocation.SOURCE_OUTPUT, Collections.singletonList( sourceOutput.toFile() ) );
      fileManager.setLocation( StandardLocation.CLASS_OUTPUT, Collections.singletonList( classOutput.toFile() ) );
      if ( !classpath.isEmpty() )
      {
        fileManager.setLocation( StandardLocation.CLASS_PATH, classpath );
      }

      final JavaCompiler.CompilationTask task =
        javaCompiler.getTask( null,
                              fileManager,
                              diagnosticCollector,
                              options,
                              Collections.emptySet(),
                              inputs );
      task.setProcessors( processors );
      final Boolean success = task.call();
      final List<String> createdSourceFiles =
        Files
          .walk( sourceOutput )
          .filter( Files::isRegularFile )
          .map( sourceOutput::relativize )
          .map( Path::toString )
          .toList();
      final List<String> classOutputFilenames =
        Files
          .walk( classOutput )
          .filter( Files::isRegularFile )
          .map( classOutput::relativize )
          .map( Path::toString )
          .toList();

      return new Compilation( success,
                              sourceOutput,
                              createdSourceFiles,
                              classOutput,
                              classOutputFilenames,
                              diagnosticCollector.getDiagnostics() );
    }
    catch ( final IOException ioe )
    {
      //noinspection CallToPrintStackTrace
      ioe.printStackTrace();
      throw new AssertionError( "Unexpected io exception " + ioe, ioe );
    }
  }

  @Nonnull
  public static Compilation assertCompilesWithoutWarnings( @Nonnull final List<JavaFileObject> inputs,
                                                           @Nonnull final Iterable<String> options,
                                                           @Nonnull final Iterable<? extends Processor> processors,
                                                           @Nonnull final Collection<? extends File> classpath )
  {
    final Compilation results = compile( inputs, options, processors, classpath );
    assertNoErrors( results.diagnostics() );
    assertNoWarnings( results.diagnostics() );
    assertTrue( results.success() );
    return results;
  }

  /**
   * Output the specified file to target direct.
   *
   * @param file      the file to emit relative to outputDir.
   * @param sourceDir the outputDir directory
   * @param targetDir the target directory
   * @throws IOException if an error occurs writing the file or creating the directory.
   */
  public static void outputFile( @Nonnull final String file,
                                 @Nonnull final Path sourceDir,
                                 @Nonnull final Path targetDir )
    throws IOException
  {
    final Path source = sourceDir.resolve( file );
    final Path target = targetDir.resolve( file );
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
    if ( !Files.exists( target ) || !doesSourceMatchTarget( source, target ) )
    {
      Files.copy( source, target, StandardCopyOption.REPLACE_EXISTING );
    }
  }

  private static boolean doesSourceMatchTarget( @Nonnull final Path source, @Nonnull final Path target )
    throws IOException
  {
    try ( InputStream generated = new FileInputStream( source.toFile() ) )
    {
      final byte[] existing = Files.readAllBytes( target );
      final byte[] data = new byte[ generated.available() ];
      assertEquals( generated.read( data ), data.length );
      return Arrays.equals( existing, data );
    }
  }

  public static void assertSourceMatchesTarget( @Nonnull final Path source, @Nonnull final Path target )
    throws IOException
  {
    try ( InputStream generated = new FileInputStream( source.toFile() ) )
    {
      final byte[] existing = Files.readAllBytes( target );
      final byte[] data = new byte[ generated.available() ];
      assertEquals( generated.read( data ), data.length );
      assertEquals( existing, data );
    }
  }

  public static void outputFiles( @Nonnull final Compilation compilation, @Nonnull final Path targetDir )
    throws IOException
  {
    outputFiles( compilation.classOutputFilenames(), compilation.classOutput(), targetDir );
  }

  public static void outputFiles( @Nonnull final List<String> files,
                                  @Nonnull final Path sourceDir,
                                  @Nonnull final Path targetDir )
    throws IOException
  {
    for ( final String file : files )
    {
      outputFile( file, sourceDir, targetDir );
    }
  }
}
