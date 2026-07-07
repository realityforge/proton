#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: tools/package_maven_central.sh <version> [--gpg-executable PATH] [--gpg-key-id KEYID]" >&2
  echo "Defaults: GPG_USER selects the signing key and GPG_PASS supplies an optional passphrase." >&2
}

if [[ $# -lt 1 ]]; then
  usage
  exit 1
fi

VERSION="$1"
shift

if [[ -z "${VERSION}" || "${VERSION}" == *-SNAPSHOT ]]; then
  echo "Release version must be non-empty and must not end with -SNAPSHOT: ${VERSION}" >&2
  exit 1
fi

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "${ROOT}"
bazel build //tools/release:maven_artifacts --release_version="${VERSION}"
bazel test //tools/release:all_tests --release_version="${VERSION}"
bazel run //tools/release:dist --release_version="${VERSION}" -- "$@"
