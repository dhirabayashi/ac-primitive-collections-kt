#!/bin/sh
# The compile command from AtCoder's install script (see Dockerfile).
/usr/local/kotlinc/bin/kotlinc ./Main.kt -cp ac_library.jar:kotlinx-collections-immutable-jvm.jar -include-runtime -jvm-target 23 -d ./Main.jar
