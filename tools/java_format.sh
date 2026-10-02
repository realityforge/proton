#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="${1:-write}"

cd "${ROOT}"

format_roots=(--root=core/src --root=qa/src/main --root=qa/src/test/java --root=tools)

case "${MODE}" in
  write)
    exec bazel run @rules_palantir_java_format//:java_format -- "${format_roots[@]}"
    ;;
  watch)
    exec bazel run @rules_palantir_java_format//:java_format_watch -- "${format_roots[@]}"
    ;;
  check)
    exec bazel build //:java_format_check
    ;;
  *)
    echo "usage: tools/java_format.sh [write|check|watch]" >&2
    exit 2
    ;;
esac
