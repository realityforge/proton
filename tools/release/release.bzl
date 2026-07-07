load("@rules_java//java/common:java_info.bzl", "JavaInfo")

ReleaseVersionInfo = provider("Release version selected by //tools/release:version.", fields = ["value"])

def _version_impl(ctx):
    return [ReleaseVersionInfo(value = ctx.build_setting_value)]

release_version = rule(
    implementation = _version_impl,
    build_setting = config.string(flag = True),
)

def _direct_java_class_jars(java_targets):
    jars = []
    for target in java_targets:
        jars.extend(target[JavaInfo].runtime_output_jars)
    return jars

def _direct_java_source_jars(java_targets):
    jars = []
    for target in java_targets:
        jars.extend(target[JavaInfo].source_jars)
    return jars

def _transitive_runtime_jars(java_targets):
    jars = []
    for target in java_targets:
        jars.extend(target[JavaInfo].transitive_runtime_jars.to_list())
    return jars

def _release_jar_impl(ctx):
    out = ctx.actions.declare_file(ctx.attr.out)
    input_jars = _direct_java_class_jars(ctx.attr.java_targets) + ctx.files.jars

    args = ctx.actions.args()
    args.add("merge")
    args.add("--output", out)
    args.add_all(input_jars, before_each = "--input")

    ctx.actions.run(
        executable = ctx.executable._jar_builder,
        arguments = [args],
        inputs = input_jars,
        outputs = [out],
        mnemonic = "ReleaseJar",
        progress_message = "Building release jar %{output}",
    )
    return [DefaultInfo(files = depset([out]))]

release_jar = rule(
    implementation = _release_jar_impl,
    attrs = {
        "jars": attr.label_list(allow_files = [".jar"]),
        "java_targets": attr.label_list(providers = [JavaInfo]),
        "out": attr.string(mandatory = True),
        "_jar_builder": attr.label(
            default = Label("//tools/release:jar_builder"),
            executable = True,
            cfg = "exec",
        ),
    },
)

def _release_shaded_jar_impl(ctx):
    out = ctx.actions.declare_file(ctx.attr.out)
    input_jars = _direct_java_class_jars(ctx.attr.java_targets) + ctx.files.jars
    shade_jars = _transitive_runtime_jars(ctx.attr.shade_targets)

    args = ctx.actions.args()
    args.add("merge")
    args.add("--output", out)
    args.add_all(input_jars, before_each = "--input")
    args.add_all(shade_jars, before_each = "--shade-input")
    args.add_all(ctx.attr.relocations, before_each = "--relocate")
    args.add_all(ctx.attr.skip_prefixes, before_each = "--skip-prefix")

    ctx.actions.run(
        executable = ctx.executable._jar_builder,
        arguments = [args],
        inputs = input_jars + shade_jars,
        outputs = [out],
        mnemonic = "ReleaseShadedJar",
        progress_message = "Building shaded release jar %{output}",
    )
    return [DefaultInfo(files = depset([out]))]

release_shaded_jar = rule(
    implementation = _release_shaded_jar_impl,
    attrs = {
        "jars": attr.label_list(allow_files = [".jar"]),
        "java_targets": attr.label_list(providers = [JavaInfo]),
        "out": attr.string(mandatory = True),
        "relocations": attr.string_list(),
        "shade_targets": attr.label_list(providers = [JavaInfo]),
        "skip_prefixes": attr.string_list(),
        "_jar_builder": attr.label(
            default = Label("//tools/release:jar_builder"),
            executable = True,
            cfg = "exec",
        ),
    },
)

def _release_source_jar_impl(ctx):
    out = ctx.actions.declare_file(ctx.attr.out)
    source_jars = _direct_java_source_jars(ctx.attr.java_targets)

    args = ctx.actions.args()
    args.add("merge")
    args.add("--output", out)
    args.add_all(source_jars, before_each = "--input")

    ctx.actions.run(
        executable = ctx.executable._jar_builder,
        arguments = [args],
        inputs = source_jars,
        outputs = [out],
        mnemonic = "ReleaseSourceJar",
        progress_message = "Building release source jar %{output}",
    )
    return [DefaultInfo(files = depset([out]))]

release_source_jar = rule(
    implementation = _release_source_jar_impl,
    attrs = {
        "java_targets": attr.label_list(providers = [JavaInfo]),
        "out": attr.string(mandatory = True),
        "_jar_builder": attr.label(
            default = Label("//tools/release:jar_builder"),
            executable = True,
            cfg = "exec",
        ),
    },
)

def _release_javadoc_jar_impl(ctx):
    out = ctx.actions.declare_file(ctx.attr.out)
    source_jars = _direct_java_source_jars(ctx.attr.java_targets)
    classpath = _direct_java_class_jars(ctx.attr.java_targets) + ctx.files.classpath

    args = ctx.actions.args()
    args.add("--output", out)
    args.add_all(source_jars, before_each = "--source-jar")
    args.add_all(classpath, before_each = "--classpath")

    ctx.actions.run(
        executable = ctx.executable._javadoc_jar_builder,
        arguments = [args],
        inputs = source_jars + classpath,
        outputs = [out],
        mnemonic = "ReleaseJavadocJar",
        progress_message = "Building release Javadocs jar %{output}",
    )
    return [DefaultInfo(files = depset([out]))]

release_javadoc_jar = rule(
    implementation = _release_javadoc_jar_impl,
    attrs = {
        "classpath": attr.label_list(allow_files = [".jar"]),
        "java_targets": attr.label_list(providers = [JavaInfo]),
        "out": attr.string(mandatory = True),
        "_javadoc_jar_builder": attr.label(
            default = Label("//tools/release:javadoc_jar_builder"),
            executable = True,
            cfg = "exec",
        ),
    },
)

def _dependency_xml(dependency, version):
    parts = dependency.split(":")
    if len(parts) != 3 and len(parts) != 4:
        fail("dependency must be group:artifact:version[:scope]: %s" % dependency)
    dep_version = version if parts[2] == "{version}" else parts[2]
    scope = ""
    if len(parts) == 4:
        scope = "\n      <scope>%s</scope>" % parts[3]
    return """    <dependency>
      <groupId>%s</groupId>
      <artifactId>%s</artifactId>
      <version>%s</version>%s
    </dependency>""" % (parts[0], parts[1], dep_version, scope)

def _pom_content(artifact_id, display_name, description, dependencies, version):
    dependency_block = "\n".join([_dependency_xml(dependency, version) for dependency in dependencies])
    return """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://www.w3.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.realityforge.proton</groupId>
  <artifactId>%s</artifactId>
  <version>%s</version>
  <packaging>jar</packaging>
  <name>%s</name>
  <description>%s</description>
  <url>https://github.com/realityforge/proton</url>
  <licenses>
    <license>
      <name>The Apache Software License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <connection>scm:git:git@github.com:realityforge/proton.git</connection>
    <developerConnection>scm:git:git@github.com:realityforge/proton.git</developerConnection>
    <url>git@github.com:realityforge/proton.git</url>
  </scm>
  <issueManagement>
    <url>https://github.com/realityforge/proton/issues</url>
    <system>GitHub Issues</system>
  </issueManagement>
  <developers>
    <developer>
      <id>realityforge</id>
      <name>Peter Donald</name>
    </developer>
  </developers>
  <dependencies>
%s
  </dependencies>
</project>
""" % (artifact_id, version, display_name, description, dependency_block)

def _release_pom_impl(ctx):
    version = ctx.attr.version[ReleaseVersionInfo].value
    out = ctx.actions.declare_file(ctx.attr.out)
    ctx.actions.write(
        output = out,
        content = _pom_content(
            ctx.attr.artifact_id,
            ctx.attr.display_name,
            ctx.attr.description,
            ctx.attr.dependencies,
            version,
        ),
    )
    return [DefaultInfo(files = depset([out]))]

release_pom = rule(
    implementation = _release_pom_impl,
    attrs = {
        "artifact_id": attr.string(mandatory = True),
        "dependencies": attr.string_list(),
        "description": attr.string(mandatory = True),
        "display_name": attr.string(mandatory = True),
        "out": attr.string(mandatory = True),
        "version": attr.label(default = Label("//tools/release:version")),
    },
)

def _release_version_file_impl(ctx):
    version = ctx.attr.version[ReleaseVersionInfo].value
    out = ctx.actions.declare_file(ctx.attr.out)
    ctx.actions.write(output = out, content = version + "\n")
    return [DefaultInfo(files = depset([out]))]

release_version_file = rule(
    implementation = _release_version_file_impl,
    attrs = {
        "out": attr.string(mandatory = True),
        "version": attr.label(default = Label("//tools/release:version")),
    },
)
