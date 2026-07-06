package org.realityforge.proton;

import java.lang.reflect.Method;
import javax.annotation.Nonnull;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public final class DeferredElementSetTest
{
  @Test
  public void deferElementAddsUniqueElements()
  {
    final DeferredElementSet set = new DeferredElementSet();
    final TypeElement element = typeElement( "com.example.Widget" );

    set.deferElement( element );
    set.deferElement( element );

    assertEquals( set.getDeferred().size(), 1 );
    assertTrue( set.getDeferred().contains( element ) );
  }

  @Test
  public void clearRemovesDeferredElements()
  {
    final DeferredElementSet set = new DeferredElementSet();
    set.deferElement( typeElement( "com.example.Widget" ) );

    set.clear();

    assertTrue( set.getDeferred().isEmpty() );
  }

  @Test
  public void extractDeferredResolvesCurrentTypeElementsAndClearsSet()
  {
    final DeferredElementSet set = new DeferredElementSet();
    final TypeElement original = typeElement( "com.example.Widget" );
    final TypeElement resolved = typeElement( "com.example.Widget" );
    set.deferElement( original );

    assertEquals( set.extractDeferred( processingEnvironment( "com.example.Widget", resolved ) ).size(), 1 );
    set.deferElement( original );
    assertSame( set.extractDeferred( processingEnvironment( "com.example.Widget", resolved ) ).get( 0 ), resolved );
    assertTrue( set.getDeferred().isEmpty() );
  }

  @Nonnull
  private static ProcessingEnvironment processingEnvironment( @Nonnull final String qualifiedName,
                                                              @Nonnull final TypeElement resolved )
  {
    final Elements elements = TestUtil.proxy( Elements.class, ( self, method, args ) -> {
      if ( "getTypeElement".equals( method.getName() ) )
      {
        assertEquals( args[ 0 ].toString(), qualifiedName );
        return resolved;
      }
      return TestUtil.unsupported( method );
    } );
    return TestUtil.proxy( ProcessingEnvironment.class, ( self, method, args ) -> {
      if ( "getElementUtils".equals( method.getName() ) )
      {
        return elements;
      }
      return TestUtil.unsupported( method );
    } );
  }

  @Nonnull
  private static TypeElement typeElement( @Nonnull final String qualifiedName )
  {
    return TestUtil.proxy( TypeElement.class, ( self, method, args ) -> invokeTypeElement( method, qualifiedName ) );
  }

  private static Object invokeTypeElement( @Nonnull final Method method, @Nonnull final String qualifiedName )
  {
    if ( "getQualifiedName".equals( method.getName() ) )
    {
      return TestUtil.name( qualifiedName );
    }
    return TestUtil.unsupported( method );
  }
}
