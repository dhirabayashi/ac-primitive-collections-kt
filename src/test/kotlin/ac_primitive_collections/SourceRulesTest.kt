// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Source rules from docs/PLAN.md, sections 4.5, 5.8.2, and 8.3. */
class SourceRulesTest {
    private val roots = listOf("generated/main/kotlin", "src/main/kotlin", "templates").map(::File)
    private val libraryRoots = listOf("generated/main/kotlin", "src/main/kotlin").map(::File)

    private fun sources(from: List<File>) = from.flatMap { root -> root.walkTopDown().filter { it.extension == "kt" }.toList() }

    private val forbidden = listOf(
        Regex("""\(\s*\w+\s*\+\s*\w+\s*\)\s*/\s*2""") to "midpoint overflow: use ushr 1",
        Regex("""\bcapacity\s*\*\s*2|\bcap\s*\*\s*2|\bcap\s*\+\s*cap\b""") to "capacity overflow: use nextCapacity",
        Regex("""\bhashCode\(\)\s*%""") to "negative modulo: use and (n - 1) or floorMod",
        Regex("""(?m)^\s*import\s+.*\*\s*$""") to "star imports change name resolution in pasted code",
        Regex("""Array\s*\(\s*Int\.MAX_VALUE""") to "array size limit: use MAX_ARRAY_SIZE",
        Regex("""\bfun\s+\w+\s*\([^)]*\)\s*:\s*(Int|Long|Double|Float|Short|Byte|Char|Boolean)\?""") to "Int? boxes",
    )

    @Test
    fun forbiddenPatternsAreAbsent() {
        val problems = sources(roots).flatMap { file ->
            val text = file.readText()
            forbidden.mapNotNull { (pattern, reason) -> pattern.find(text)?.let { "${file.path}: ${it.value} ($reason)" } }
        }
        assertEquals(emptyList(), problems)
    }

    private val topLevelPrivate = Regex("""(?m)^private\s+(?:(?:inline|const|tailrec)\s+)*(?:fun|val|var)\s+(?:<[^>]+>\s*)?(?:[\w.<>?]+\.)?(\w+)""")

    @Test
    fun topLevelPrivateNamesAreUniqueAcrossFiles() {
        val names = sources(libraryRoots).flatMap { file -> topLevelPrivate.findAll(file.readText()).map { it.groupValues[1] to file.name } }
        val duplicates = names.groupBy({ it.first }, { it.second }).filter { it.value.distinct().size > 1 }
        assertEquals(emptyMap(), duplicates)
        assertTrue(names.isNotEmpty(), "The pattern should find something")
    }

    private val topLevelType = Regex("""(?m)^(?:(?:public|internal|data|value|annotation|fun|abstract|sealed|enum)\s+)*(?:class|interface|object|typealias)\s+(\w+)""")

    @Test
    fun publicNamesDoNotClashWithTheJudgeEnvironment() {
        val ours = sources(libraryRoots).flatMap { file -> topLevelType.findAll(file.readText()).map { it.groupValues[1] } }.toSet()
        assertTrue("IntList" in ours)
        val judge = File("e2e/judge/judge-library-classes.txt").readLines().filterNot { it.startsWith("#") }.toSet()
        val packages = listOf("java.util.", "java.lang.", "kotlin.", "kotlin.collections.", "kotlin.ranges.", "kotlin.text.", "kotlin.random.", "java.util.function.")
        val clashes = ours.filter { name ->
            name in judge || packages.any { runCatching { Class.forName(it + name) }.isSuccess }
        }
        assertEquals(emptyList(), clashes)
    }
}
