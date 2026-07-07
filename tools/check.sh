#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "${ROOT}"
COVERAGE_FILTER="^//(core|qa)/src/main/java/org/realityforge/proton[/:]"
BAZEL_INFO="$(bazel info execution_root output_base)"
EXECUTION_ROOT=""
OUTPUT_BASE=""
while IFS= read -r line; do
  case "${line}" in
    "execution_root: "*)
      EXECUTION_ROOT="${line#execution_root: }"
      ;;
    "output_base: "*)
      OUTPUT_BASE="${line#output_base: }"
      ;;
  esac
done <<< "${BAZEL_INFO}"
if [[ -z "${EXECUTION_ROOT}" || -z "${OUTPUT_BASE}" ]]; then
  echo "Unable to determine Bazel execution_root and output_base" >&2
  exit 1
fi
COVERAGE_REPORT="${EXECUTION_ROOT}/bazel-out/_coverage/_coverage_report.dat"

BAZEL_OUTPUT_BASE="${OUTPUT_BASE}" tools/update_java_deps.sh
bazel run //:buildifier_check
tools/java_format.sh check
bazel build //...

# Run the packaging-oriented QA tests outside coverage because they execute the
# built jar with an external javac process. The coverage run uses a QA target that
# excludes only those methods, avoiding JaCoCo offline instrumentation leakage
# into that external compiler process.
bazel test \
  //third_party/java:verify_config_sha256 \
  //qa/src/test/java/org/realityforge/proton/qa:qa_tests \
  //tools/release:all_tests
bazel coverage \
  //core/src/test/java/org/realityforge/proton:core_tests \
  //qa/src/test/java/org/realityforge/proton/qa:qa_coverage_tests \
  --combined_report=lcov \
  --instrumentation_filter="${COVERAGE_FILTER}"
tools/check_coverage.py "${COVERAGE_REPORT}" 0.72 0.62
