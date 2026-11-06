#!/usr/bin/env python3
# Writes files that repeat the bundled IntList under new names, to find AtCoder's source length
# and compile time limits (docs/PLAN.md, section 15). Submit each to the code test.
# Usage: size-probe.py BUNDLED_MAIN_KT OUTPUT_DIR
import pathlib
import re
import sys

source = pathlib.Path(sys.argv[1]).read_text()
out = pathlib.Path(sys.argv[2])
out.mkdir(parents=True, exist_ok=True)

# Keep the real program and library, and append renamed copies of IntList.
marker = "// ---- ac-primitive-collections-kt: IntList.kt ----"
start = source.index(marker)
# The next file's marker, or the end of the file if IntList.kt comes last.
next_marker = source.find("// ---- ac-primitive-collections-kt:", start + len(marker))
end = next_marker if next_marker >= 0 else len(source)
library = source[start:end]

for target_kib in (64, 128, 256, 512, 1024):
    text = source
    copy = 0
    while len(text.encode()) < target_kib * 1024:
        copy += 1
        text += "\n" + re.sub(r"\b(intListOf|buildIntList|IntList)\b", lambda m: f"{m.group(1)}{copy}", library)
    path = out / f"Main-{target_kib}KiB.kt"
    path.write_text(text)
    print(f"{path} {len(text.encode())} bytes, {copy} extra copies")
