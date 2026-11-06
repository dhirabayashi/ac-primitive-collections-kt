// SPDX-License-Identifier: CC0-1.0
package bytecodecheck

import java.io.File
import java.util.zip.ZipFile
import kotlin.system.exitProcess

private const val USAGE = "Usage: bytecode-check (--library | --strict) [--class-prefix PREFIX]... PATH\n" +
    "PATH is a jar or a directory of class files. With --class-prefix, only matching classes are checked."

private fun classFiles(path: File): Sequence<Pair<String, ByteArray>> =
    if (path.isDirectory) {
        path.walkTopDown().filter { it.extension == "class" }
            .map { it.relativeTo(path).invariantSeparatorsPath to it.readBytes() }
    } else {
        sequence {
            ZipFile(path).use { zip ->
                for (entry in zip.entries()) {
                    if (entry.name.endsWith(".class")) yield(entry.name to zip.getInputStream(entry).readBytes())
                }
            }
        }
    }

fun main(args: Array<String>) {
    var mode: Mode? = null
    val prefixes = mutableListOf<String>()
    var path: File? = null
    var i = 0
    while (i < args.size) {
        when (val arg = args[i]) {
            "--library" -> mode = Mode.LIBRARY
            "--strict" -> mode = Mode.STRICT
            "--class-prefix" -> prefixes += args[++i]
            else -> path = File(arg)
        }
        i++
    }
    val chosenMode = mode
    val chosenPath = path
    if (chosenMode == null || chosenPath == null || !chosenPath.exists()) {
        System.err.println(USAGE)
        exitProcess(2)
    }
    var checked = 0
    val findings = classFiles(chosenPath)
        .filter { (name, _) -> prefixes.isEmpty() || prefixes.any(name::startsWith) }
        .flatMap { (_, bytes) ->
            checked++
            BoxingChecker.check(bytes, chosenMode)
        }
        .toList()
    findings.forEach(::println)
    if (checked == 0) {
        System.err.println("No classes matched in $chosenPath, so nothing was checked.")
        exitProcess(1)
    }
    println("Checked $checked classes in $chosenPath ($chosenMode): ${findings.size} findings.")
    if (findings.isNotEmpty()) exitProcess(1)
}
