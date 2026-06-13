require 'buildr/git_auto_version'
require 'buildr/gpg'
require 'buildr/single_intermediate_layout'
require 'buildr/shade'

Buildr::MavenCentral.define_publish_tasks(:profile_name => 'org.realityforge', :username => 'realityforge')

FORMATTER_DEPS =
  [
    :palantir_java_format,
    :palantir_java_format_spi,
    :palantir_java_format_guava,
    :failureaccess,
    :listenablefuture,
    :jspecify,
    :error_prone_annotations,
    :j2objc_annotations,
    :functionaljava,
    :jackson_core,
    :jackson_databind,
    :jackson_annotations,
    :jackson_datatype_jdk8,
    :jackson_datatype_guava,
    :jackson_module_parameter_names
  ]
FORMATTER_JDK_EXPORTS =
  %w(
    --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
    --add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
    --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
    --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
    --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
    --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
  )

def remove_jar_entries(jar, pattern)
  Zip::File.open(jar.to_s) do |zip|
    zip.entries.select { |entry| entry.name =~ pattern }.each do |entry|
      zip.remove(entry)
    end
  end
end

desc 'Proton Annotation Processor Library'
define 'proton' do
  project.group = 'org.realityforge.proton'
  compile.options.source = '17'
  compile.options.target = '17'
  compile.options.lint = 'all'

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  pom.add_apache_v2_license
  pom.add_github_project('realityforge/proton')
  pom.add_developer('realityforge', 'Peter Donald')
  pom.include_transitive_dependencies << artifact(:javax_annotation)

  desc 'Utilities for building annotation processors'
  define 'core' do
    deps = artifacts(:javax_annotation, :javax_json, :javapoet, :guava)
    formatter_deps = artifacts(*FORMATTER_DEPS)
    pom.include_transitive_dependencies << deps
    pom.dependency_filter = Proc.new { |dep| dep[:scope].to_s != 'test' && deps.include?(dep[:artifact]) }

    compile.with deps,
                 formatter_deps
    test.with :testng
    doc.options.merge!('Xdoclint:all,-missing' => true)

    package(:jar).enhance do |jar|
      formatter_deps.each do |dependency|
        jar.merge(dependency)
      end
      jar.enhance do |f|
        remove_jar_entries(f, %r{\AMETA-INF/versions/21(/|\z)})
        Buildr::Shade.shade(f,
                            f,
                            'com.palantir.javaformat' => 'org.realityforge.proton.vendor.javaformat',
                            'com.google.common' => 'org.realityforge.proton.vendor.google.common',
                            'com.google.thirdparty' => 'org.realityforge.proton.vendor.google.thirdparty',
                            'com.google.errorprone' => 'org.realityforge.proton.vendor.google.errorprone',
                            'com.google.j2objc' => 'org.realityforge.proton.vendor.google.j2objc',
                            'org.jspecify' => 'org.realityforge.proton.vendor.jspecify',
                            'fj' => 'org.realityforge.proton.vendor.fj',
                            'com.fasterxml.jackson' => 'org.realityforge.proton.vendor.jackson')
      end
    end
    package(:sources)
    package(:javadoc)
  end

  desc 'Utilities to help testing annotation processors'
  define 'qa' do
    core_jar = project('core').package(:jar)
    compile.with project('core'),
                 project('core').compile.dependencies,
                 :testng
    test.with :testng,
              core_jar,
              project('core').compile.dependencies
    test.options[:properties] =
      {
        'proton_test.fixture_dir' => _('src/test/fixtures').to_s,
        'proton.core.jar' => core_jar.to_s,
        'proton.core.pom' => core_jar.to_s.sub( /\.jar\z/, '.pom' ),
        'proton.javax_annotation.jar' => artifact(:javax_annotation).to_s,
        'proton.javapoet.jar' => artifact(:javapoet).to_s,
        'proton.guava.jar' => artifact(:guava).to_s
      }
    test.options[:java_args] = ['-ea'] + FORMATTER_JDK_EXPORTS
    test.using :testng
    doc.options.merge!('Xdoclint:all,-missing' => true)

    package(:jar)
    package(:sources)
    package(:javadoc)
  end

  iml.excluded_directories << project._('tmp')

  ipr.add_component_from_artifact(:idea_codestyle)
end
