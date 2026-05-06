package org.realityforge.proton;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.NullType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import org.testng.annotations.Test;
import static org.testng.Assert.assertFalse;

public final class SuperficialValidationTest
{
  @Test
  public void isTypeOf_returnsFalseForNullType()
    throws Throwable
  {
    assertFalse( invokeIsTypeOf( String.class, new FakeNullType() ) );
  }

  private static boolean invokeIsTypeOf( final Class<?> clazz, final TypeMirror type )
    throws Throwable
  {
    final Method method = SuperficialValidation.class.getDeclaredMethod( "isTypeOf", Class.class, TypeMirror.class );
    method.setAccessible( true );
    try
    {
      return (boolean) method.invoke( null, clazz, type );
    }
    catch ( final InvocationTargetException ite )
    {
      throw ite.getCause();
    }
  }

  private static final class FakeNullType
    implements NullType
  {
    @Override
    public TypeKind getKind()
    {
      return TypeKind.NULL;
    }

    @Override
    public <R, P> R accept( final TypeVisitor<R, P> visitor, final P parameter )
    {
      return visitor.visitNull( this, parameter );
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

    @Override
    public String toString()
    {
      return "<nulltype>";
    }
  }
}
