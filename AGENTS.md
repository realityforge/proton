# Repository Guidelines

This guide helps contributors work effectively on the codebase.

## Bazel Rules

- Non-negotiable: do not use `glob()` in Bazel targets; list source files explicitly.
- Non-negotiable: every source directory owns its own `BUILD.bazel`; Bazel targets must not list source files from child, sibling, or parent directories.
- Non-negotiable: run `tools/check.sh` before claiming implementation work is complete.

## Commit & Pull Request Guidelines

- Keep commits small and focused
- update `CHANGELOG.md` for user-visible changes in the same change, even if the behavior only affects generated code or developer-facing warnings. When updating `CHANGELOG.md`, add the message under the "Unreleased" section. DO NOT add a `Changes in this release:` header as that is added as part of the automation.
