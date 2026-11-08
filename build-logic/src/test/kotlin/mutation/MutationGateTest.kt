// SPDX-License-Identifier: CC0-1.0
package mutation

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MutationGateTest {
    private val templates = TemplateMap(
        mapOf(
            "IntIntMaxHeap.kt" to GeneratedFile("KTypeVTypeMinHeap.kt", "Int", "Int"),
            "LongIntMinHeap.kt" to GeneratedFile("KTypeVTypeMinHeap.kt", "Long", "Int"),
            "LongListSort.kt" to GeneratedFile("KTypeListSort.kt", "Long", null),
            "IntListSort.kt" to GeneratedFile("KTypeListSort.kt", "Int", null),
            "IntList.kt" to GeneratedFile("KTypeList.kt", "Int", null),
            "IntListConversions.kt" to GeneratedFile("KTypeListConversions.kt", "Int", null),
            "IntListMapToLong.kt" to GeneratedFile("KTypeListMapToVType.kt", "Int", "Long"),
        ),
    )

    private fun mutation(
        file: String,
        method: String = "add",
        description: String = "changed conditional boundary",
        line: Int = 1,
        detected: Boolean = false,
    ) = Mutation(file, method, "ConditionalsBoundaryMutator", description, line, if (detected) "KILLED" else "SURVIVED", detected)

    private fun key(template: String, method: String = "add", description: String = "changed conditional boundary") =
        MutationKey(template, method, "ConditionalsBoundaryMutator", description)

    // ---- Which template and types made a file ----

    @Test
    fun readsBothTypesOfAPairTemplateInEitherDirection() {
        assertEquals(GeneratedFile("KTypeVTypeMinHeap.kt", "Int", "Obj"), TemplateMap.parse("IntObjMaxHeap.kt", "KTypeVTypeMinHeap.kt"))
        assertEquals(GeneratedFile("KTypeVTypeMinHeap.kt", "Double", "Long"), TemplateMap.parse("DoubleLongMinHeap.kt", "KTypeVTypeMinHeap.kt"))
    }

    @Test
    fun readsAConversionTargetAfterTheElementType() {
        assertEquals(GeneratedFile("KTypeListMapToVType.kt", "Char", "Double"), TemplateMap.parse("CharListMapToDouble.kt", "KTypeListMapToVType.kt"))
    }

    @Test
    fun readsASingleTypeTemplate() {
        assertEquals(GeneratedFile("KTypeListSort.kt", "Double", null), TemplateMap.parse("DoubleListSort.kt", "KTypeListSort.kt"))
    }

    @Test
    fun rejectsAFileThatDoesNotMatchItsTemplate() {
        assertFailsWith<IllegalArgumentException> { TemplateMap.parse("IntSubList.kt", "KTypeList.kt") }
    }

    @Test
    fun readsTheHeaderAndSkipsHandWrittenFiles() {
        val dir = Files.createTempDirectory("mutation-gate").toFile()
        try {
            dir.resolve("IntIntMaxHeap.kt").writeText("// SPDX\n// Generated from templates/KTypeVTypeMinHeap.kt. Do not edit.\nclass A\n")
            dir.resolve("Support.kt").writeText("// SPDX\npackage x\n")
            dir.resolve("Empty.kt").writeText("")
            val map = TemplateMap.read(dir)
            assertEquals(key("KTypeVTypeMinHeap.kt"), map.key(mutation("IntIntMaxHeap.kt")))
            assertEquals(key("Support.kt"), map.key(mutation("Support.kt")))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun readsEveryCommittedGeneratedFile() {
        val dir = File("../generated/main/kotlin/ac_primitive_collections")
        val names = dir.listFiles { file -> file.extension == "kt" }!!.map { it.name }
        val map = TemplateMap.read(dir)
        for (name in names) {
            assertTrue(map.key(mutation(name)).template.startsWith("KType"), name)
        }
    }

    // ---- Keys ----

    @Test
    fun replacesTheElementTypeInAMethodName() {
        assertEquals(key("KTypeListSort.kt", "mergeSortKType"), templates.key(mutation("LongListSort.kt", "mergeSortLong")))
        assertEquals(key("KTypeListSort.kt", "mergeSortKType"), templates.key(mutation("IntListSort.kt", "mergeSortInt")))
    }

    @Test
    fun replacesTheValueTypeInAMethodName() {
        assertEquals(key("KTypeListMapToVType.kt", "toVTypeList"), templates.key(mutation("IntListMapToLong.kt", "toLongList")))
    }

    @Test
    fun leavesATypeNameInsideAWordAlone() {
        assertEquals(key("KTypeList.kt", "Interval"), templates.key(mutation("IntList.kt", "Interval")))
    }

    @Test
    fun leavesHandWrittenMethodNamesAlone() {
        assertEquals(key("KeySort.kt", "sortByLong"), templates.key(mutation("KeySort.kt", "sortByLong")))
    }

    @Test
    fun replacesGeneratedClassNamesInADescription() {
        fun described(text: String) = templates.key(mutation("IntList.kt", description = text)).description
        assertEquals("removed call to ac_primitive_collections/KTypeList::openGap", described("removed call to ac_primitive_collections/IntList::openGap"))
        assertEquals("removed call to ac_primitive_collections/KTypeListConversionsKt::x", described("removed call to ac_primitive_collections/IntListConversionsKt::x"))
        assertEquals("removed call to ac_primitive_collections/KTypeList\$Itr::next", described("removed call to ac_primitive_collections/IntList\$Itr::next"))
        assertEquals("removed call to ac_primitive_collections/SupportKt::checkRange", described("removed call to ac_primitive_collections/SupportKt::checkRange"))
        assertEquals("replaced long return with 0 for x", described("replaced long return with 0 for x"))
    }

    @Test
    fun givesEveryDirectionAndTypeOfAPairTheSameKey() {
        assertEquals(templates.key(mutation("IntIntMaxHeap.kt")), templates.key(mutation("LongIntMinHeap.kt")))
    }

    // ---- The review table ----

    private fun table(vararg rows: String) =
        "# x\n\n${ReviewTable.HEADING}\n\ntext\n\n| テンプレート | メソッド | ミューテータ | 説明 | 上限 | 理由 |\n|---|---|---|---|---|---|\n" +
            rows.joinToString("\n") + "\n\nafter\n"

    @Test
    fun readsOneAllowancePerListedMethod() {
        val rows = ReviewTable.parse(
            table("| `KTypeList.kt` | `add`、`clear` | `ConditionalsBoundaryMutator` | `changed conditional boundary` | 2 | why |"),
        )
        assertEquals(listOf(Allowance(key("KTypeList.kt", "add"), 2), Allowance(key("KTypeList.kt", "clear"), 2)), rows)
    }

    @Test
    fun rejectsAMalformedTable() {
        val good = "| `KTypeList.kt` | `add` | `ConditionalsBoundaryMutator` | `changed conditional boundary` | 1 | why |"
        assertFailsWith<IllegalArgumentException> { ReviewTable.parse("# no heading\n") }
        assertFailsWith<IllegalArgumentException> { ReviewTable.parse("${ReviewTable.HEADING}\n\nno table\n") }
        assertFailsWith<IllegalArgumentException> { ReviewTable.parse("${ReviewTable.HEADING}\n\n| テンプレート | メソッド |\n|---|---|\n") }
        assertFailsWith<IllegalArgumentException> { ReviewTable.parse(table(good, good)) }
        assertFailsWith<IllegalArgumentException> { ReviewTable.parse(table(good.replace("| 1 |", "| one |"))) }
        assertFailsWith<IllegalArgumentException> { ReviewTable.parse(table(good.replace("| 1 |", "| 0 |"))) }
        assertFailsWith<IllegalArgumentException> { ReviewTable.parse(table(good.replace("`add`", "add"))) }
        assertFailsWith<IllegalArgumentException> { ReviewTable.parse(table(good.replace("`KTypeList.kt`", "`A.kt` `B.kt`"))) }
    }

    @Test
    fun readsTheCommittedReviewTable() {
        val allowances = ReviewTable.parse(File("../docs/mutation-review.md").readText())
        assertTrue(allowances.isNotEmpty())
    }

    // ---- The verdict ----

    private val heapAllowance = listOf(Allowance(key("KTypeVTypeMinHeap.kt"), 1))

    @Test
    fun failsOnAnUnlistedSurvivor() {
        val result = MutationGate.check(listOf(mutation("IntList.kt", line = 70)), templates, heapAllowance)
        assertEquals(1, result.failures.size)
        assertTrue("not in the review table" in result.failures.single() && "70 SURVIVED" in result.failures.single())
    }

    @Test
    fun failsWhenOneClassHasMoreThanTheLimit() {
        val result = MutationGate.check(listOf(mutation("IntIntMaxHeap.kt"), mutation("IntIntMaxHeap.kt", line = 2)), templates, heapAllowance)
        assertEquals(1, result.failures.size)
        assertTrue("allows 1" in result.failures.single())
    }

    @Test
    fun countsTheLimitPerGeneratedClass() {
        val result = MutationGate.check(listOf(mutation("IntIntMaxHeap.kt"), mutation("LongIntMinHeap.kt")), templates, heapAllowance)
        assertEquals(emptyList(), result.failures)
        assertEquals(emptyList(), result.warnings)
    }

    @Test
    fun allowsAListedMutationToBeDetected() {
        val mutations = listOf(mutation("IntIntMaxHeap.kt", detected = true), mutation("IntList.kt", detected = true))
        val result = MutationGate.check(mutations, templates, heapAllowance)
        assertEquals(emptyList(), result.failures)
        assertEquals(1, result.warnings.size)
        assertEquals(2, result.detected)
        assertEquals(2, result.total)
    }

    @Test
    fun readsDetectedFromTheReport() {
        val file = Files.createTempFile("mutations", ".xml").toFile()
        try {
            file.writeText(
                """<?xml version="1.0" encoding="UTF-8"?>
                <mutations>
                <mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'><sourceFile>IntList.kt</sourceFile><mutatedClass>ac_primitive_collections.IntList</mutatedClass><mutatedMethod>add</mutatedMethod><methodDescription>(I)V</methodDescription><lineNumber>73</lineNumber><mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator><indexes><index>1</index></indexes><blocks><block>0</block></blocks><killingTest/><description>Replaced integer addition with subtraction</description></mutation>
                <mutation detected='true' status='MEMORY_ERROR' numberOfTestsRun='1'><sourceFile>IntList.kt</sourceFile><mutatedClass>ac_primitive_collections.IntList</mutatedClass><mutatedMethod>grow</mutatedMethod><methodDescription>(J)V</methodDescription><lineNumber>80</lineNumber><mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator><indexes><index>1</index></indexes><blocks><block>0</block></blocks><killingTest/><description>x</description></mutation>
                </mutations>
                """.trimIndent(),
            )
            assertEquals(
                listOf(
                    Mutation("IntList.kt", "add", "MathMutator", "Replaced integer addition with subtraction", 73, "NO_COVERAGE", false),
                    Mutation("IntList.kt", "grow", "MathMutator", "x", 80, "MEMORY_ERROR", true),
                ),
                MutationReport.parse(file),
            )
        } finally {
            file.delete()
        }
    }
}
