require 'buildr/release_tool'

Buildr::ReleaseTool.define_release_task do |t|
  t.extract_version_from_changelog
  t.zapwhite
  t.ensure_git_clean
  t.build
  t.patch_changelog('realityforge/proton')
  t.tag_project
  t.maven_central_publish
  t.patch_changelog_post_release
  t.push_changes
  t.github_release('realityforge/proton')
end
