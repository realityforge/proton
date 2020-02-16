package org.realityforge.proton;

import java.util.List;
import javax.annotation.Nonnull;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public final class TypesUtil
{
  private TypesUtil()
  {
  }

  public static boolean containsArrayType( @Nonnull final TypeMirror type )
  {
    if ( TypeKind.DECLARED == type.getKind() )
    {
      final DeclaredType declaredType = (DeclaredType) type;
      for ( final TypeMirror typeArgument : declaredType.getTypeArguments() )
      {
        if ( containsArrayType( typeArgument ) )
        {
          return true;
        }
      }
      return false;
    }
    else
    {
      return TypeKind.ARRAY == type.getKind();
    }
  }

  public static boolean containsRawType( @Nonnull final TypeMirror type )
  {
    if ( TypeKind.DECLARED == type.getKind() )
    {
      final DeclaredType declaredType = (DeclaredType) type;
      final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
      if ( typeArguments.isEmpty() && !( (TypeElement) declaredType.asElement() ).getTypeParameters().isEmpty() )
      {
        return true;
      }
      else
      {
        for ( final TypeMirror typeArgument : typeArguments )
        {
          if ( containsRawType( typeArgument ) )
          {
            return true;
          }
        }
        return false;
      }
    }
    else
    {
      return false;
    }
  }

  public static boolean containsWildcard( @Nonnull final TypeMirror type )
  {
    if ( TypeKind.WILDCARD == type.getKind() )
    {
      return true;
    }
    else if ( TypeKind.DECLARED == type.getKind() )
    {
      final DeclaredType declaredType = (DeclaredType) type;
      for ( final TypeMirror typeArgument : declaredType.getTypeArguments() )
      {
        if ( containsWildcard( typeArgument ) )
        {
          return true;
        }
      }
      return false;
    }
    else
    {
      return false;
    }
  }
}
