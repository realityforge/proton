package org.realityforge.proton;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.testng.annotations.Test;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.*;

public final class ElementsUtilTest
{
  @Test
  public void accessibilityHelpersInspectRealElements()
    throws Exception
  {
    final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull( compiler );
    final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    final Path classOutput = Files.createTempDirectory( "compile" );
    try ( StandardJavaFileManager fileManager =
            compiler.getStandardFileManager( diagnostics, Locale.getDefault(), UTF_8 ) )
    {
      fileManager.setLocationFromPaths( StandardLocation.CLASS_OUTPUT, Collections.singletonList( classOutput ) );
      final JavaFileObject source =
        new SourceJavaFileObject( "com.example.Scope",
                                  "package com.example;\n" +
                                  "public class Scope {\n" +
                                  "  public Scope() {}\n" +
                                  "  public void publicMethod() {}\n" +
                                  "  void packageMethod() {}\n" +
                                  "  private void privateMethod() {}\n" +
                                  "  public static class PublicStaticNested {}\n" +
                                  "  static class PackageStaticNested {}\n" +
                                  "  public class PublicInner {}\n" +
                                  "  private static class PrivateNested {}\n" +
                                  "}\n" +
                                  "class PackageCtor { PackageCtor() {} }\n" +
                                  "class PrivateCtor { private PrivateCtor() {} }\n" );
      final JavaFileObject publicCtorSource =
        new SourceJavaFileObject( "com.example.PublicCtor",
                                  "package com.example;\n" +
                                  "public class PublicCtor { public PublicCtor() {} }\n" );
      final JavaFileObject defaultCtorSource =
        new SourceJavaFileObject( "com.example.DefaultCtor",
                                  "package com.example;\n" +
                                  "public class DefaultCtor {}\n" );
      final JavaFileObject otherSource =
        new SourceJavaFileObject( "com.other.OtherScope",
                                  "package com.other;\n" +
                                  "public class OtherScope {}\n" );
      final AccessProcessor processor = new AccessProcessor();
      final JavaCompiler.CompilationTask task =
        compiler.getTask( null,
                          fileManager,
                          diagnostics,
                          List.of( "--release", "17" ),
                          null,
                          List.of( source, publicCtorSource, defaultCtorSource, otherSource ) );
      task.setProcessors( Collections.singletonList( processor ) );
      assertTrue( task.call(), asMessage( diagnostics ) );
      assertTrue( processor.wasValidated() );
    }
    finally
    {
      deleteDir( classOutput );
    }
  }

  @Nonnull
  private static String asMessage( @Nonnull final DiagnosticCollector<JavaFileObject> diagnostics )
  {
    return diagnostics.getDiagnostics().stream().map( Object::toString ).collect( Collectors.joining( "\n" ) );
  }

  private static void deleteDir( @Nonnull final Path dir )
    throws IOException
  {
    try ( Stream<Path> paths = Files.walk( dir ) )
    {
      for ( final Path path : paths.sorted( Comparator.reverseOrder() ).toList() )
      {
        Files.deleteIfExists( path );
      }
    }
  }

  @Nonnull
  private static ExecutableElement method( @Nonnull final TypeElement type, @Nonnull final String name )
  {
    return type
      .getEnclosedElements()
      .stream()
      .filter( e -> ElementKind.METHOD == e.getKind() && name.equals( e.getSimpleName().toString() ) )
      .map( e -> (ExecutableElement) e )
      .findFirst()
      .orElseThrow();
  }

  @Nonnull
  private static TypeElement nestedType( @Nonnull final TypeElement type, @Nonnull final String name )
  {
    return type
      .getEnclosedElements()
      .stream()
      .filter( e -> ( e.getKind().isClass() || e.getKind().isInterface() ) &&
                    name.equals( e.getSimpleName().toString() ) )
      .map( e -> (TypeElement) e )
      .findFirst()
      .orElseThrow();
  }

  private static final class SourceJavaFileObject
    extends SimpleJavaFileObject
  {
    @Nonnull
    private final String _source;

    SourceJavaFileObject( @Nonnull final String classname, @Nonnull final String source )
    {
      super( URI.create( "string:///" + classname.replace( '.', '/' ) + Kind.SOURCE.extension ), Kind.SOURCE );
      _source = source;
    }

    @Override
    public CharSequence getCharContent( final boolean ignoreEncodingErrors )
    {
      return _source;
    }
  }

  private static final class AccessProcessor
    extends AbstractProcessor
  {
    private boolean _validated;

    @Nonnull
    @Override
    public Set<String> getSupportedAnnotationTypes()
    {
      return Collections.singleton( "*" );
    }

    @Nonnull
    @Override
    public SourceVersion getSupportedSourceVersion()
    {
      return SourceVersion.latestSupported();
    }

    @Override
    public boolean process( final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv )
    {
      if ( !_validated && !roundEnv.processingOver() )
      {
        final TypeElement scope = getTypeElement( "com.example.Scope" );
        final TypeElement otherScope = getTypeElement( "com.other.OtherScope" );
        final ExecutableElement publicMethod = method( scope, "publicMethod" );
        final ExecutableElement packageMethod = method( scope, "packageMethod" );
        final ExecutableElement privateMethod = method( scope, "privateMethod" );
        final TypeElement publicStaticNested = nestedType( scope, "PublicStaticNested" );
        final TypeElement packageStaticNested = nestedType( scope, "PackageStaticNested" );
        final TypeElement publicInner = nestedType( scope, "PublicInner" );
        final TypeElement privateNested = nestedType( scope, "PrivateNested" );

        assertSame( ElementsUtil.getOwningType( scope ), scope );
        assertSame( ElementsUtil.getOwningType( publicMethod ), scope );
        assertTrue( ElementsUtil.isPackageAccess( packageMethod ) );
        assertFalse( ElementsUtil.isPackageAccess( publicMethod ) );
        assertFalse( ElementsUtil.isPackageAccess( privateMethod ) );

        assertTrue( ElementsUtil.isElementAccessibleFrom( scope, packageMethod ) );
        assertFalse( ElementsUtil.isElementAccessibleFrom( otherScope, packageMethod ) );
        assertTrue( ElementsUtil.isElementAccessibleFrom( otherScope, publicMethod ) );
        assertFalse( ElementsUtil.isElementAccessibleFrom( scope, privateMethod ) );

        assertTrue( ElementsUtil.isTypeAccessibleFrom( otherScope, publicStaticNested ) );
        assertTrue( ElementsUtil.isTypeAccessibleFrom( scope, packageStaticNested ) );
        assertFalse( ElementsUtil.isTypeAccessibleFrom( otherScope, packageStaticNested ) );
        assertFalse( ElementsUtil.isTypeAccessibleFrom( scope, privateNested ) );
        assertTrue( ElementsUtil.isNonStaticNestedType( publicInner ) );
        assertFalse( ElementsUtil.isNonStaticNestedType( publicStaticNested ) );
        assertFalse( ElementsUtil.isNonStaticNestedType( scope ) );

        assertSame( ElementsUtil.asTypeElement( processingEnv, scope.asType() ), scope );
        final Element parameter =
          processingEnv.getElementUtils().getTypeElement( List.class.getName() ).getTypeParameters().get( 0 );
        assertNull( ElementsUtil.asTypeElement( processingEnv, parameter.asType() ) );
        assertTrue( ElementsUtil.isAssignableTo( processingEnv, scope.asType(), Object.class.getName() ) );
        assertFalse( ElementsUtil.isAssignableTo( processingEnv, otherScope.asType(), "com.example.Scope" ) );
        assertFalse( ElementsUtil.isAssignableTo( processingEnv, otherScope.asType(), "com.example.Missing" ) );

        assertTrue( ElementsUtil.hasAccessibleNoArgConstructor( otherScope, getTypeElement( "com.example.PublicCtor" ) ) );
        assertTrue( ElementsUtil.hasAccessibleNoArgConstructor( scope, getTypeElement( "com.example.PackageCtor" ) ) );
        assertFalse( ElementsUtil.hasAccessibleNoArgConstructor( otherScope, getTypeElement( "com.example.PackageCtor" ) ) );
        assertFalse( ElementsUtil.hasAccessibleNoArgConstructor( scope, getTypeElement( "com.example.PrivateCtor" ) ) );
        assertTrue( ElementsUtil.hasAccessibleNoArgConstructor( otherScope, getTypeElement( "com.example.DefaultCtor" ) ) );

        _validated = true;
      }
      return false;
    }

    @Nonnull
    private TypeElement getTypeElement( @Nonnull final String classname )
    {
      final TypeElement typeElement = processingEnv.getElementUtils().getTypeElement( classname );
      assertNotNull( typeElement );
      return typeElement;
    }

    boolean wasValidated()
    {
      return _validated;
    }
  }
}
