#!/bin/sh
# SPDX-License-Identifier: CC0-1.0
# A draft of the install step to propose at the next AtCoder language update.
# It fetches the release jar, verifies it, and leaves it where the compile and run commands' -cp
# can name it. See docs/PLAN.md, section 1.4 and Phase 7.
# Usage: install.sh JAR_URL SHA256 DESTINATION
set -eu

url=$1
expected=$2
destination=$3

curl -fsSL -o "$destination" "$url"
echo "$expected  $destination" | sha256sum -c -
