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

    /**
     * The bundler brings array extensions for an array that a called library function returns, and finds those
     * from the declared return types in the source (`bundler.Bundler`). An inferred one would be missed, and a
     * contest submission would then fail to compile. So every public method that returns a standard array must
     * declare it in the source.
     */
    @Test
    fun functionsThatReturnArraysDeclareItForTheBundler() {
        val parsed = sources(libraryRoots).associate { it.name to bundler.SourceFile(it.name, it.readText()) }
        val arrays = bundler.Bundler.STANDARD_ARRAYS.associateBy { Class.forName("[" + jvmDescriptor(it)) }
        val classes = File("build/classes/kotlin/main/ac_primitive_collections").listFiles { f -> f.extension == "class" }.orEmpty()
        var checked = 0
        val missing = classes.flatMap { file ->
            val type = Class.forName("ac_primitive_collections." + file.nameWithoutExtension)
            if (!java.lang.reflect.Modifier.isPublic(type.modifiers)) return@flatMap emptyList()
            // Top-level functions live in the facade class `<File>Kt`, and a class in the file that declares it.
            val outer = type.name.substringAfterLast('.').substringBefore('$')
            val source = parsed[outer.removeSuffix("Kt") + ".kt"]?.takeIf { outer.endsWith("Kt") }
                ?: parsed.values.single { f -> f.declarations.any { it.isType && it.name == outer } }
            val internal = source.declarations.filter { it.visibility != bundler.Declaration.Visibility.PUBLIC }.map { it.name }.toSet()
            type.declaredMethods.filter { java.lang.reflect.Modifier.isPublic(it.modifiers) && it.returnType in arrays && it.name !in internal }.mapNotNull { method ->
                checked++
                val array = arrays.getValue(method.returnType)
                if (array in source.publicReturnTypes[method.name].orEmpty()) null else "${source.name}: ${method.name} returns $array"
            }
        }
        assertEquals(emptyList(), missing.distinct())
        assertTrue(checked > 0, "Some method should return an array")
    }

    private val arrayExtensionClass = Regex("""(?:Int|Long|Double|Char|Boolean|Byte|Short|Float)ArrayExtensionsKt""")

    /**
     * The bundler matches an array extension only against the user's names, not the library's (`bundler.Bundler`),
     * so no other library class may call one: a contest submission would then fail to compile. A class that calls
     * or inlines one names its class in the constant pool or in the inline debug map, so the bytes show it.
     */
    @Test
    fun onlyTheUsersCodeCallsArrayExtensions() {
        val classes = File("build/classes/kotlin/main/ac_primitive_collections").listFiles { f -> f.extension == "class" }.orEmpty()
        var facades = 0
        val problems = classes.flatMap { file ->
            val own = file.nameWithoutExtension.substringBefore('$')
            if (arrayExtensionClass.matches(own)) facades++
            val named = arrayExtensionClass.findAll(String(file.readBytes(), Charsets.ISO_8859_1)).map { it.value }.toSet()
            (named - own).map { "${file.name} refers to $it" }
        }
        assertEquals(emptyList(), problems)
        assertEquals(4, facades, "Each element type should have its array extensions")
    }

    private fun jvmDescriptor(array: String) = when (array.removeSuffix("Array")) {
        "Int" -> "I"
        "Long" -> "J"
        "Double" -> "D"
        "Char" -> "C"
        "Boolean" -> "Z"
        "Byte" -> "B"
        "Short" -> "S"
        else -> "F"
    }

    private val iterableOfProgressionType = Regex("""Iterable<(Int|Long|Char)>""")

    /**
     * An `Iterable` overload boxes, and Kotlin picks it without a warning for `1..n step 2` and `n downTo 1`
     * unless an overload for the progression type exists (docs/PLAN.md, section 4.6). The boxing happens inside
     * the library, so the check on user code cannot see it.
     */
    @Test
    fun iterableOverloadsHaveProgressionOverloads() {
        var checked = 0
        val missing = sources(libraryRoots).flatMap { file ->
            val lines = file.readLines().map { it.trim() }.toSet()
            lines.filter { it.startsWith("fun ") && iterableOfProgressionType.containsMatchIn(it) }.mapNotNull { line ->
                checked++
                val wanted = iterableOfProgressionType.replace(line) { "${it.groupValues[1]}Progression" }
                if (wanted in lines) null else "${file.path}: $wanted"
            }
        }
        assertEquals(emptyList(), missing)
        assertTrue(checked > 0, "The pattern should find something")
    }
}
