package org.realityforge.proton;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public final class NamesUtilTest
{
  private static final Pattern GETTER_PATTERN = Pattern.compile( "^get(.*)$" );
  private static final Pattern ISSER_PATTERN = Pattern.compile( "^is(.*)$" );
  private static final String SENTINEL = "<default>";

  @Test
  public void isJavaIdentifier()
  {
    assertTrue( NamesUtil.isJavaIdentifier( "name" ) );
    assertTrue( NamesUtil.isJavaIdentifier( "_name1" ) );
    assertFalse( NamesUtil.isJavaIdentifier( "bad-name" ) );
    assertFalse( NamesUtil.isJavaIdentifier( "class" ) );
  }

  @Test
  public void firstCharacterToLowerCase()
  {
    assertEquals( NamesUtil.firstCharacterToLowerCase( "Name" ), "name" );
    assertEquals( NamesUtil.firstCharacterToLowerCase( "URLValue" ), "uRLValue" );
  }

  @Test
  public void deriveName()
  {
    assertEquals( NamesUtil.deriveName( executableElement( "getURLValue", TypeKind.DECLARED ),
                                        GETTER_PATTERN,
                                        SENTINEL,
                                        SENTINEL ),
                  "uRLValue" );
    assertEquals( NamesUtil.deriveName( executableElement( "getName", TypeKind.DECLARED ),
                                        GETTER_PATTERN,
                                        "declaredName",
                                        SENTINEL ),
                  "declaredName" );
    assertNull( NamesUtil.deriveName( executableElement( "fetchName", TypeKind.DECLARED ),
                                      GETTER_PATTERN,
                                      SENTINEL,
                                      SENTINEL ) );
  }

  @Test
  public void getPropertyAccessorName()
  {
    assertEquals( NamesUtil.getPropertyAccessorName( executableElement( "getName", TypeKind.DECLARED ),
                                                     GETTER_PATTERN,
                                                     ISSER_PATTERN,
                                                     SENTINEL,
                                                     SENTINEL ),
                  "name" );
    assertEquals( NamesUtil.getPropertyAccessorName( executableElement( "isReady", TypeKind.BOOLEAN ),
                                                     GETTER_PATTERN,
                                                     ISSER_PATTERN,
                                                     SENTINEL,
                                                     SENTINEL ),
                  "ready" );
    assertEquals( NamesUtil.getPropertyAccessorName( executableElement( "isReady", TypeKind.DECLARED ),
                                                     GETTER_PATTERN,
                                                     ISSER_PATTERN,
                                                     SENTINEL,
                                                     SENTINEL ),
                  "isReady" );
    assertEquals( NamesUtil.getPropertyAccessorName( executableElement( "fetchName", TypeKind.DECLARED ),
                                                     GETTER_PATTERN,
                                                     ISSER_PATTERN,
                                                     "declaredName",
                                                     SENTINEL ),
                  "declaredName" );
  }

  @Test
  public void constantCaseToLowerCamel()
  {
    assertEquals( NamesUtil.constantCaseToLowerCamel( "FOO" ), "foo" );
    assertEquals( NamesUtil.constantCaseToLowerCamel( "FOO_BAR" ), "fooBar" );
    assertEquals( NamesUtil.constantCaseToLowerCamel( "FOO__BAR_" ), "fooBar" );
    assertEquals( NamesUtil.constantCaseToLowerCamel( "URL_VALUE" ), "urlValue" );
  }

  @Test
  public void constantCaseToLowerCamelIgnoresDefaultLocale()
  {
    final Locale defaultLocale = Locale.getDefault();
    try
    {
      Locale.setDefault( Locale.forLanguageTag( "tr" ) );
      assertEquals( NamesUtil.constantCaseToLowerCamel( "INITIAL_ID" ), "initialId" );
    }
    finally
    {
      Locale.setDefault( defaultLocale );
    }
  }

  @Test
  public void extractName()
  {
    final ExecutableElement method = executableElement( "doStuff", TypeKind.DECLARED );
    assertEquals( NamesUtil.extractName( method,
                                         m -> m.getSimpleName().toString(),
                                         "com.example.Action",
                                         "name",
                                         SENTINEL,
                                         SENTINEL ),
                  "doStuff" );
    assertEquals( NamesUtil.extractName( method,
                                         m -> "ignored",
                                         "com.example.Action",
                                         "name",
                                         SENTINEL,
                                         "customName" ),
                  "customName" );
  }

  @Test
  public void extractNameRejectsInvalidDeclaredName()
  {
    final ExecutableElement method = executableElement( "doStuff", TypeKind.DECLARED );
    final ProcessorException exception;
    try
    {
      NamesUtil.extractName( method, m -> "ignored", "com.example.Action", "name", SENTINEL, "bad-name" );
      fail( "Expected ProcessorException" );
      return;
    }
    catch ( final ProcessorException pe )
    {
      exception = pe;
    }
    assertEquals( exception.getMessage(),
                  "@Action target specified an invalid value 'bad-name' for the parameter name. " +
                  "The value must be a valid java identifier" );
  }

  @Test
  public void extractNameRejectsKeywordDeclaredName()
  {
    final ExecutableElement method = executableElement( "doStuff", TypeKind.DECLARED );
    final ProcessorException exception;
    try
    {
      NamesUtil.extractName( method, m -> "ignored", "com.example.Action", "name", SENTINEL, "class" );
      fail( "Expected ProcessorException" );
      return;
    }
    catch ( final ProcessorException pe )
    {
      exception = pe;
    }
    assertEquals( exception.getMessage(),
                  "@Action target specified an invalid value 'class' for the parameter name. " +
                  "The value must not be a java keyword" );
  }

  @Test
  public void extractNameRejectsMissingDefault()
  {
    final ExecutableElement method = executableElement( "doStuff", TypeKind.DECLARED );
    final ProcessorException exception;
    try
    {
      NamesUtil.extractName( method, m -> null, "com.example.Action", "name", SENTINEL, SENTINEL );
      fail( "Expected ProcessorException" );
      return;
    }
    catch ( final ProcessorException pe )
    {
      exception = pe;
    }
    assertEquals( exception.getMessage(),
                  "@Action target did not specify the parameter name and the default value could not be derived" );
  }

  @Nonnull
  private static ExecutableElement executableElement( @Nonnull final String name, final TypeKind returnKind )
  {
    return proxy( ExecutableElement.class, ( self, method, args ) -> {
      if ( "getSimpleName".equals( method.getName() ) )
      {
        return name( name );
      }
      else if ( "getReturnType".equals( method.getName() ) )
      {
        return typeMirror( returnKind );
      }
      else if ( "getKind".equals( method.getName() ) )
      {
        return ElementKind.METHOD;
      }
      else
      {
        return unsupported( method );
      }
    } );
  }

  @Nonnull
  private static TypeMirror typeMirror( final TypeKind kind )
  {
    return new TypeMirror()
    {
      @Override
      public TypeKind getKind()
      {
        return kind;
      }

      @Override
      public <R, P> R accept( final TypeVisitor<R, P> visitor, final P parameter )
      {
        return visitor.visit( this, parameter );
      }

      @Override
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
      public <A extends Annotation> A[] getAnnotationsByType( final Class<A> annotationType )
      {
        @SuppressWarnings( "unchecked" )
        final A[] annotations = (A[]) Array.newInstance( annotationType, 0 );
        return annotations;
      }
    };
  }

  @Nonnull
  private static Name name( @Nonnull final String value )
  {
    return new Name()
    {
      @Override
      public boolean contentEquals( final CharSequence cs )
      {
        return value.contentEquals( cs );
      }

      @Override
      public int length()
      {
        return value.length();
      }

      @Override
      public char charAt( final int index )
      {
        return value.charAt( index );
      }

      @Override
      public CharSequence subSequence( final int start, final int end )
      {
        return value.subSequence( start, end );
      }

      @Nonnull
      @Override
      public String toString()
      {
        return value;
      }
    };
  }

  @Nonnull
  private static <T> T proxy( @Nonnull final Class<T> type, @Nonnull final ProxyInvocation invocation )
  {
    return type.cast( Proxy.newProxyInstance( type.getClassLoader(), new Class<?>[]{ type }, ( self, method, args ) -> {
      if ( "equals".equals( method.getName() ) )
      {
        return self == args[ 0 ];
      }
      else if ( "hashCode".equals( method.getName() ) )
      {
        return System.identityHashCode( self );
      }
      else if ( "toString".equals( method.getName() ) )
      {
        return type.getSimpleName() + "Proxy";
      }
      else
      {
        return invocation.invoke( self, method, args );
      }
    } ) );
  }

  private static Object unsupported( @Nonnull final Method method )
  {
    throw new UnsupportedOperationException( method.toString() );
  }

  private interface ProxyInvocation
  {
    Object invoke( @Nonnull Object self, @Nonnull Method method, Object[] args )
      throws Throwable;
  }
}
