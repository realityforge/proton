package org.realityforge.proton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nonnull;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public final class ResourceUtilTest
{
  @Test
  public void writeResourceWritesUtf8Content()
    throws Exception
  {
    final CapturingResource resource = new CapturingResource();
    final Element element = TestUtil.proxy( Element.class, ( self, method, args ) -> TestUtil.unsupported( method ) );

    ResourceUtil.writeResource( processingEnvironment( "metadata.txt", element, resource.asFileObject() ),
                                "metadata.txt",
                                "Hello \u00B5",
                                element );

    assertEquals( resource.content(), "Hello \u00B5" );
    assertFalse( resource.isDeleted() );
  }

  @Test
  public void writeResourceDeletesResourceWhenWriteFails()
  {
    final IOException failure = new IOException( "Write failed" );
    final FailingResource resource = new FailingResource( failure );
    final Element element = TestUtil.proxy( Element.class, ( self, method, args ) -> TestUtil.unsupported( method ) );

    try
    {
      ResourceUtil.writeResource( processingEnvironment( "metadata.txt", element, resource.asFileObject() ),
                                  "metadata.txt",
                                  "content",
                                  element );
      fail( "Expected IOException" );
    }
    catch ( final IOException e )
    {
      assertSame( e, failure );
    }
    assertTrue( resource.isDeleted() );
  }

  @Test
  public void writeResourceDeletesResourceWhenOpenFails()
  {
    final IOException failure = new IOException( "Open failed" );
    final FailingOpenResource resource = new FailingOpenResource( failure );
    final Element element = TestUtil.proxy( Element.class, ( self, method, args ) -> TestUtil.unsupported( method ) );

    try
    {
      ResourceUtil.writeResource( processingEnvironment( "metadata.txt", element, resource.asFileObject() ),
                                  "metadata.txt",
                                  "content",
                                  element );
      fail( "Expected IOException" );
    }
    catch ( final IOException e )
    {
      assertSame( e, failure );
    }
    assertTrue( resource.isDeleted() );
  }

  @SuppressWarnings( "SameParameterValue" )
  @Nonnull
  private static ProcessingEnvironment processingEnvironment( @Nonnull final String filename,
                                                              @Nonnull final Element element,
                                                              @Nonnull final FileObject fileObject )
  {
    return TestUtil.proxy( ProcessingEnvironment.class, ( self, method, args ) -> {
      if ( "getFiler".equals( method.getName() ) )
      {
        return filer( filename, element, fileObject );
      }
      return TestUtil.unsupported( method );
    } );
  }

  @Nonnull
  private static Filer filer( @Nonnull final String filename,
                              @Nonnull final Element element,
                              @Nonnull final FileObject fileObject )
  {
    return TestUtil.proxy( Filer.class, ( self, method, args ) -> {
      if ( "createResource".equals( method.getName() ) )
      {
        assertSame( args[ 0 ], StandardLocation.CLASS_OUTPUT );
        assertEquals( args[ 1 ], "" );
        assertEquals( args[ 2 ], filename );
        final Element[] originatingElements = (Element[]) args[ 3 ];
        assertEquals( originatingElements.length, 1 );
        assertSame( originatingElements[ 0 ], element );
        return fileObject;
      }
      return TestUtil.unsupported( method );
    } );
  }

  private static final class CapturingResource
  {
    @Nonnull
    private final ByteArrayOutputStream _outputStream = new ByteArrayOutputStream();
    private boolean _deleted;

    @Nonnull
    FileObject asFileObject()
    {
      return TestUtil.proxy( FileObject.class, ( self, method, args ) -> {
        if ( "openOutputStream".equals( method.getName() ) )
        {
          return _outputStream;
        }
        else if ( "delete".equals( method.getName() ) )
        {
          _deleted = true;
          return true;
        }
        return TestUtil.unsupported( method );
      } );
    }

    @Nonnull
    String content()
    {
      return _outputStream.toString( StandardCharsets.UTF_8 );
    }

    boolean isDeleted()
    {
      return _deleted;
    }
  }

  private static final class FailingResource
  {
    @Nonnull
    private final IOException _failure;
    private boolean _deleted;

    FailingResource( @Nonnull final IOException failure )
    {
      _failure = failure;
    }

    @Nonnull
    FileObject asFileObject()
    {
      return TestUtil.proxy( FileObject.class, ( self, method, args ) -> {
        if ( "openOutputStream".equals( method.getName() ) )
        {
          return new OutputStream()
          {
            @Override
            public void write( final int b )
              throws IOException
            {
              throw _failure;
            }

            @Override
            public void write( final byte[] b, final int off, final int len )
              throws IOException
            {
              throw _failure;
            }
          };
        }
        else if ( "delete".equals( method.getName() ) )
        {
          _deleted = true;
          return true;
        }
        return TestUtil.unsupported( method );
      } );
    }

    boolean isDeleted()
    {
      return _deleted;
    }
  }

  private static final class FailingOpenResource
  {
    @Nonnull
    private final IOException _failure;
    private boolean _deleted;

    FailingOpenResource( @Nonnull final IOException failure )
    {
      _failure = failure;
    }

    @Nonnull
    FileObject asFileObject()
    {
      return TestUtil.proxy( FileObject.class, ( self, method, args ) -> {
        if ( "openOutputStream".equals( method.getName() ) )
        {
          throw _failure;
        }
        else if ( "delete".equals( method.getName() ) )
        {
          _deleted = true;
          return true;
        }
        return TestUtil.unsupported( method );
      } );
    }

    boolean isDeleted()
    {
      return _deleted;
    }
  }
}
