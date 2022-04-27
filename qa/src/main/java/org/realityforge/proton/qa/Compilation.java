package org.realityforge.proton.qa;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import static org.testng.Assert.*;

public record Compilation(boolean success,
                          @Nonnull Path sourceOutput,
                          @Nonnull List<String> sourceOutputFilenames,
                          @Nonnull Path classOutput,
                          @Nonnull List<String> classOutputFilenames,
                          @Nonnull List<Diagnostic<? extends JavaFileObject>> diagnostics)
{
  public Compilation
  {
    assertNotNull( sourceOutput );
    assertNotNull( sourceOutputFilenames );
    assertNotNull( classOutput );
    assertNotNull( classOutputFilenames );
    assertNotNull( diagnostics );
  }

  public void assertJavaFileCount( final long count )
  {
    assertSourceOutputFilenameCount( f -> f.endsWith( ".java" ), count );
  }

  public void assertSourceOutputFilenameCount( @Nonnull final Predicate<String> predicate, final long count )
  {
    assertEquals( sourceOutputFilenames.stream().filter( predicate ).count(), count );
  }

  public void assertJavaSourcePresent( @Nonnull final String classname )
  {
    final String filename = classname.replace( ".", "/" ) + ".java";
    assertSourceOutputFilenamePresent( filename );
  }

  public void assertSourceOutputFilenamePresent( @Nonnull final String filename )
  {
    assertTrue( sourceOutputFilenames.stream().anyMatch( f -> f.equals( filename ) ),
                "Missing source output filename " + filename );
  }

  public void assertClassFileCount( final long count )
  {
    assertClassOutputFilenameCount( f -> f.endsWith( ".class" ), count );
  }

  public void assertClassOutputFilenameCount( @Nonnull final Predicate<String> predicate, final long count )
  {
    assertEquals( classOutputFilenames.stream().filter( predicate ).count(), count );
  }

  public void assertJavaClassPresent( @Nonnull final String classname )
  {
    final String filename = classname.replace( ".", "/" ) + ".class";
    assertClassOutputFilenamePresent( filename );
  }

  public void assertClassOutputFilenamePresent( @Nonnull final String filename )
  {
    assertTrue( classOutputFilenames.stream().anyMatch( f -> f.equals( filename ) ),
                "Missing source output filename " + filename );
  }
}
