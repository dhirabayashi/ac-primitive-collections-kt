#!/bin/sh
# SPDX-License-Identifier: CC0-1.0
# Builds the jar at a release tag and publishes it as a GitHub release. CI runs it after the gates pass
# on a tag push. See the release procedure in docs/PLAN.md, Phase 1.
# Usage: release.sh TAG [--dry-run]. The dry run builds and checks, then prints the notes instead of publishing.
set -eu

tag=$1
root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

# Only v and dotted digits are releases, the same rule the bundler reads tags by.
if ! printf '%s\n' "$tag" | grep -Eq '^v[0-9]+(\.[0-9]+)*$'; then
  echo "Not a release tag, so nothing is published: $tag"
  exit 0
fi

"$root/gradlew" -p "$root" -q jar
jar=$root/build/libs/ac-primitive-collections.jar
version=$(unzip -p "$jar" META-INF/MANIFEST.MF | sed -n 's/^Implementation-Version: //p' | tr -d '\r')
if [ "$version" != "${tag#v}" ]; then
  echo "The jar says $version, not ${tag#v}. Build it from a checkout of the tag."
  exit 1
fi

# A pipeline's status is its last command's, so pick the tool first rather than falling back with ||.
if command -v sha256sum >/dev/null; then sha=$(sha256sum "$jar" | cut -d' ' -f1); else sha=$(shasum -a 256 "$jar" | cut -d' ' -f1); fi
size=$(wc -c <"$jar" | tr -d ' ')
url=https://github.com/dhirabayashi/ac-primitive-collections-kt/releases/download/$tag/ac-primitive-collections.jar
notes=$(cat <<NOTES
The library jar for the judge install proposal. Contest submissions use the bundler, which reads the tag.

- Size: $size bytes
- SHA-256: \`$sha\`
- Install rehearsal: \`sh install/install.sh $url $sha ac-primitive-collections.jar\`
NOTES
)

if [ "${2:-}" = --dry-run ]; then
  printf '%s\n' "$notes"
  exit 0
fi
gh release create "$tag" "$jar" --title "$tag" --notes "$notes"
