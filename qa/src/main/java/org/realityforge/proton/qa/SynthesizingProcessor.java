package org.realityforge.proton.qa;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes( "*" )
@SupportedSourceVersion( SourceVersion.RELEASE_8 )
final class SynthesizingProcessor
  extends AbstractProcessor
{
  @Nonnull
  private final String _classname;
  @Nonnull
  private final String _source;
  private final int _targetRound;
  private int _round;

  SynthesizingProcessor( @Nonnull final String classname,
                         @Nonnull final String source,
                         final int targetRound )
  {
    _classname = Objects.requireNonNull( classname );
    _source = Objects.requireNonNull( source );
    _targetRound = targetRound;
  }

  @Override
  public boolean process( final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv )
  {
    try
    {
      if ( _targetRound == _round )
      {
        processingEnv.getMessager()
          .printMessage( Diagnostic.Kind.NOTE, "Synthesizing " + _classname + " in round " + _round );
        final JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile( _classname );
        try ( final Writer writer = sourceFile.openWriter() )
        {
          writer.write( _source );
        }
      }
    }
    catch ( final IOException e )
    {
      processingEnv.getMessager().printMessage( Diagnostic.Kind.ERROR, "Error occurred synthesizing files: " + e );
    }
    _round++;
    return false;
  }
}
