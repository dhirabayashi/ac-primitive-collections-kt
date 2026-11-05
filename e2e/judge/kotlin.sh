#!/bin/sh
# Copied from AtCoder's install script (see Dockerfile).
if [ "$1" -gt 1024 ]; then
  stack_size=1024
else
  stack_size="$1"
fi

java -Xss"$stack_size"M -DONLINE_JUDGE=true -cp ac_library.jar:kotlinx-collections-immutable-jvm.jar:Main.jar MainKt
