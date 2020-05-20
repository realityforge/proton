package org.realityforge.proton;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

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

  public static boolean hasRawTypes( @Nonnull final ProcessingEnvironment processingEnv,
                                     @Nonnull final TypeMirror type )
  {
    final TypeKind kind = type.getKind();
    if ( TypeKind.TYPEVAR == kind )
    {
      final TypeVariable typeVariable = (TypeVariable) type;
      return hasRawTypes( processingEnv, typeVariable.getLowerBound() ) ||
             hasRawTypes( processingEnv, typeVariable.getUpperBound() );
    }
    else if ( TypeKind.ARRAY == kind )
    {
      return hasRawTypes( processingEnv, ( (ArrayType) type ).getComponentType() );
    }
    else if ( TypeKind.DECLARED == kind )
    {
      final DeclaredType declaredType = (DeclaredType) type;
      final int typeArgumentCount = declaredType.getTypeArguments().size();
      final TypeElement typeElement = (TypeElement) processingEnv.getTypeUtils().asElement( type );
      if ( typeArgumentCount != typeElement.getTypeParameters().size() )
      {
        return true;
      }
      else
      {
        return declaredType
          .getTypeArguments()
          .stream()
          .anyMatch( t -> hasRawTypes( processingEnv, t ) );
      }
    }
    else if ( TypeKind.EXECUTABLE == kind )
    {
      final ExecutableType executableType = (ExecutableType) type;
      return hasRawTypes( processingEnv, executableType.getReturnType() ) ||
             executableType.getTypeVariables()
               .stream()
               .anyMatch( t -> hasRawTypes( processingEnv, t ) ) ||
             executableType.getThrownTypes()
               .stream()
               .anyMatch( t -> hasRawTypes( processingEnv, t ) ) ||
             executableType.getParameterTypes()
               .stream()
               .anyMatch( t -> hasRawTypes( processingEnv, t ) );
    }
    else
    {
      return false;
    }
  }
}
