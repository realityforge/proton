package org.realityforge.proton.qa;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.testng.annotations.Test;

public final class CompilationTest
{
  @Test
  public void assertionHelpersMatchSourceAndClassOutputs()
  {
    final Compilation compilation =
      compilation( List.of( "com/example/GeneratedModel.java", "com/example/metadata.json" ),
                   List.of( "com/example/Model.class",
                            "com/example/GeneratedModel.class",
                            "META-INF/services/javax.annotation.processing.Processor" ) );

    compilation.assertJavaFileCount( 1 );
    compilation.assertSourceOutputFilenameCount( f -> f.startsWith( "com/example/" ), 2 );
    compilation.assertJavaSourcePresent( "com.example.GeneratedModel" );
    compilation.assertSourceOutputFilenamePresent( "com/example/metadata.json" );

    compilation.assertClassFileCount( 2 );
    compilation.assertClassOutputFilenameCount( f -> f.endsWith( ".class" ), 2 );
    compilation.assertJavaClassPresent( "com.example.Model" );
    compilation.assertClassOutputFilenamePresent( "META-INF/services/javax.annotation.processing.Processor" );
  }

  @Test( expectedExceptions = AssertionError.class )
  public void constructorRejectsNullDiagnostics()
  {
    new Compilation( true, Path.of( "source" ), List.of(), Path.of( "classes" ), List.of(), null );
  }

  @Nonnull
  private static Compilation compilation( @Nonnull final List<String> sourceOutputFilenames,
                                          @Nonnull final List<String> classOutputFilenames )
  {
    return new Compilation( true,
                            Path.of( "source" ),
                            sourceOutputFilenames,
                            Path.of( "classes" ),
                            classOutputFilenames,
                            diagnostics() );
  }

  @Nonnull
  private static List<Diagnostic<? extends JavaFileObject>> diagnostics()
  {
    return Collections.emptyList();
  }
}
