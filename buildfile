require 'buildr/git_auto_version'
require 'buildr/gpg'
require 'buildr/single_intermediate_layout'
require 'buildr/jacoco'

desc 'Utilities for building annotation processors'
define 'proton-processor-pack' do
  project.group = 'org.realityforge.proton'
  compile.options.source = '1.8'
  compile.options.target = '1.8'
  compile.options.lint = 'all'

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  pom.add_apache_v2_license
  pom.add_github_project('realityforge/proton-processor-pack')
  pom.add_developer('realityforge', 'Peter Donald')
  pom.include_transitive_dependencies << artifact(:javax_annotation)

  compile.with :javax_annotation,
               :jetbrains_annotations,
               :javacsv,
               :testng

  package(:jar)
  package(:sources)
  package(:javadoc)

  iml.excluded_directories << project._('tmp')

  ipr.add_component_from_artifact(:idea_codestyle)
end
