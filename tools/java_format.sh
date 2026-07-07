#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="${1:-write}"

case "${MODE}" in
  write | check)
    ;;
  *)
    echo "usage: tools/java_format.sh [write|check]" >&2
    exit 2
    ;;
esac

cd "${ROOT}"

args_file="$(mktemp)"
trap 'rm -f "${args_file}"' EXIT

while IFS= read -r source_file; do
  printf '%s/%s\n' "${ROOT}" "${source_file}" >> "${args_file}"
done < <(rg --files -g '*.java' core/src qa/src/main qa/src/test/java tools | sort)

if [[ ! -s "${args_file}" ]]; then
  exit 0
fi

if [[ "${MODE}" == "check" ]]; then
  bazel run //tools/java-format:palantir_java_format -- \
    --palantir \
    --dry-run \
    --set-exit-if-changed \
    "@${args_file}"
else
  bazel run //tools/java-format:palantir_java_format -- \
    --palantir \
    --replace \
    "@${args_file}"
fi
