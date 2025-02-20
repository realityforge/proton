/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.realityforge.proton;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.AbstractElementVisitor14;
import javax.lang.model.util.SimpleAnnotationValueVisitor14;
import javax.lang.model.util.SimpleTypeVisitor14;

/**
 * A utility class that traverses {@link Element} instances and ensures that all type information
 * is present and resolvable.
 *
 * @author Gregory Kick
 */
public final class SuperficialValidation
{
  private SuperficialValidation()
  {
  }

  public static boolean validateElements( @Nonnull final ProcessingEnvironment processingEnv,
                                          @Nonnull final Iterable<? extends Element> elements )
  {
    return new ValidatorVisitors( processingEnv ).validateElements( elements );
  }

  public static boolean validateElement( @Nonnull final ProcessingEnvironment processingEnv,
                                         @Nonnull final Element element )
  {
    return new ValidatorVisitors( processingEnv ).validateElement( element );
  }

  public static boolean validateTypes( @Nonnull final ProcessingEnvironment processingEnv,
                                       @Nonnull final Iterable<? extends TypeMirror> types )
  {
    return new ValidatorVisitors( processingEnv ).validateTypes( types );
  }

  public static boolean validateType( @Nonnull final ProcessingEnvironment processingEnv,
                                      @Nonnull final TypeMirror type )
  {
    return new ValidatorVisitors( processingEnv ).validateType( type );
  }

  private static final class ValidatorVisitors
  {
    @Nonnull
    final TypeValidatingVisitor _typeValidatingVisitor;
    @Nonnull
    final ElementValidatingVisitor _elementValidatingVisitor;
    @Nonnull
    final ValueValidatingVisitor _valueValidatingVisitor;

    ValidatorVisitors( @Nonnull final ProcessingEnvironment processingEnv )
    {
      _typeValidatingVisitor = new TypeValidatingVisitor( this );
      _elementValidatingVisitor = new ElementValidatingVisitor( this );
      _valueValidatingVisitor = new ValueValidatingVisitor( this, processingEnv );
    }

    private boolean validateElement( @Nonnull final Element element )
    {
      return element.accept( _elementValidatingVisitor, null );
    }

    private boolean validateType( @Nonnull final TypeMirror type )
    {
      return type.accept( _typeValidatingVisitor, null );
    }

    private boolean validateElements( @Nonnull final Iterable<? extends Element> elements )
    {
      for ( final Element element : elements )
      {
        if ( !validateElement( element ) )
        {
          return false;
        }
      }
      return true;
    }

    private boolean validateTypes( @Nonnull final Iterable<? extends TypeMirror> types )
    {
      for ( final TypeMirror type : types )
      {
        if ( !validateType( type ) )
        {
          return false;
        }
      }
      return true;
    }
  }

  /**
   * Returns true if the raw type underlying the given {@link TypeMirror} represents the same raw
   * type as the given {@link Class} and throws an IllegalArgumentException if the {@link
   * TypeMirror} does not represent a type that can be referenced by a {@link Class}
   */
  private static boolean isTypeOf( @Nonnull final Class<?> clazz, @Nonnull final TypeMirror type )
  {
    return type.accept( new IsTypeOf( clazz ), null );
  }

  private static final class IsTypeOf
    extends SimpleTypeVisitor14<Boolean, Void>
  {
    @Nonnull
    private final Class<?> _clazz;

    IsTypeOf( @Nonnull final Class<?> clazz )
    {
      _clazz = Objects.requireNonNull( clazz );
    }

    @Override
    protected Boolean defaultAction( TypeMirror type, Void ignored )
    {
      throw new IllegalArgumentException( type + " cannot be represented as a Class<?>." );
    }

    @Override
    public Boolean visitNoType( NoType noType, Void p )
    {
      return TypeKind.VOID == noType.getKind() && Void.TYPE.equals( _clazz );
    }

    @Override
    public Boolean visitPrimitive( final PrimitiveType type, final Void p )
    {
      final TypeKind kind = type.getKind();
      switch ( kind )
      {
        case BOOLEAN:
          return Boolean.TYPE.equals( _clazz );
        case BYTE:
          return Byte.TYPE.equals( _clazz );
        case CHAR:
          return Character.TYPE.equals( _clazz );
        case DOUBLE:
          return Double.TYPE.equals( _clazz );
        case FLOAT:
          return Float.TYPE.equals( _clazz );
        case INT:
          return Integer.TYPE.equals( _clazz );
        case LONG:
          return Long.TYPE.equals( _clazz );
        default:
          assert TypeKind.SHORT == kind;
          return Short.TYPE.equals( _clazz );
      }
    }

    @Override
    public Boolean visitArray( final ArrayType array, final Void p )
    {
      return _clazz.isArray() && isTypeOf( _clazz.getComponentType(), array.getComponentType() );
    }

    @Override
    public Boolean visitDeclared( final DeclaredType type, final Void ignored )
    {
      return ( (TypeElement) type.asElement() ).getQualifiedName().contentEquals( _clazz.getCanonicalName() );
    }
  }

  private static class ValueValidatingVisitor
    extends SimpleAnnotationValueVisitor14<Boolean, TypeMirror>
  {
    @Nonnull
    private final ValidatorVisitors _visitors;
    @Nonnull
    private final ProcessingEnvironment processingEnv;

    ValueValidatingVisitor( @Nonnull final ValidatorVisitors visitors,
                            @Nonnull final ProcessingEnvironment processingEnv )
    {
      _visitors = visitors;
      this.processingEnv = processingEnv;
    }

    @Override
    protected Boolean defaultAction( final Object o, final TypeMirror expectedType )
    {
      return isTypeOf( o.getClass(), expectedType );
    }

    @Override
    public Boolean visitUnknown( final AnnotationValue av, final TypeMirror expectedType )
    {
      // just take the default action for the unknown
      return defaultAction( av, expectedType );
    }

    @Override
    public Boolean visitAnnotation( final AnnotationMirror a, final TypeMirror expectedType )
    {
      return processingEnv.getTypeUtils().isSameType( a.getAnnotationType(), expectedType ) &&
             validateAnnotation( a );
    }

    private boolean validateAnnotations( @Nonnull final Iterable<? extends AnnotationMirror> annotationMirrors )
    {
      for ( AnnotationMirror annotationMirror : annotationMirrors )
      {
        if ( !validateAnnotation( annotationMirror ) )
        {
          return false;
        }
      }
      return true;
    }

    private boolean validateAnnotation( @Nonnull final AnnotationMirror annotationMirror )
    {
      return _visitors.validateType( annotationMirror.getAnnotationType() ) &&
             validateAnnotationValues( annotationMirror.getElementValues() );
    }

    private boolean validateAnnotationValues( @Nonnull final Map<? extends ExecutableElement, ? extends AnnotationValue> valueMap )
    {
      for ( final Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> valueEntry : valueMap.entrySet() )
      {
        final TypeMirror expectedType = valueEntry.getKey().getReturnType();
        if ( !validateAnnotationValue( valueEntry.getValue(), expectedType ) )
        {
          return false;
        }
      }
      return true;
    }

    private boolean validateAnnotationValue( @Nonnull final AnnotationValue annotationValue,
                                             @Nonnull final TypeMirror expectedType )
    {
      return annotationValue.accept( _visitors._valueValidatingVisitor, expectedType );
    }

    @Override
    public Boolean visitArray( final List<? extends AnnotationValue> values, final TypeMirror expectedType )
    {
      if ( TypeKind.ARRAY != expectedType.getKind() )
      {
        return false;
      }
      else
      {
        for ( final AnnotationValue value : values )
        {
          if ( !value.accept( this, ( (ArrayType) expectedType ).getComponentType() ) )
          {
            return false;
          }
        }
        return true;
      }
    }

    @Override
    public Boolean visitEnumConstant( final VariableElement enumConstant, final TypeMirror expectedType )
    {
      return processingEnv.getTypeUtils().isSameType( enumConstant.asType(), expectedType ) &&
             _visitors.validateElement( enumConstant );
    }

    @Override
    public Boolean visitType( final TypeMirror type, final TypeMirror ignored )
    {
      // We could check assignability here, but would require a Types instance. Since this
      // isn't really the sort of thing that shows up in a bad AST from upstream compilation
      // we ignore the expected type and just validate the type.  It might be wrong, but
      // it's valid.
      return _visitors.validateType( type );
    }

    @Override
    public Boolean visitBoolean( final boolean b, final TypeMirror expectedType )
    {
      return isTypeOf( Boolean.TYPE, expectedType );
    }

    @Override
    public Boolean visitByte( final byte b, final TypeMirror expectedType )
    {
      return isTypeOf( Byte.TYPE, expectedType );
    }

    @Override
    public Boolean visitChar( final char c, final TypeMirror expectedType )
    {
      return isTypeOf( Character.TYPE, expectedType );
    }

    @Override
    public Boolean visitDouble( final double d, final TypeMirror expectedType )
    {
      return isTypeOf( Double.TYPE, expectedType );
    }

    @Override
    public Boolean visitFloat( final float f, final TypeMirror expectedType )
    {
      return isTypeOf( Float.TYPE, expectedType );
    }

    @Override
    public Boolean visitInt( final int i, final TypeMirror expectedType )
    {
      return isTypeOf( Integer.TYPE, expectedType );
    }

    @Override
    public Boolean visitLong( final long l, final TypeMirror expectedType )
    {
      return isTypeOf( Long.TYPE, expectedType );
    }

    @Override
    public Boolean visitShort( final short s, final TypeMirror expectedType )
    {
      return isTypeOf( Short.TYPE, expectedType );
    }

  }

  private static class TypeValidatingVisitor
    extends SimpleTypeVisitor14<Boolean, Void>
  {
    @Nonnull
    private final ValidatorVisitors _visitors;

    TypeValidatingVisitor( @Nonnull final ValidatorVisitors visitors )
    {
      _visitors = visitors;
    }

    @Override
    protected Boolean defaultAction( final TypeMirror t, final Void p )
    {
      return true;
    }

    @Override
    public Boolean visitArray( final ArrayType t, final Void p )
    {
      return _visitors.validateType( t.getComponentType() );
    }

    @Override
    public Boolean visitDeclared( final DeclaredType t, final Void p )
    {
      return _visitors.validateTypes( t.getTypeArguments() );
    }

    @Override
    public Boolean visitError( final ErrorType t, final Void p )
    {
      return false;
    }

    @Override
    public Boolean visitUnknown( final TypeMirror t, final Void p )
    {
      // just make the default choice for unknown types
      return defaultAction( t, p );
    }

    @Override
    public Boolean visitWildcard( final WildcardType t, final Void p )
    {
      final TypeMirror extendsBound = t.getExtendsBound();
      final TypeMirror superBound = t.getSuperBound();
      return ( null == extendsBound || _visitors.validateType( extendsBound ) ) &&
             ( null == superBound || _visitors.validateType( superBound ) );
    }

    @Override
    public Boolean visitExecutable( final ExecutableType t, final Void p )
    {
      return _visitors.validateTypes( t.getParameterTypes() ) &&
             _visitors.validateType( t.getReturnType() ) &&
             _visitors.validateTypes( t.getThrownTypes() ) &&
             _visitors.validateTypes( t.getTypeVariables() );
    }
  }

  private static class ElementValidatingVisitor
    extends AbstractElementVisitor14<Boolean, Void>
  {
    @Nonnull
    private final ValidatorVisitors _visitors;

    ElementValidatingVisitor( @Nonnull final ValidatorVisitors visitors )
    {
      _visitors = visitors;
    }

    @Override
    public Boolean visitRecordComponent( final RecordComponentElement t, final Void unused )
    {
      // just assume that record components are OK
      return null;
    }

    @Override
    public Boolean visitModule( final ModuleElement t, final Void unused )
    {
      // just assume that modules are OK
      return true;
    }

    @Override
    public Boolean visitPackage( final PackageElement e, final Void p )
    {
      // don't validate enclosed elements because it will return types in the package
      return _visitors._valueValidatingVisitor.validateAnnotations( e.getAnnotationMirrors() );
    }

    @Override
    public Boolean visitType( final TypeElement e, final Void p )
    {
      return isValidBaseElement( e ) &&
             _visitors.validateElements( e.getTypeParameters() ) &&
             _visitors.validateTypes( e.getInterfaces() ) &&
             _visitors.validateType( e.getSuperclass() );
    }

    @Override
    public Boolean visitVariable( final VariableElement e, final Void p )
    {
      return isValidBaseElement( e );
    }

    @Override
    public Boolean visitExecutable( final ExecutableElement e, final Void p )
    {
      final AnnotationValue defaultValue = e.getDefaultValue();
      return isValidBaseElement( e ) &&
             ( null == defaultValue ||
               defaultValue.accept( _visitors._valueValidatingVisitor, e.getReturnType() ) ) &&
             _visitors.validateType( e.getReturnType() ) &&
             _visitors.validateTypes( e.getThrownTypes() ) &&
             _visitors.validateElements( e.getTypeParameters() ) &&
             _visitors.validateElements( e.getParameters() );
    }

    @Override
    public Boolean visitTypeParameter( final TypeParameterElement e, final Void p )
    {
      return isValidBaseElement( e ) && _visitors.validateTypes( e.getBounds() );
    }

    @Override
    public Boolean visitUnknown( final Element e, final Void p )
    {
      // just assume that unknown elements are OK
      return true;
    }

    private boolean isValidBaseElement( @Nonnull final Element e )
    {
      return _visitors.validateType( e.asType() ) &&
             _visitors._valueValidatingVisitor.validateAnnotations( e.getAnnotationMirrors() ) &&
             _visitors.validateElements( e.getEnclosedElements() );
    }
  }
}
