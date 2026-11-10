// SPDX-License-Identifier: CC0-1.0
package bundler

import kotlin.io.path.createTempDirectory
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
    fun aMemberThatHandsItsWorkToASameNamedExtensionPullsItIn() {
        // The shape of SubList.sortWith, which calls the list's sortWith extension, with no other name to lead there.
        val list = lib("L.kt", "class L")
        val view = lib("V.kt", "class V(val parent: L) { fun sortAll() { parent.sortAll(0) } }")
        val extension = lib("Ext.kt", "fun L.sortAll(from: Int) {}")
        val result = Bundler.bundle(main("fun main() { V(L()).sortAll() }"), listOf(list, view, extension), options)
        assertEquals(listOf("Ext.kt", "L.kt", "V.kt"), result.includedFiles)
    }

    @Test
    fun anExtensionOnALibraryTypeNeedsThatType() {
        val intType = lib("IntL.kt", "class IntL\nfun intLOf() = IntL()")
        val longType = lib("LongL.kt", "class LongL\nfun longLOf() = LongL()")
        val intSort = lib("IntLSort.kt", "fun IntL.sortBy() {}")
        val longSort = lib("LongLSort.kt", "fun LongL.sortBy() {}")
        val all = listOf(intType, longType, intSort, longSort)
        val one = Bundler.bundle(main("fun main() { intLOf().sortBy() }"), all, options)
        assertEquals(listOf("IntL.kt", "IntLSort.kt"), one.includedFiles)
        val both = Bundler.bundle(main("fun main() { intLOf().sortBy(); longLOf() }"), all, options)
        assertEquals(listOf("IntL.kt", "IntLSort.kt", "LongL.kt", "LongLSort.kt"), both.includedFiles)
        assertEquals(emptyList(), Bundler.bundle(main("fun main() { listOf(1).sortBy() }"), all, options).includedFiles)
    }

    @Test
    fun anExtensionThatReturnsAnotherTypeBringsThatTypesExtensions() {
        val intType = lib("IntL.kt", "class IntL")
        val longType = lib("LongL.kt", "class LongL")
        val convert = lib("IntLToLong.kt", "fun IntL.toLong() = LongL()")
        val intSort = lib("IntLSort.kt", "fun IntL.sortBy() {}")
        val longSort = lib("LongLSort.kt", "fun LongL.sortBy() {}")
        val result = Bundler.bundle(main("fun main() { IntL().toLong().sortBy() }"), listOf(longSort, intSort, convert, longType, intType), options)
        assertEquals(listOf("IntL.kt", "IntLSort.kt", "IntLToLong.kt", "LongL.kt", "LongLSort.kt"), result.includedFiles)
    }

    @Test
    fun anExtensionOnAnOutsideTypeNeedsOnlyItsName() {
        val generic = lib("G.kt", "fun Iterable<Int>.toL() = 1\nfun <T> T.ext() = 2")
        assertEquals(listOf("G.kt"), Bundler.bundle(main("fun main() { listOf(1).toL() }"), listOf(generic), options).includedFiles)
        assertEquals(listOf("G.kt"), Bundler.bundle(main("fun main() { 1.ext() }"), listOf(generic), options).includedFiles)
    }

    @Test
    fun anExtensionOnAStandardArrayNeedsANameThatMakesThatArray() {
        val intArrays = lib("IntArrayExtensions.kt", "fun IntArray.lowerBound(x: Int) = 0")
        val longArrays = lib("LongArrayExtensions.kt", "fun LongArray.lowerBound(x: Long) = 0")
        val list = lib(
            "LongL.kt",
            "class LongL(val a: LongArray) { fun lowerBound(x: Long) = 0\nfun order(): IntArray = IntArray(1)\nfun toLongArray(): LongArray = a\n" +
                "fun addAll(b: IntArray) {}\nprivate fun keys(): IntArray = IntArray(1)\nfun sort() { val keys = IntArray(1) } }",
        )
        val all = listOf(intArrays, longArrays, list)
        fun included(body: String) = Bundler.bundle(main(body), all, options).includedFiles
        // The list's own lowerBound, its arrays, and its private function bring no array extensions.
        assertEquals(listOf("LongL.kt"), included("fun main() { LongL(x).lowerBound(1L) }"))
        // Nor does an array next to it, since only the list's code names lowerBound.
        assertEquals(listOf("LongL.kt"), included("fun main() { LongL(IntArray(3)).order() }"))
        assertEquals(listOf("LongL.kt"), included("fun main() { LongL(LongArray(3)).toLongArray() }"))
        // A function that the user calls brings its return type's.
        assertEquals(listOf("IntArrayExtensions.kt", "LongL.kt"), included("fun main() { LongL(x).order().lowerBound(1) }"))
        assertEquals(listOf("LongArrayExtensions.kt", "LongL.kt"), included("fun main() { LongL(x).toLongArray().lowerBound(1L) }"))
        assertEquals(listOf("IntArrayExtensions.kt"), included("fun main() { IntArray(3).lowerBound(1) }"))
        assertEquals(listOf("IntArrayExtensions.kt"), included("fun main() { intArrayOf(1).lowerBound(1) }"))
        assertEquals(listOf("IntArrayExtensions.kt"), included("fun main() { listOf(1).toIntArray().lowerBound(1) }"))
        assertEquals(listOf("IntArrayExtensions.kt", "LongArrayExtensions.kt"), included("fun f(a: IntArray, b: LongArray) = a.lowerBound(1) + b.lowerBound(1)"))
        assertEquals(emptyList(), included("fun f(a: IntArray) = a.size"))
        assertEquals(emptyList(), included("fun f(a: List<Int>) = a.lowerBound(1)"))
    }

    @Test
    fun publicFunctionsReportTheirReturnTypes() {
        val file = lib(
            "F.kt",
            "fun <T> IntArray.a(x: (Int) -> Long = { 0L }): LongArray = LongArray(1)\n" +
                "class C { inline fun b(): Array<IntArray> { return arrayOf() }\n" +
                "internal fun c(): IntArray = IntArray(0)\nfun d() = 1\nfun e(): Int\nfun f(): DoubleArray }",
        )
        assertEquals(setOf("LongArray"), file.publicReturnTypes["a"])
        assertEquals(setOf("Array", "IntArray"), file.publicReturnTypes["b"])
        assertEquals(null, file.publicReturnTypes["c"])
        assertEquals(emptySet(), file.publicReturnTypes["d"])
        assertEquals(setOf("Int"), file.publicReturnTypes["e"])
        assertEquals(setOf("DoubleArray"), file.publicReturnTypes["f"])
    }

    @Test
    fun aNullableReceiverCountsAsItsType() {
        val owner = lib("N.kt", "class N")
        val extension = lib("NExt.kt", "fun N?.orNew() = this")
        assertEquals(emptyList(), Bundler.bundle(main("fun main() { x.orNew() }"), listOf(owner, extension), options).includedFiles)
        assertEquals(listOf("N.kt", "NExt.kt"), Bundler.bundle(main("fun main() { N().orNew() }"), listOf(owner, extension), options).includedFiles)
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
    fun warnsAboutInternalUse() {
        val result = Bundler.bundle(main("fun main() { helper(); IntList() }"), library, options)
        assertTrue("helper" in result.warnings.single(), result.warnings.toString())
    }

    /** The scan cannot tell a local variable from the internal function, so it must not stop a submission. */
    @Test
    fun aLocalVariableNamedLikeAnInternalDoesNotStopTheBundle() {
        val result = Bundler.bundle(main("fun main() { val helper = 1; println(IntList().add(helper)) }"), library, options)
        assertTrue("helper" in result.warnings.single(), result.warnings.toString())
        assertTrue("val helper = 1" in result.source)
    }

    @Test
    fun codeWithoutTheLibraryImportGetsNoInternalWarning() {
        assertEquals(emptyList(), Bundler.bundle(main("fun main() { val helper = 1 }", ""), library, options).warnings)
    }

    @Test
    fun aTopLevelNameOfTheUsersOwnIsNotInternalUse() {
        assertEquals(emptyList(), Bundler.bundle(main("val answer = 1\nfun main() { println(answer) }"), library, options).warnings)
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
    private class FakeGit(val answers: Map<String, String?>, val files: Map<String, String> = emptyMap()) : Git {
        override fun run(vararg args: String): String? =
            if (args.first() == "show") files[args[1]] else answers[args.first()]
    }

    private val pushed = mapOf(
        "remote" to "git@github.com:me/repo.git",
        "rev-parse" to "abc",
        "ls-remote" to "tagobject\trefs/tags/v0.1.0\nabc\trefs/tags/v0.1.0^{}",
    )

    private fun latest(lsRemote: String) = Publication.latestRelease(FakeGit(mapOf("ls-remote" to lsRemote)))

    @Test
    fun usesTheCommitOfAnAnnotatedTag() {
        assertEquals(Release("v0.1.0", "abc"), Publication.latestRelease(FakeGit(pushed)))
    }

    @Test
    fun acceptsALightweightTag() {
        assertEquals(Release("v0.1.0", "abc"), latest("abc\trefs/tags/v0.1.0"))
    }

    @Test
    fun comparesVersionsAsNumbers() {
        assertEquals("v0.10.0", latest("a\trefs/tags/v0.10.0\nb\trefs/tags/v0.9.0").tag)
        assertEquals("v1.0.1", latest("a\trefs/tags/v1.0\nb\trefs/tags/v1.0.1").tag)
        assertEquals("v1.1", latest("a\trefs/tags/v1.1\nb\trefs/tags/v1.0.5").tag)
    }

    @Test
    fun ignoresTagsThatAreNotReleases() {
        assertEquals("v0.1.0", latest("a\trefs/tags/v0.1.0\nb\trefs/tags/v9.0.0-rc1\nc\trefs/tags/other\nd\trefs/tags/vnext").tag)
    }

    @Test
    fun rejectsAnOriginWithoutReleases() {
        val failure = assertFailsWith<BundleException> { latest("a\trefs/tags/other") }
        assertTrue("no release tag" in failure.message!!, failure.message)
        assertFailsWith<BundleException> { latest("") }
    }

    @Test
    fun rejectsAnUnreachableRemote() {
        val failure = assertFailsWith<BundleException> { Publication.latestRelease(FakeGit(pushed + ("ls-remote" to null))) }
        assertTrue("Cannot reach" in failure.message!!, failure.message)
    }

    @Test
    fun readsSourcesFromTheReleaseCommit() {
        val git = FakeGit(
            mapOf("ls-tree" to "generated/main/kotlin/ac_primitive_collections/B.kt\nsrc/main/kotlin/A.kt\nsrc/main/kotlin/notes.txt"),
            mapOf("abc:generated/main/kotlin/ac_primitive_collections/B.kt" to "class B", "abc:src/main/kotlin/A.kt" to "class A"),
        )
        val files = Publication.loadRelease(git, Release("v0.1.0", "abc"))
        assertEquals(listOf("A.kt" to "class A", "B.kt" to "class B"), files.map { it.name to it.text })
    }

    @Test
    fun tellsHowToGetACommitThatIsNotHere() {
        val noCommit = FakeGit(mapOf("ls-tree" to null))
        val failure = assertFailsWith<BundleException> { Publication.loadRelease(noCommit, Release("v0.1.0", "abc")) }
        assertTrue("git fetch --tags origin" in failure.message!!, failure.message)
        val noFile = FakeGit(mapOf("ls-tree" to "src/main/kotlin/A.kt"))
        assertFailsWith<BundleException> { Publication.loadRelease(noFile, Release("v0.1.0", "abc")) }
    }

    @Test
    fun rejectsATagWithoutSources() {
        val failure = assertFailsWith<BundleException> { Publication.loadRelease(FakeGit(mapOf("ls-tree" to "")), Release("v0.1.0", "abc")) }
        assertTrue("no library sources" in failure.message!!, failure.message)
    }

    @Test
    fun pinsTheUrlToTheTag() {
        assertEquals(
            "// Library source (created with generative AI): https://github.com/me/repo/tree/v0.1.0",
            Publication.referenceLine(FakeGit(pushed), Release("v0.1.0", "abc")),
        )
    }

    @Test
    fun marksPracticeOutputAsUnpublished() {
        assertTrue("UNPUBLISHED local state at abc" in Publication.unpublishedReferenceLine(FakeGit(pushed)))
        assertTrue("unknown" in Publication.unpublishedReferenceLine(FakeGit(pushed + ("rev-parse" to null))))
    }

    @Test
    fun requiresARemote() {
        val noRemote = FakeGit(pushed + ("remote" to null))
        assertFailsWith<BundleException> { Publication.referenceLine(noRemote, Release("v0.1.0", "abc")) }
        assertFailsWith<BundleException> { Publication.unpublishedReferenceLine(noRemote) }
    }

    private val userCode = SourceFile("Main.kt", "import ac_primitive_collections.*\n\nfun helper() = Newer()\n")

    private fun differences(main: SourceFile, included: List<String>, published: List<SourceFile>, local: List<SourceFile>) =
        Publication.localDifferenceWarnings(main, included, published, local, "v0.1.0")

    @Test
    fun saysNothingWhenTheLocalLibraryMatches() {
        val file = SourceFile("A.kt", "class A\n")
        assertEquals(emptyList(), differences(userCode, listOf("A.kt"), listOf(file), listOf(SourceFile("A.kt", "class A"))))
    }

    @Test
    fun warnsOnlyAboutChangedFilesInTheSubmission() {
        val published = listOf(SourceFile("A.kt", "class A"), SourceFile("B.kt", "class B"))
        val local = listOf(SourceFile("A.kt", "class A { val x = 1 }"), SourceFile("B.kt", "class B { val x = 1 }"))
        val warning = differences(userCode, listOf("A.kt"), published, local).single()
        assertTrue("v0.1.0" in warning && "A.kt" in warning && "B.kt" !in warning, warning)
    }

    @Test
    fun saysNothingAboutAnIncludedFileThatIsGoneLocally() {
        assertEquals(emptyList(), differences(userCode, listOf("A.kt"), listOf(SourceFile("A.kt", "class A")), emptyList()))
    }

    @Test
    fun warnsAboutNamesThatOnlyTheLocalLibraryHas() {
        val published = listOf(SourceFile("A.kt", "class A"))
        val local = published + SourceFile("Newer.kt", "class Newer\nclass Unused")
        val warning = differences(userCode, emptyList(), published, local).single()
        assertTrue("Newer" in warning && "Unused" !in warning, warning)
    }

    @Test
    fun aNameTheUserDeclaresIsNotANewerLibraryName() {
        val main = SourceFile("Main.kt", "import ac_primitive_collections.*\n\nclass Newer\n")
        assertEquals(emptyList(), differences(main, emptyList(), emptyList(), listOf(SourceFile("Newer.kt", "class Newer"))))
    }

    @Test
    fun codeWithoutTheLibraryImportHasNoDifferences() {
        val main = SourceFile("Main.kt", "fun main() { Newer() }\n")
        assertEquals(emptyList(), differences(main, emptyList(), emptyList(), listOf(SourceFile("Newer.kt", "class Newer"))))
    }

    @Test
    fun normalizesRemoteUrls() {
        assertEquals("https://github.com/o/r", Publication.httpsUrl("git@github.com:o/r.git"))
        assertEquals("https://github.com/o/r", Publication.httpsUrl("https://github.com/o/r.git"))
        assertEquals("https://github.com/o/r", Publication.httpsUrl("https://github.com/o/r"))
    }
}

/** Runs the real git, because a fake cannot catch wrong arguments. */
class PublicationGitTest {
    @Test
    fun readsThePushedTagWhateverTheWorkingTreeHolds() {
        val base = createTempDirectory("publication").toFile()
        try {
            val origin = base.resolve("origin.git").apply { mkdirs() }
            val work = base.resolve("work").apply { mkdirs() }
            val source = work.resolve("generated/main/kotlin/ac_primitive_collections/A.kt")
            source.parentFile.mkdirs()
            fun git(directory: java.io.File, vararg args: String) {
                val identity = arrayOf("-c", "user.name=t", "-c", "user.email=t@example.invalid", "-c", "commit.gpgsign=false", "-c", "tag.gpgsign=false")
                assertTrue(ProcessGit(directory).run(*identity, *args) != null, args.joinToString(" "))
            }
            git(origin, "init", "--bare")
            git(work, "init")
            git(work, "remote", "add", "origin", origin.path)
            source.writeText("class Published\n")
            git(work, "add", ".")
            git(work, "commit", "-m", "first")
            git(work, "tag", "-a", "v0.1.0", "-m", "first")
            git(work, "push", "origin", "HEAD", "--tags")
            source.writeText("class Committed\n")
            git(work, "commit", "-am", "second")
            git(work, "tag", "v0.2.0")
            source.writeText("class Uncommitted\n")

            val gitInWork = ProcessGit(work)
            val release = Publication.latestRelease(gitInWork)
            assertEquals("v0.1.0", release.tag)
            val file = Publication.loadRelease(gitInWork, release).single()
            assertEquals("A.kt", file.name)
            assertEquals("class Published", file.text.trim())
        } finally {
            base.deleteRecursively()
        }
    }
}
