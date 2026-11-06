# SPDX-License-Identifier: CC0-1.0
# Reproduces the Kotlin toolchain from AtCoder's install script:
# https://img.atcoder.jp/file/language-update/2025-10/062-2-0-21_openjdk-23.toml
# The base image is an assumption; the script does not name the judge's OS.
# Build for linux/amd64. Under emulation it checks compatibility only, so do not use it for timing.
# e2e/run-e2e.sh passes the versions from gradle.properties. A plain build needs the same arguments.
FROM --platform=linux/amd64 ubuntu:24.04

ARG KOTLIN_VERSION
ARG JDK_URL

RUN test -n "$KOTLIN_VERSION" && test -n "$JDK_URL" \
 && apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl time unzip wget \
 && cd /tmp \
 && wget -q "https://github.com/JetBrains/kotlin/releases/download/v$KOTLIN_VERSION/kotlin-compiler-$KOTLIN_VERSION.zip" \
 && mv "kotlin-compiler-$KOTLIN_VERSION.zip" kotlin-compiler.zip \
 && unzip -q kotlin-compiler.zip -d /usr/local \
 && curl -fsSL "$JDK_URL" | tar zx -C /usr/local --strip-component 1 \
 && mkdir -p /judge \
 && wget -q -O /judge/ac_library.jar https://github.com/ocha98/ac-library-java/releases/download/v2.0.0/ac_library23.jar \
 && wget -q -O /judge/kotlinx-collections-immutable-jvm.jar https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-collections-immutable-jvm/0.4.0/kotlinx-collections-immutable-jvm-0.4.0.jar \
 && rm -f /tmp/kotlin-compiler.zip

COPY kotlin.sh /judge/kotlin.sh
COPY compile.sh /judge/compile.sh
WORKDIR /judge
