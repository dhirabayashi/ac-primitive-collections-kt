// SPDX-License-Identifier: CC0-1.0
package bundler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BundlerTest {
    private val options = BundleOptions("// Library source: URL")

    private fun lib(name: String, body: String, imports: String = "") =
        SourceFile(name, "// SPDX\n// header\npackage ac_primitive_collections\n$imports\n$body\n")

    private val intList = lib("IntList.kt", "class IntList { fun add(x: Int) = helper() }\ninternal fun helper() {}\n".trimIndent())
    private val sort = lib("IntListSort.kt", "fun interface IntComparator { fun compare(a: Int, b: Int): Int }\nfun IntList.sortWith(c: IntComparator) {}")
    private val subList = lib("IntSubList.kt", "fun IntList.subList(a: Int, b: Int) = IntSubList(this)\nclass IntSubList(val p: IntList)")
    private val tree = lib("IntTree.kt", "class IntTree")
    private val library = listOf(intList, sort, subList, tree)

    private fun main(body: String, imports: String = "import ac_primitive_collections.*") =
        SourceFile("Main.kt", "$imports\n\n$body\n")

    @Test
    fun includesOnlyWhatIsUsed() {
        val result = Bundler.bundle(main("fun main() { val a = IntList(); a.add(1) }"), library, options)
        assertEquals(listOf("IntList.kt"), result.includedFiles)
        assertFalse("IntTree" in result.source)
    }

    @Test
    fun followsDependenciesTransitively() {
        val result = Bundler.bundle(main("fun main() { println(1) }\nfun f(s: IntSubList) {}"), library, options)
        assertEquals(listOf("IntList.kt", "IntSubList.kt"), result.includedFiles)
    }

    @Test
    fun findsExtensionsByName() {
        val result = Bundler.bundle(main("fun f(a: IntList) = a.subList(0, 1)"), library, options)
        assertTrue("IntSubList.kt" in result.includedFiles)
    }

    @Test
    fun seesUsesInsideStringTemplates() {
        val direct = Bundler.bundle(main("fun f(a: Any) = \"x \${IntTree()} y\""), library, options)
        assertEquals(listOf("IntTree.kt"), direct.includedFiles)
        val constant = lib("Const.kt", "val answer = 42")
        val simple = Bundler.bundle(main("fun f() = \"\$answer\""), library + constant, options)
        assertEquals(listOf("Const.kt"), simple.includedFiles)
    }

    @Test
    fun aMemberCallInsideTheLibraryDoesNotPullInTheSameNamedExtension() {
        val owner = lib("L.kt", "class L { fun addAll(x: Int) {} ; fun twice() { addAll(1); addAll(2) } }")
        val extension = lib("Ext.kt", "fun L.addAll(x: Iterable<Int>) {}")
        val noUse = Bundler.bundle(main("fun main() { L().twice() }"), listOf(owner, extension), options)
        assertEquals(listOf("L.kt"), noUse.includedFiles)
        val userUse = Bundler.bundle(main("fun main() { L().addAll(listOf(1)) }"), listOf(owner, extension), options)
        assertEquals(listOf("Ext.kt", "L.kt"), userUse.includedFiles)
    }

    @Test
    fun aFileThatDeclaresBothStillDoesNotDependOnItself() {
        val both = lib("B.kt", "class B { fun f() {} }\nfun f(x: Int) {}")
        val user = lib("U.kt", "class U { fun g() = f(1) }")
        val result = Bundler.bundle(main("fun main() { U().g() }"), listOf(both, user), options)
        assertEquals(listOf("B.kt", "U.kt"), result.includedFiles)
    }

    @Test
    fun ignoresNamesInCommentsAndStrings() {
        val result = Bundler.bundle(main("// IntTree\n/* IntTree /* nested */ IntTree */\nfun f() = \"IntTree\""), library, options)
        assertEquals(emptyList(), result.includedFiles)
    }

    @Test
    fun removesPackageAndLibraryImportsAndKeepsOthers() {
        val result = Bundler.bundle(main("fun main() { IntList() }", "import java.io.File\nimport ac_primitive_collections.*"), library, options)
        assertFalse("package " in result.source)
        assertFalse("import ac_primitive_collections" in result.source)
        assertTrue("import java.io.File" in result.source)
        assertTrue(result.source.lines().first { it.isNotEmpty() }.startsWith("// SPDX-License-Identifier"))
        assertTrue("// Library source: URL" in result.source)
    }

    @Test
    fun codeWithoutTheLibraryImportGetsNothing() {
        val result = Bundler.bundle(main("fun main() { class IntList; IntList() }", ""), library, options)
        assertEquals(emptyList(), result.includedFiles)
    }

    @Test
    fun mergesFileAnnotationsToTheTop() {
        val result = Bundler.bundle(main("@file:Suppress(\"A\")\nimport ac_primitive_collections.*\nfun main() { IntList() }", ""), library, options)
        assertTrue(result.source.startsWith("@file:Suppress(\"A\")"))
    }

    @Test
    fun reportsClashingDeclarations() {
        val failure = assertFailsWith<BundleException> {
            Bundler.bundle(main("class IntList"), library, options)
        }
        assertTrue("IntList" in failure.message!!)
        val extension = assertFailsWith<BundleException> {
            Bundler.bundle(main("fun IntList.subList(a: Int, b: Int) = 1"), library, options)
        }
        assertTrue("IntList.subList" in extension.message!!)
    }

    @Test
    fun aSameNamedExtensionOnAnotherReceiverIsFine() {
        Bundler.bundle(main("fun IntArray.subList(a: Int) = 1\nfun main() { IntList().subList(0, 1) }"), library, options)
    }

    @Test
    fun rejectsInternalUse() {
        val failure = assertFailsWith<BundleException> {
            Bundler.bundle(main("fun main() { helper(); IntList() }"), library, options)
        }
        assertTrue("helper" in failure.message!!)
    }

    @Test
    fun rejectsImportAlias() {
        assertFailsWith<BundleException> {
            Bundler.bundle(main("fun main() {}", "import ac_primitive_collections.IntList as L"), library, options)
        }
    }

    @Test
    fun rejectsClashingImports() {
        val withImport = lib("Uses.kt", "class Uses(val r: Random)", "import kotlin.random.Random")
        val failure = assertFailsWith<BundleException> {
            Bundler.bundle(main("fun main() { Uses(TODO()) }", "import ac_primitive_collections.*\nimport java.util.Random"), library + withImport, options)
        }
        assertTrue("clash" in failure.message!!)
    }

    @Test
    fun warnsWhenUserCodeShadowsNamesTheLibraryUses() {
        val usesMaxOf = lib("Uses.kt", "class Uses { fun f() = maxOf(1, 2) }\nclass Holder(val b: StringBuilder)")
        val result = Bundler.bundle(main("fun maxOf(a: Int, b: Int) = 0\nclass StringBuilder\nfun main() { Uses() ; Holder(TODO()) }"), library + usesMaxOf, options)
        assertEquals(2, result.warnings.size)
    }

    @Test
    fun warnsAboutSize() {
        val result = Bundler.bundle(main("fun main() { IntList() }"), library, options.copy(sizeWarningBytes = 10))
        assertTrue(result.warnings.single().contains("threshold"))
    }

    @Test
    fun stripsComments() {
        val commented = lib("C.kt", "/** Doc IntTree. */\nclass C {\n\n    // note\n    val url = \"http://x\" // tail\n}")
        val result = Bundler.bundle(main("fun main() { C() }"), listOf(commented), options.copy(stripComments = true))
        assertFalse("Doc" in result.source)
        assertFalse("note" in result.source)
        assertFalse("tail" in result.source)
        assertTrue("http://x" in result.source)
    }

    @Test
    fun checksLibraryRules() {
        assertTrue(Bundler.checkLibrary(listOf(intList)).isEmpty())
        val star = lib("A.kt", "class A", "import java.util.*")
        assertTrue("star" in Bundler.checkLibrary(listOf(star)).single())
        val operator = lib("B.kt", "operator fun IntList.plus(x: Int) = 1")
        assertTrue("operator" in Bundler.checkLibrary(listOf(operator)).single())
        val p1 = lib("C.kt", "private fun same() = 1")
        val p2 = lib("D.kt", "private fun same() = 2")
        assertTrue("same" in Bundler.checkLibrary(listOf(p1, p2)).single())
        assertFailsWith<BundleException> { Bundler.bundle(main("fun main() {}"), listOf(star), options) }
    }
}

class SourceFileTest {
    private fun declarations(body: String) = SourceFile("X.kt", body).declarations

    @Test
    fun parsesDeclarationShapes() {
        val parsed = declarations(
            """
            @PublishedApi
            internal fun helper(): Nothing = TODO()
            inline fun <T> T.ext(block: (T) -> Unit) {}
            fun IntArray.lowerBound(x: Int): Int = 0
            operator fun Foo.plus(o: Foo) = o
            private const val LIMIT: Int = 3
            val IntArray.lastPos: Int get() = 0
            class Plain(val inner: Int) { fun member() {} ; val prop = 1 }
            data class Data(val a: Int)
            fun interface Cmp { fun compare(a: Int): Int }
            @JvmInline value class Wrap(val v: Long)
            object Obj
            typealias Alias = Int
            val anonymous = object : Runnable { override fun run() {} }
            val lambda = fun(a: Int) = a
            enum class Color { RED }
            annotation class Ann
            val reference = Plain::class
            """.trimIndent(),
        )
        assertEquals(
            listOf(
                "helper", "ext", "lowerBound", "plus", "LIMIT", "lastPos", "Plain", "Data", "Cmp", "Wrap", "Obj", "Alias",
                "anonymous", "lambda", "Color", "Ann", "reference",
            ),
            parsed.map { it.name },
        )
        val byName = parsed.associateBy { it.name }
        assertEquals(Declaration.Visibility.INTERNAL, byName.getValue("helper").visibility)
        assertEquals(Declaration.Visibility.PRIVATE, byName.getValue("LIMIT").visibility)
        assertEquals("IntArray", byName.getValue("lowerBound").receiver)
        assertEquals("IntArray", byName.getValue("lastPos").receiver)
        assertEquals("T", byName.getValue("ext").receiver)
        assertTrue(byName.getValue("plus").isOperator)
        assertTrue(byName.getValue("Plain").isType)
        assertEquals(null, byName.getValue("helper").receiver)
    }

    @Test
    fun handlesGenericReceivers() {
        val parsed = declarations("fun Iterable<Int>.toIntList(): IntList = TODO()\nfun Map<Int, List<Int>>.f() {}")
        assertEquals(listOf("toIntList", "f"), parsed.map { it.name })
        assertEquals("Iterable<Int>", parsed[0].receiver)
    }

    @Test
    fun readsHeaderParts() {
        val file = SourceFile(
            "X.kt",
            "// c\n@file:Suppress(\"A\")\n\npackage a.b\n\nimport x.Y\nimport z.*\n\n// doc\nclass Y2 { val y = Y() }\n",
        )
        assertEquals("a.b", file.packageName)
        assertEquals(listOf("x.Y", "z.*"), file.imports)
        assertEquals(listOf("@file:Suppress(\"A\")"), file.fileAnnotations)
        assertTrue(file.body.startsWith("// doc"))
        assertFalse("x" in file.identifiers)
        assertTrue("Y" in file.identifiers)
    }

    @Test
    fun lexerHandlesLiterals() {
        val tokens = lex("""val a = '"' ; val b = "x\"y ${'$'}{f("n${'$'}g") + 1} z" ; val c = '\''; val `odd name` = 0x1F""").tokens
        val identifiers = tokens.filter { it.isIdentifier }.map { it.text }
        assertEquals(listOf("val", "a", "val", "b", "f", "g", "val", "c", "val", "odd name"), identifiers)
    }

    @Test
    fun lexerHandlesRawStrings() {
        val source = "val a = \"\"\"x \" \$y \${z} \"\"\"\"\nval b = 1"
        val identifiers = lex(source).tokens.filter { it.isIdentifier }.map { it.text }
        assertEquals(listOf("val", "a", "y", "z", "val", "b"), identifiers)
    }

    @Test
    fun lexerKeepsBracesInTemplatesBalanced() {
        val identifiers = lex("val a = \"\${ run { b } } c\"\nval d = 1").tokens.filter { it.isIdentifier }.map { it.text }
        assertEquals(listOf("val", "a", "run", "b", "val", "d"), identifiers)
    }
}

class PublicationTest {
    private class FakeGit(val answers: Map<String, String?>) : Git {
        override fun run(vararg args: String): String? = answers[args.first()]
    }

    private val published = mapOf(
        "remote" to "git@github.com:me/repo.git",
        "status" to "",
        "tag" to "v0.1.0\nv0.0.9\nother",
        "rev-parse" to "abc",
        "ls-remote" to "tagobject\trefs/tags/v0.1.0\nabc\trefs/tags/v0.1.0^{}",
    )

    @Test
    fun pinsTheUrlToThePublishedTag() {
        assertEquals(
            "// Library source (created with generative AI): https://github.com/me/repo/tree/v0.1.0",
            Publication.referenceLine(FakeGit(published), allowUnpublished = false),
        )
    }

    @Test
    fun acceptsALightweightTag() {
        val git = FakeGit(published + ("ls-remote" to "abc\trefs/tags/v0.1.0"))
        assertTrue("tree/v0.1.0" in Publication.referenceLine(git, false))
    }

    private fun assertRejected(change: Pair<String, String?>, expected: String) {
        val git = FakeGit(published + change)
        val failure = assertFailsWith<BundleException> { Publication.referenceLine(git, false) }
        assertTrue(expected in failure.message!!, failure.message)
        assertTrue("UNPUBLISHED" in Publication.referenceLine(git, true))
    }

    @Test
    fun rejectsUncommittedChanges() = assertRejected("status" to " M src/main/x.kt", "uncommitted")

    @Test
    fun rejectsMissingStatus() = assertRejected("status" to null, "status")

    @Test
    fun rejectsAnUntaggedHead() = assertRejected("tag" to "", "no release tag")

    @Test
    fun rejectsAnUnpushedTag() = assertRejected("ls-remote" to "", "not on origin")

    @Test
    fun rejectsAMovedTag() = assertRejected("ls-remote" to "zzz\trefs/tags/v0.1.0", "somewhere other")

    @Test
    fun rejectsAnUnreachableRemote() = assertRejected("ls-remote" to null, "Cannot reach")

    @Test
    fun requiresARemote() {
        assertFailsWith<BundleException> { Publication.referenceLine(FakeGit(published + ("remote" to null)), true) }
    }

    @Test
    fun normalizesRemoteUrls() {
        assertEquals("https://github.com/o/r", Publication.httpsUrl("git@github.com:o/r.git"))
        assertEquals("https://github.com/o/r", Publication.httpsUrl("https://github.com/o/r.git"))
        assertEquals("https://github.com/o/r", Publication.httpsUrl("https://github.com/o/r"))
    }
}
