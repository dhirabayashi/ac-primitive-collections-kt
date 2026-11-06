// SPDX-License-Identifier: CC0-1.0
package codegen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `checkGenerated` compares `generated/` with this same generator, and the compiler only sees classes
 * that something uses, so neither notices a missing combination. These lists are written by hand from
 * docs/PLAN.md section 5.1 for that reason. What each file contains is up to its own generated tests.
 */
class CodegenTest {
    private val pairHeaps = listOf(
        "IntIntMaxHeap", "IntIntMinHeap", "IntLongMaxHeap", "IntLongMinHeap", "IntObjMaxHeap", "IntObjMinHeap",
        "LongIntMaxHeap", "LongIntMinHeap", "LongLongMaxHeap", "LongLongMinHeap", "LongObjMaxHeap", "LongObjMinHeap",
        "DoubleIntMaxHeap", "DoubleIntMinHeap", "DoubleLongMaxHeap", "DoubleLongMinHeap", "DoubleObjMaxHeap", "DoubleObjMinHeap",
    )

    private fun names(generated: Map<String, String>) =
        generated.keys.map { it.removePrefix("ac_primitive_collections/").removeSuffix(".kt") }.toSet()

    private val main = Codegen.generate(File("../templates"))
    private val test = Codegen.generate(File("../test-templates"))

    @Test
    fun generatesExactlyThePlannedLibraryFiles() {
        assertEquals((pairHeaps + listOf("IntList", "IntListConversions", "IntListSort", "IntSubList")).toSet(), names(main))
    }

    @Test
    fun generatesExactlyThePlannedTestFiles() {
        assertEquals(
            (pairHeaps.map { it + "Test" } + listOf("IntListDifferentialTest", "IntListTest", "IntSubListTest")).toSet(),
            names(test),
        )
    }
}
