package org.realityforge.proton.qa;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.processing.Processor;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public final class ProcessorTestHarnessTest
  extends AbstractProcessorTest
{
  private record JavacResult( int exitCode,
                              @Nonnull String output,
                              @Nonnull String generatedSource,
                              @Nonnull String processorPath )
  {
  }

  @Override
  @Nonnull
  protected String getOptionPrefix()
  {
    return "proton_test";
  }

  @Override
  @Nonnull
  protected Processor processor()
  {
    return new TestProcessor();
  }

  @Test
  public void assertSuccessfulCompileChecksExpectedAndExpectedFormatted()
    throws Exception
  {
    assertSuccessfulCompile( "com.example.Model", "com/example/generated/GeneratedModel.java" );
  }

  @Test
  public void processorAdvertisesCommonSupportedOptions()
  {
    final Set<String> options = processor().getSupportedOptions();
    assertTrue( options.contains( "proton_test.custom_option" ) );
    for ( final String option :
      Arrays.asList( "verbose_out_of_round.errors",
                     "defer.errors",
                     "defer.unresolved",
                     "debug",
                     "profile",
                     "warnings_as_errors",
                     "format_generated_source" ) )
    {
      assertTrue( options.contains( "proton_test." + option ), "Missing option " + option );
    }
  }

  @Test
  public void formatterJdkExportsExposesRequiredExports()
  {
    assertEquals( formatterJdkExports().size(), 6 );
    assertTrue( formatterJdkExports().contains( "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED" ) );
  }

  @Test
  public void outputFixtureDataWritesExpectedAndExpectedFormatted()
    throws Exception
  {
    final Path dir = Files.createTempDirectory( "proton-fixtures" );
    final String fixtureDir = System.getProperty( "proton_test.fixture_dir" );
    final String outputFixtureData = System.getProperty( "proton_test.output_fixture_data" );
    try
    {
      final Path input = dir.resolve( "input/com/example/Model.java" );
      Files.createDirectories( input.getParent() );
      Files.writeString( input,
                         "package com.example;\n" +
                         "import org.realityforge.proton.qa.GenerateType;\n" +
                         "@GenerateType\n" +
                         "public class Model {}\n" );
      System.setProperty( "proton_test.fixture_dir", dir.toString() );
      System.setProperty( "proton_test.output_fixture_data", "true" );
      assertSuccessfulCompile( "com.example.Model", "com/example/generated/GeneratedModel.java" );
      assertTrue( Files.exists( dir.resolve( "expected/com/example/generated/GeneratedModel.java" ) ) );
      assertTrue( Files.exists( dir.resolve( "expectedFormatted/com/example/generated/GeneratedModel.java" ) ) );
    }
    finally
    {
      restoreProperty( "proton_test.fixture_dir", fixtureDir );
      restoreProperty( "proton_test.output_fixture_data", outputFixtureData );
      deleteDir( dir );
    }
  }

  @Test
  public void coreJarContainsRelocatedFormatterDependencies()
    throws Exception
  {
    final List<String> entries = jarEntries( coreJar() );
    for ( final String prefix :
      Arrays.asList( "org/realityforge/proton/vendor/javaformat/",
                     "org/realityforge/proton/vendor/google/common/",
                     "org/realityforge/proton/vendor/jackson/",
                     "org/realityforge/proton/vendor/fj/",
                     "org/realityforge/proton/vendor/jspecify/",
                     "org/realityforge/proton/vendor/google/errorprone/",
                     "org/realityforge/proton/vendor/google/j2objc/" ) )
    {
      assertTrue( entries.stream().anyMatch( e -> e.startsWith( prefix ) ), "Missing relocated package " + prefix );
    }
    for ( final String prefix :
      Arrays.asList( "com/palantir/javaformat/",
                     "com/google/common/",
                     "com/google/thirdparty/",
                     "com/google/errorprone/",
                     "com/google/j2objc/",
                     "org/jspecify/",
                     "fj/",
                     "com/fasterxml/jackson/",
                     "META-INF/versions/21/" ) )
    {
      assertFalse( entries.stream().anyMatch( e -> e.startsWith( prefix ) ), "Unrelocated package " + prefix );
    }
  }

  @Test
  public void corePomExcludesFormatterDependenciesAndPreservesExistingDependencies()
    throws Exception
  {
    final Document pom = loadPom();
    assertTrue( dependencyVersions( pom, "com.palantir.javaformat", "palantir-java-format" ).isEmpty() );
    assertTrue( dependencyVersions( pom, "com.palantir.javaformat", "palantir-java-format-spi" ).isEmpty() );
    assertTrue( dependencyVersions( pom, "com.google.guava", "failureaccess" ).isEmpty() );
    assertTrue( dependencyVersions( pom, "com.google.guava", "listenablefuture" ).isEmpty() );
    assertTrue( dependencyVersions( pom, "org.jspecify", "jspecify" ).isEmpty() );
    assertTrue( dependencyVersions( pom, "com.google.errorprone", "error_prone_annotations" ).isEmpty() );
    assertTrue( dependencyVersions( pom, "com.google.j2objc", "j2objc-annotations" ).isEmpty() );
    assertTrue( dependencyVersions( pom, "org.functionaljava", "functionaljava" ).isEmpty() );
    assertTrue( dependencyVersions( pom, "com.fasterxml.jackson.core", "jackson-core" ).isEmpty() );
    assertTrue( dependencyVersions( pom, "com.fasterxml.jackson.core", "jackson-databind" ).isEmpty() );
    assertTrue( dependencyVersions( pom, "com.fasterxml.jackson.core", "jackson-annotations" ).isEmpty() );
    assertTrue( dependencyVersions( pom, "com.fasterxml.jackson.datatype", "jackson-datatype-jdk8" ).isEmpty() );
    assertTrue( dependencyVersions( pom, "com.fasterxml.jackson.datatype", "jackson-datatype-guava" ).isEmpty() );
    assertTrue( dependencyVersions( pom, "com.fasterxml.jackson.module", "jackson-module-parameter-names" ).isEmpty() );

    assertEquals( dependencyVersions( pom, "org.realityforge.javax.annotation", "javax.annotation" ),
                  Arrays.asList( "1.1.1" ) );
    assertEquals( dependencyVersions( pom, "org.glassfish", "javax.json" ), Arrays.asList( "1.1" ) );
    assertEquals( dependencyVersions( pom, "com.palantir.javapoet", "javapoet" ), Arrays.asList( "0.14.0" ) );
    assertEquals( dependencyVersions( pom, "com.google.guava", "guava" ), Arrays.asList( "27.1-jre" ) );
  }

  @Test
  public void externalJavacUsesPackagedVendorFormatter()
    throws Exception
  {
    final JavacResult result = runExternalJavac( coreJar() );
    assertEquals( result.exitCode(), 0, result.output() );
    assertFalse( result.processorPath().contains( "palantir-java-format" ) );
    assertTrue( result.generatedSource().contains( "public final class GeneratedModel" ) );
  }

  @Test
  public void externalJavacReportsMissingFormatter()
    throws Exception
  {
    final JavacResult result = runExternalJavac( Path.of( "target/proton_core/classes" ).toAbsolutePath() );
    assertTrue( 0 != result.exitCode(), "Expected javac failure but compilation succeeded" );
    assertTrue( result.output().contains( "proton_test.format_generated_source=true" ), result.output() );
    assertTrue( result.output().contains( "com.palantir.javaformat.java.Formatter" ), result.output() );
    assertTrue( result.output().contains( "org.realityforge.proton.vendor.javaformat.java.Formatter" ), result.output() );
    assertTrue( result.output().contains( "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED" ),
                result.output() );
  }

  private void restoreProperty( @Nonnull final String key, final String value )
  {
    if ( null == value )
    {
      System.clearProperty( key );
    }
    else
    {
      System.setProperty( key, value );
    }
  }

  @Nonnull
  private Path coreJar()
  {
    return Path.of( System.getProperty( "proton.core.jar" ) );
  }

  @Nonnull
  private Path corePom()
  {
    return Path.of( System.getProperty( "proton.core.pom" ) );
  }

  @Nonnull
  private List<String> jarEntries( @Nonnull final Path jar )
    throws IOException
  {
    try ( final JarFile jarFile = new JarFile( jar.toFile() ) )
    {
      return jarFile.stream().map( ZipEntry::getName ).toList();
    }
  }

  @Nonnull
  private Document loadPom()
    throws Exception
  {
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature( "http://apache.org/xml/features/disallow-doctype-decl", true );
    return factory.newDocumentBuilder().parse( corePom().toFile() );
  }

  @Nonnull
  private List<String> dependencyVersions( @Nonnull final Document pom,
                                           @Nonnull final String groupId,
                                           @Nonnull final String artifactId )
  {
    final List<String> versions = new ArrayList<>();
    final NodeList dependencies = pom.getElementsByTagName( "dependency" );
    for ( int i = 0; i < dependencies.getLength(); i++ )
    {
      final Element dependency = (Element) dependencies.item( i );
      if ( groupId.equals( childText( dependency, "groupId" ) ) &&
           artifactId.equals( childText( dependency, "artifactId" ) ) )
      {
        versions.add( childText( dependency, "version" ) );
      }
    }
    return versions;
  }

  @Nonnull
  private String childText( @Nonnull final Element element, @Nonnull final String tagName )
  {
    final NodeList nodes = element.getElementsByTagName( tagName );
    return 0 == nodes.getLength() ? "" : nodes.item( 0 ).getTextContent();
  }

  @Nonnull
  private JavacResult runExternalJavac( @Nonnull final Path corePath )
    throws Exception
  {
    final Path dir = Files.createTempDirectory( "proton-javac" );
    try
    {
      final Path source = dir.resolve( "src/com/example/Model.java" );
      final Path sourceOutput = dir.resolve( "generated" );
      final Path classOutput = dir.resolve( "classes" );
      Files.createDirectories( source.getParent() );
      Files.createDirectories( sourceOutput );
      Files.createDirectories( classOutput );
      Files.writeString( source,
                         "package com.example;\n" +
                         "import org.realityforge.proton.qa.GenerateType;\n" +
                         "@GenerateType\n" +
                         "public class Model {}\n" );

      final Path testClasses = Path.of( System.getProperty( "baseDir" ) );
      final String classPath = testClasses.toString();
      final String processorPath =
        joinPaths( testClasses,
                   corePath,
                   Path.of( System.getProperty( "proton.javax_annotation.jar" ) ),
                   Path.of( System.getProperty( "proton.javapoet.jar" ) ),
                   Path.of( System.getProperty( "proton.guava.jar" ) ) );
      assertFalse( processorPath.contains( "palantir-java-format" ) );

      final List<String> command = new ArrayList<>();
      command.add( Path.of( System.getProperty( "java.home" ) ).resolve( "bin/javac" ).toString() );
      command.addAll( formatterJdkExports().stream().map( e -> "-J" + e ).toList() );
      command.add( "-classpath" );
      command.add( classPath );
      command.add( "-processorpath" );
      command.add( processorPath );
      command.add( "-processor" );
      command.add( TestProcessor.class.getName() );
      command.add( "-Xlint:all,-processing" );
      command.add( "-implicit:none" );
      command.add( "-Aproton_test.defer.errors=false" );
      command.add( "-s" );
      command.add( sourceOutput.toString() );
      command.add( "-d" );
      command.add( classOutput.toString() );
      command.add( source.toString() );

      final Process process = new ProcessBuilder( command ).redirectErrorStream( true ).start();
      final String output = new String( process.getInputStream().readAllBytes(), StandardCharsets.UTF_8 );
      final int exitCode = process.waitFor();
      final Path generatedSource = sourceOutput.resolve( "com/example/generated/GeneratedModel.java" );
      return new JavacResult( exitCode,
                              output,
                              Files.exists( generatedSource ) ? Files.readString( generatedSource ) : "",
                              processorPath );
    }
    finally
    {
      deleteDir( dir );
    }
  }

  @Nonnull
  private String joinPaths( @Nonnull final Path... paths )
  {
    return Arrays.stream( paths ).map( Path::toString ).reduce( ( a, b ) -> a + File.pathSeparator + b ).orElse( "" );
  }

  @SuppressWarnings( { "resource", "ResultOfMethodCallIgnored" } )
  private void deleteDir( @Nonnull final Path directory )
    throws IOException
  {
    try ( final Stream<Path> stream = Files.walk( directory ) )
    {
      stream.sorted( ( a, b ) -> b.compareTo( a ) ).map( Path::toFile ).forEach( file -> assertTrue( file.delete() ) );
    }
  }
}
