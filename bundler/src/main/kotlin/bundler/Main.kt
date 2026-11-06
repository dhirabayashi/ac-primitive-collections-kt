// SPDX-License-Identifier: CC0-1.0
package bundler

import java.io.File
import kotlin.system.exitProcess

private const val USAGE = """Usage: bundler [options] [Main.kt]
  The input defaults to Main.kt in the current directory.
  -o, --output FILE      Write here instead of standard output.
  --root DIR             The library repository. Defaults to AC_PRIMITIVE_COLLECTIONS_ROOT, then the current directory.
  -p, --allow-unpublished  Practice mode: allow a library state with no pushed tag. Never use this in a contest.
  --strip-comments       Remove comments and blank lines from the output."""

fun loadLibrary(root: File): List<SourceFile> {
    val directories = listOf("generated/main/kotlin", "src/main/kotlin").map(root::resolve)
    val files = directories.flatMap { directory -> directory.walkTopDown().filter { it.extension == "kt" }.toList() }
    if (files.isEmpty()) throw BundleException("No library sources under $root. Set --root.")
    return files.sortedBy { it.name }.map { SourceFile(it.name, it.readText()) }
}

fun main(args: Array<String>) {
    var output: File? = null
    var input: File? = null
    var root = File(System.getenv("AC_PRIMITIVE_COLLECTIONS_ROOT") ?: ".")
    var allowUnpublished = false
    var stripComments = false
    var i = 0
    while (i < args.size) {
        when (val arg = args[i]) {
            "-o", "--output" -> output = File(args[++i])
            "--root" -> root = File(args[++i])
            "-p", "--allow-unpublished" -> allowUnpublished = true
            "--strip-comments" -> stripComments = true
            else -> if (arg.startsWith("-")) {
                System.err.println(USAGE)
                exitProcess(2)
            } else {
                input = File(arg)
            }
        }
        i++
    }
    val mainFile = input ?: File("Main.kt")
    if (!mainFile.isFile) {
        System.err.println("error: ${mainFile.path} not found in ${File("").absolutePath}\n$USAGE")
        exitProcess(2)
    }
    try {
        val reference = Publication.referenceLine(ProcessGit(root), allowUnpublished)
        val result = Bundler.bundle(
            SourceFile(mainFile.name, mainFile.readText()),
            loadLibrary(root),
            BundleOptions(reference, stripComments),
        )
        result.warnings.forEach { System.err.println("warning: $it") }
        System.err.println("Included ${result.includedFiles.size} library files, ${result.source.toByteArray().size} bytes: ${result.includedFiles.joinToString()}")
        if (output != null) output.writeText(result.source) else print(result.source)
    } catch (e: BundleException) {
        System.err.println("error: ${e.message}")
        exitProcess(1)
    }
}
