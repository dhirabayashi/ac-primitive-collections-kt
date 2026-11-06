#!/bin/sh
# SPDX-License-Identifier: CC0-1.0
# Compiles and runs every e2e/usage sample in the judge image, in both the paste and jar forms,
# with the judge's commands. Run from anywhere. Needs Docker. See docs/PLAN.md, section 7.5.
set -eu

root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
image=ac-judge-kotlin:2.2.10
work=$root/build/e2e
property() { sed -n "s/^$1=//p" "$root/gradle.properties"; }

"$root/gradlew" -p "$root" -q jar :bundler:installDist :bytecode-check:installDist
docker build -q -t "$image" \
  --build-arg "KOTLIN_VERSION=$(property judgeKotlinVersion)" \
  --build-arg "JDK_URL=$(property judgeJdkUrl)" \
  "$root/e2e/judge" >/dev/null

bundler=$root/bundler/build/install/bundler/bin/bundler
checker=$root/bytecode-check/build/install/bytecode-check/bin/bytecode-check
library_jar=$root/build/libs/ac-primitive-collections-kt-0.1.0-SNAPSHOT.jar

# Limit the container to one CPU so that the JVM picks Serial GC, as on the judge.
in_judge() {
  docker run --rm --platform linux/amd64 --cpus=1 --memory=3900m -v "$1:/work" -w /work "$image" sh -c "$2"
}

failures=0
for sample in "$root"/e2e/usage/*/; do
  name=$(basename "$sample")
  for form in paste jar; do
    dir=$work/$name/$form
    rm -rf "$dir"
    mkdir -p "$dir"
    if [ "$form" = paste ]; then
      "$bundler" --root "$root" --allow-unpublished -o "$dir/Main.kt" "$sample/Main.kt" 2>"$dir/bundle.log"
      extra=
    else
      cp "$sample/Main.kt" "$dir/Main.kt"
      # A rehearsal of the install proposal: the same script, with a file URL in place of the release.
      sha=$(shasum -a 256 "$library_jar" 2>/dev/null | cut -d' ' -f1 || sha256sum "$library_jar" | cut -d' ' -f1)
      cp "$library_jar" "$dir/release.jar"
      cp "$root/install/install.sh" "$dir/install.sh"
      in_judge "$dir" "sh ./install.sh file:///work/release.jar $sha ac-primitive-collections.jar" >/dev/null
      extra=ac-primitive-collections.jar:
    fi
    if [ -f "$sample/input.txt" ]; then input=$sample/input.txt; else input=/dev/null; fi
    cp "$input" "$dir/input.txt"
    # The jar form appends the library to -cp, which is what the install proposal asks for.
    in_judge "$dir" "cp /judge/*.jar . && 
      if [ -n '$extra' ]; then
        /usr/bin/time -f '%e' -o compile-time.txt /usr/local/kotlinc/bin/kotlinc ./Main.kt -cp ${extra}ac_library.jar:kotlinx-collections-immutable-jvm.jar -include-runtime -jvm-target 23 -d ./Main.jar 2>compile.log
        java -Xss1024M -DONLINE_JUDGE=true -cp ${extra}ac_library.jar:kotlinx-collections-immutable-jvm.jar:Main.jar MainKt <input.txt >output.txt
      else
        /usr/bin/time -f '%e' -o compile-time.txt sh /judge/compile.sh 2>compile.log
        sh /judge/kotlin.sh 1024 <input.txt >output.txt
      fi"
    # The paste form holds library code in MainKt, so the library rules apply; the jar form has only user code.
    if [ "$form" = paste ]; then mode=--library; else mode=--strict; fi
    if ! "$checker" "$mode" --class-prefix MainKt --class-prefix Int --class-prefix Long --class-prefix Double "$dir/Main.jar" >"$dir/boxing.log"; then
      echo "FAIL $name/$form: boxing"; cat "$dir/boxing.log"; failures=$((failures + 1))
    fi
    echo "$name/$form: compile $(cat "$dir/compile-time.txt")s, source $(wc -c <"$dir/Main.kt" | tr -d ' ') bytes"
  done
  if ! cmp -s "$work/$name/paste/output.txt" "$work/$name/jar/output.txt"; then
    echo "FAIL $name: paste and jar outputs differ"; failures=$((failures + 1))
  fi
  # A checked-in expected output catches a behavior change that affects both forms alike.
  if ! cmp -s "$sample/expected.txt" "$work/$name/paste/output.txt"; then
    echo "FAIL $name: output differs from expected.txt"; diff "$sample/expected.txt" "$work/$name/paste/output.txt" | head -20; failures=$((failures + 1))
  fi
done

if [ "$failures" -ne 0 ]; then
  echo "$failures E2E failures."
  exit 1
fi
echo "E2E passed."
