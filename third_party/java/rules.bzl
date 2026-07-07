load("@rules_java//java:defs.bzl", _java_binary = "java_binary", _java_library = "java_library", _java_test = "java_test")

_JAVA_JAVACOPTS = [
    "--release",
    "17",
    "-Werror",
    "-Xlint:all,-processing,-serial,-path,-options,-classfile",
]

_JAVA_TEST_JVM_FLAGS = [
    "-ea",
]

def java_library(name, srcs = [], javacopts = [], deps = [], **kwargs):
    _java_library(
        name = name,
        srcs = srcs,
        deps = deps,
        javacopts = _JAVA_JAVACOPTS + javacopts,
        **kwargs
    )

def java_binary(name, srcs = [], javacopts = [], deps = [], **kwargs):
    _java_binary(
        name = name,
        srcs = srcs,
        deps = deps,
        javacopts = _JAVA_JAVACOPTS + javacopts,
        **kwargs
    )

def java_test(name, srcs = [], javacopts = [], deps = [], jvm_flags = [], **kwargs):
    _java_test(
        name = name,
        srcs = srcs,
        deps = deps,
        javacopts = _JAVA_JAVACOPTS + javacopts,
        jvm_flags = _JAVA_TEST_JVM_FLAGS + jvm_flags,
        **kwargs
    )

def java_testng_test(name, test_classes, testng_args = [], **kwargs):
    java_test(
        name = name,
        args = [
            "-testclass",
            ",".join(test_classes),
        ] + testng_args,
        env_inherit = ["PATH"],
        main_class = "org.testng.TestNG",
        use_testrunner = False,
        **kwargs
    )
