# SPDX-License-Identifier: CC0-1.0
# Reproduces the Kotlin toolchain from AtCoder's install script:
# https://img.atcoder.jp/file/language-update/2025-10/062-2-0-21_openjdk-23.toml
# The base image is an assumption; the script does not name the judge's OS.
# Build for linux/amd64. Under emulation it checks compatibility only, so do not use it for timing.
FROM --platform=linux/amd64 ubuntu:24.04

RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl unzip wget \
 && cd /tmp \
 && wget -q https://github.com/JetBrains/kotlin/releases/download/v2.2.10/kotlin-compiler-2.2.10.zip \
 && mv kotlin-compiler-2.2.10.zip kotlin-compiler.zip \
 && unzip -q kotlin-compiler.zip -d /usr/local \
 && curl -fsSL https://download.java.net/java/GA/jdk23.0.2/6da2a6609d6e406f85c491fcb119101b/7/GPL/openjdk-23.0.2_linux-x64_bin.tar.gz \
    | tar zx -C /usr/local --strip-component 1 \
 && mkdir -p /judge \
 && wget -q -O /judge/ac_library.jar https://github.com/ocha98/ac-library-java/releases/download/v2.0.0/ac_library23.jar \
 && wget -q -O /judge/kotlinx-collections-immutable-jvm.jar https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-collections-immutable-jvm/0.4.0/kotlinx-collections-immutable-jvm-0.4.0.jar \
 && rm -f /tmp/kotlin-compiler.zip

COPY kotlin.sh /judge/kotlin.sh
COPY compile.sh /judge/compile.sh
WORKDIR /judge
