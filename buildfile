require 'buildr/git_auto_version'
require 'buildr/gpg'
require 'buildr/single_intermediate_layout'
require 'buildr/jacoco'

Buildr::MavenCentral.define_publish_tasks(:profile_name => 'org.realityforge', :username => 'realityforge')

desc 'Proton Annotation Processor Library'
define 'proton' do
  project.group = 'org.realityforge.proton'
  compile.options.source = '1.8'
  compile.options.target = '1.8'
  compile.options.lint = 'all'

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  pom.add_apache_v2_license
  pom.add_github_project('realityforge/proton')
  pom.add_developer('realityforge', 'Peter Donald')
  pom.include_transitive_dependencies << artifact(:javax_annotation)

  desc 'Utilities for building annotation processors'
  define 'core' do
    compile.with :javax_annotation,
                 :javax_json,
                 :javapoet,
                 Buildr::Util.tools_jar,
                 :guava

    package(:jar)
    package(:sources)
    package(:javadoc)
  end

  desc 'Utilities to help testing annotation processors'
  define 'qa' do
    compile.with project('core'),
                 project('core').compile.dependencies,
                 :testng,
                 :compile_testing,
                 :truth,
                 :junit,
                 :hamcrest_core

    package(:jar)
    package(:sources)
    package(:javadoc)
  end

  iml.excluded_directories << project._('tmp')

  ipr.add_component_from_artifact(:idea_codestyle)
end
