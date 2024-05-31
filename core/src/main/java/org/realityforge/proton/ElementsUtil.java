package org.realityforge.proton;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public final class ElementsUtil
{
  private ElementsUtil()
  {
  }

  public static boolean isWarningNotSuppressed( @Nonnull final Element element, @Nonnull final String warning )
  {
    return !isWarningSuppressed( element, warning );
  }

  public static boolean isWarningNotSuppressed( @Nonnull final Element element,
                                                @Nonnull final String warning,
                                                @Nullable final String alternativeSuppressWarnings )
  {
    return !isWarningSuppressed( element, warning, alternativeSuppressWarnings );
  }

  public static boolean isWarningSuppressed( @Nonnull final Element element, @Nonnull final String warning )
  {
    return isWarningSuppressed( element, warning, null );
  }

  @SuppressWarnings( "unchecked" )
  public static boolean isWarningSuppressed( @Nonnull final Element element,
                                             @Nonnull final String warning,
                                             @Nullable final String alternativeSuppressWarnings )
  {
    if ( null != alternativeSuppressWarnings )
    {
      final AnnotationMirror suppress = AnnotationsUtil.findAnnotationByType( element, alternativeSuppressWarnings );
      if ( null != suppress )
      {
        final AnnotationValue value = AnnotationsUtil.findAnnotationValueNoDefaults( suppress, "value" );
        if ( null != value )
        {
          final List<AnnotationValue> warnings = (List<AnnotationValue>) value.getValue();
          for ( final AnnotationValue suppression : warnings )
          {
            if ( warning.equals( suppression.getValue() ) )
            {
              return true;
            }
          }
        }
      }
    }

    final SuppressWarnings annotation = element.getAnnotation( SuppressWarnings.class );
    if ( null != annotation )
    {
      for ( final String suppression : annotation.value() )
      {
        if ( warning.equals( suppression ) )
        {
          return true;
        }
      }
    }
    final Element enclosingElement = element.getEnclosingElement();
    return null != enclosingElement && isWarningSuppressed( enclosingElement, warning, alternativeSuppressWarnings );
  }

  @Nonnull
  public static List<TypeElement> getSuperTypes( @Nonnull final TypeElement element )
  {
    final List<TypeElement> superTypes = new ArrayList<>();
    enumerateSuperTypes( element, superTypes );
    return superTypes;
  }

  private static void enumerateSuperTypes( @Nonnull final TypeElement element,
                                           @Nonnull final List<TypeElement> superTypes )
  {
    final TypeMirror superclass = element.getSuperclass();
    if ( TypeKind.NONE != superclass.getKind() )
    {
      final TypeElement superclassElement = (TypeElement) ( (DeclaredType) superclass ).asElement();
      superTypes.add( superclassElement );
      enumerateSuperTypes( superclassElement, superTypes );
    }
    for ( final TypeMirror interfaceType : element.getInterfaces() )
    {
      final TypeElement interfaceElement = (TypeElement) ( (DeclaredType) interfaceType ).asElement();
      enumerateSuperTypes( interfaceElement, superTypes );
    }
  }

  @Nonnull
  public static List<TypeElement> getInterfaces( @Nonnull final TypeElement element )
  {
    final List<TypeElement> superTypes = new ArrayList<>();
    enumerateInterfaces( element, superTypes );
    return superTypes;
  }

  private static void enumerateInterfaces( @Nonnull final TypeElement element,
                                           @Nonnull final List<TypeElement> superTypes )
  {
    final TypeMirror superclass = element.getSuperclass();
    if ( TypeKind.NONE != superclass.getKind() )
    {
      final TypeElement superclassElement = (TypeElement) ( (DeclaredType) superclass ).asElement();
      enumerateInterfaces( superclassElement, superTypes );
    }
    for ( final TypeMirror interfaceType : element.getInterfaces() )
    {
      final TypeElement interfaceElement = (TypeElement) ( (DeclaredType) interfaceType ).asElement();
      superTypes.add( interfaceElement );
      enumerateInterfaces( interfaceElement, superTypes );
    }
  }

  @Nonnull
  public static List<VariableElement> getFields( @Nonnull final TypeElement element )
  {
    final Map<String, VariableElement> methodMap = new LinkedHashMap<>();
    enumerateFields( element, methodMap );
    return new ArrayList<>( methodMap.values() );
  }

  private static void enumerateFields( @Nonnull final TypeElement element,
                                       @Nonnull final Map<String, VariableElement> fields )
  {
    final TypeMirror superclass = element.getSuperclass();
    if ( TypeKind.NONE != superclass.getKind() )
    {
      enumerateFields( (TypeElement) ( (DeclaredType) superclass ).asElement(), fields );
    }
    for ( final Element member : element.getEnclosedElements() )
    {
      if ( ElementKind.FIELD == member.getKind() )
      {
        fields.put( member.getSimpleName().toString(), (VariableElement) member );
      }
    }
  }

  @Nonnull
  public static List<ExecutableElement> getMethods( @Nonnull final TypeElement element,
                                                    @Nonnull final Elements elementUtils,
                                                    @Nonnull final Types typeUtils )
  {
    return getMethods( element, elementUtils, typeUtils, false );
  }

  @Nonnull
  public static List<ExecutableElement> getMethods( @Nonnull final TypeElement element,
                                                    @Nonnull final Elements elementUtils,
                                                    @Nonnull final Types typeUtils,
                                                    final boolean collectInterfaceMethodsAtEnd )
  {
    final Map<String, ArrayList<ExecutableElement>> methodMap = new LinkedHashMap<>();
    enumerateMethods( element, elementUtils, typeUtils, element, methodMap, collectInterfaceMethodsAtEnd );
    if ( collectInterfaceMethodsAtEnd )
    {
      // Collect the interfaces at the end. Usually this is done
      enumerateMethodsFromInterfaces( element, elementUtils, typeUtils, element, methodMap );
    }
    return methodMap.values().stream().flatMap( Collection::stream ).toList();
  }

  private static void enumerateMethods( @Nonnull final TypeElement scope,
                                        @Nonnull final Elements elementUtils,
                                        @Nonnull final Types typeUtils,
                                        @Nonnull final TypeElement element,
                                        @Nonnull final Map<String, ArrayList<ExecutableElement>> methods,
                                        final boolean collectInterfaceMethodsAtEnd )
  {
    final TypeMirror superclass = element.getSuperclass();
    if ( TypeKind.NONE != superclass.getKind() )
    {
      final TypeElement superclassElement = (TypeElement) ( (DeclaredType) superclass ).asElement();
      enumerateMethods( scope, elementUtils, typeUtils, superclassElement, methods, collectInterfaceMethodsAtEnd );
    }
    if ( !collectInterfaceMethodsAtEnd )
    {
      for ( final TypeMirror interfaceType : element.getInterfaces() )
      {
        final TypeElement interfaceElement = (TypeElement) ( (DeclaredType) interfaceType ).asElement();
        enumerateMethodsFromInterfaces( scope, elementUtils, typeUtils, interfaceElement, methods );
      }
    }
    for ( final Element member : element.getEnclosedElements() )
    {
      if ( ElementKind.METHOD == member.getKind() )
      {
        final ExecutableElement method = (ExecutableElement) member;
        processMethod( elementUtils, typeUtils, scope, methods, method );
      }
    }
  }

  private static void enumerateMethodsFromInterfaces( @Nonnull final TypeElement scope,
                                                      @Nonnull final Elements elementUtils,
                                                      @Nonnull final Types typeUtils,
                                                      @Nonnull final TypeElement element,
                                                      @Nonnull final Map<String, ArrayList<ExecutableElement>> methods )
  {
    final TypeMirror superclass = element.getSuperclass();
    if ( TypeKind.NONE != superclass.getKind() )
    {
      final TypeElement superclassElement = (TypeElement) ( (DeclaredType) superclass ).asElement();
      enumerateMethodsFromInterfaces( scope, elementUtils, typeUtils, superclassElement, methods );
    }
    for ( final TypeMirror interfaceType : element.getInterfaces() )
    {
      final TypeElement interfaceElement = (TypeElement) ( (DeclaredType) interfaceType ).asElement();
      enumerateMethodsFromInterfaces( scope, elementUtils, typeUtils, interfaceElement, methods );
    }
    // Only collect methods from interfaces
    if ( ElementKind.INTERFACE == element.getKind() )
    {
      for ( final Element member : element.getEnclosedElements() )
      {
        if ( ElementKind.METHOD == member.getKind() )
        {
          final ExecutableElement method = (ExecutableElement) member;
          processMethod( elementUtils, typeUtils, scope, methods, method );
        }
      }
    }
  }

  private static void processMethod( @Nonnull final Elements elementUtils,
                                     @Nonnull final Types typeUtils,
                                     @Nonnull final TypeElement typeElement,
                                     @Nonnull final Map<String, ArrayList<ExecutableElement>> methods,
                                     @Nonnull final ExecutableElement method )
  {
    final ExecutableType methodType =
      (ExecutableType) typeUtils.asMemberOf( (DeclaredType) typeElement.asType(), method );

    final String key = method.getSimpleName().toString();
    final ArrayList<ExecutableElement> elements = methods.computeIfAbsent( key, k -> new ArrayList<>() );
    boolean found = false;
    final int size = elements.size();
    for ( int i = 0; i < size; i++ )
    {
      final ExecutableElement executableElement = elements.get( i );
      if ( method.equals( executableElement ) )
      {
        found = true;
        break;
      }
      else if ( isSubsignature( typeUtils, typeElement, methodType, executableElement ) )
      {
        if ( !isAbstractInterfaceMethod( method ) || isAbstractInterfaceMethod( executableElement ) )
        {
          elements.set( i, method );
        }
        found = true;
        break;
      }
      else if ( elementUtils.overrides( method, executableElement, typeElement ) )
      {
        elements.set( i, method );
        found = true;
        break;
      }
    }
    if ( !found )
    {
      elements.add( method );
    }
  }

  private static boolean isAbstractInterfaceMethod( @Nonnull final ExecutableElement method )
  {
    return method.getModifiers().contains( Modifier.ABSTRACT ) &&
           ElementKind.INTERFACE == method.getEnclosingElement().getKind();
  }

  private static boolean isSubsignature( @Nonnull final Types typeUtils,
                                         @Nonnull final TypeElement typeElement,
                                         @Nonnull final ExecutableType methodType,
                                         @Nonnull final ExecutableElement candidate )
  {
    final ExecutableType candidateType =
      (ExecutableType) typeUtils.asMemberOf( (DeclaredType) typeElement.asType(), candidate );
    final boolean isEqual = methodType.equals( candidateType );
    final boolean isSubsignature = typeUtils.isSubsignature( methodType, candidateType );
    return isSubsignature || isEqual;
  }

  @Nonnull
  public static List<ExecutableElement> getConstructors( @Nonnull final TypeElement element )
  {
    return element.getEnclosedElements().stream().
      filter( m -> ElementKind.CONSTRUCTOR == m.getKind() ).
      map( m -> (ExecutableElement) m ).
      collect( Collectors.toList() );
  }

  public static boolean doesMethodOverrideInterfaceMethod( @Nonnull final Types typeUtils,
                                                           @Nonnull final TypeElement typeElement,
                                                           @Nonnull final ExecutableElement method )
  {
    return getInterfaces( typeElement ).stream()
      .flatMap( i -> i.getEnclosedElements().stream() )
      .filter( e -> ElementKind.METHOD == e.getKind() )
      .map( e -> (ExecutableElement) e )
      .anyMatch( e -> isSubsignature( typeUtils,
                                      typeElement,
                                      (ExecutableType) typeUtils.asMemberOf( (DeclaredType) typeElement.asType(), e ),
                                      method ) );
  }

  @Nonnull
  public static TypeName toRawType( @Nonnull final TypeMirror type )
  {
    final TypeName typeName = TypeName.get( type );
    if ( typeName instanceof ParameterizedTypeName )
    {
      return ( (ParameterizedTypeName) typeName ).rawType;
    }
    else
    {
      return typeName;
    }
  }

  /**
   * Return the outer enclosing element.
   * This is either the top-level class, interface, enum, etc within a package.
   * This helps identify the top level compilation units.
   */
  @Nonnull
  public static Element getTopLevelElement( @Nonnull final Element element )
  {
    Element result = element;
    while ( ElementKind.PACKAGE != result.getEnclosingElement().getKind() )
    {
      result = result.getEnclosingElement();
    }
    return result;
  }

  public static boolean isNonStaticNestedClass( @Nonnull final TypeElement element )
  {
    return NestingKind.TOP_LEVEL != element.getNestingKind() && !element.getModifiers().contains( Modifier.STATIC );
  }

  /**
   * @deprecated Use {@link #hasDeprecatedAnnotation(Element)} instead.
   */
  @Deprecated
  public static boolean isElementDeprecated( @Nonnull final Element element )
  {
    return hasDeprecatedAnnotation( element );
  }

  public static boolean hasDeprecatedAnnotation( @Nonnull final Element element )
  {
    return element
      .getAnnotationMirrors()
      .stream()
      .anyMatch( a -> a.getAnnotationType().toString().equals( Deprecated.class.getName() ) );
  }

  public static boolean isDeprecated( @Nonnull final Element element )
  {
    if ( isElementDeprecated( element ) )
    {
      return true;
    }
    else if ( ( element.getKind().isClass() || element.getKind().isInterface() ) &&
              ElementKind.PACKAGE != element.getEnclosingElement().getKind() )
    {
      return isDeprecated( element.getEnclosingElement() );
    }
    else
    {
      return false;
    }
  }

  public static boolean isEffectivelyPublic( @Nonnull final TypeElement element )
  {
    if ( !element.getModifiers().contains( Modifier.PUBLIC ) )
    {
      return false;
    }
    else
    {
      final Element enclosing = element.getEnclosingElement();
      return ElementKind.PACKAGE == enclosing.getKind() || isEffectivelyPublic( (TypeElement) enclosing );
    }
  }

  @Nonnull
  public static PackageElement getPackageElement( @Nonnull final Element outerElement )
  {
    Element element = outerElement;
    while ( ElementKind.PACKAGE != element.getKind() )
    {
      element = element.getEnclosingElement();
    }
    return (PackageElement) element;
  }

  public static boolean areTypesInDifferentPackage( @Nonnull final TypeElement typeElement1,
                                                    @Nonnull final TypeElement typeElement2 )
  {
    return !areTypesInSamePackage( typeElement1, typeElement2 );
  }

  public static boolean areTypesInSamePackage( @Nonnull final TypeElement typeElement1,
                                               @Nonnull final TypeElement typeElement2 )
  {
    final PackageElement packageElement1 = getPackageElement( typeElement1 );
    final PackageElement packageElement2 = getPackageElement( typeElement2 );
    return Objects.equals( packageElement1.getQualifiedName(), packageElement2.getQualifiedName() );
  }

  /**
   * Return the method that the specified method is overriding if any.
   *
   * @param processingEnv the processing environment.
   * @param typeElement   the enclosing type.
   * @param method        the method.
   * @return the method that the specified method overrides, else null.
   */
  @Nullable
  public static ExecutableElement getOverriddenMethod( @Nonnull final ProcessingEnvironment processingEnv,
                                                       @Nonnull final TypeElement typeElement,
                                                       @Nonnull final ExecutableElement method )
  {
    final TypeMirror superclass = typeElement.getSuperclass();
    if ( TypeKind.NONE == superclass.getKind() )
    {
      return null;
    }
    else
    {
      final TypeElement parent = (TypeElement) processingEnv.getTypeUtils().asElement( superclass );
      final List<? extends Element> enclosedElements = parent.getEnclosedElements();
      for ( final Element enclosedElement : enclosedElements )
      {
        if ( ElementKind.METHOD == enclosedElement.getKind() &&
             processingEnv.getElementUtils().overrides( method, (ExecutableElement) enclosedElement, typeElement ) )
        {
          return (ExecutableElement) enclosedElement;
        }
      }
      return getOverriddenMethod( processingEnv, parent, method );
    }
  }
}
