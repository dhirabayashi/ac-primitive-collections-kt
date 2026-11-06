// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OperationRunnerTest {
    @Test
    fun minimizeFindsTheCulprits() {
        val shortest = OperationRunner.minimize((0 until 1000).toList()) { 123 in it && 777 in it }
        assertEquals(listOf(123, 777), shortest)
    }

    @Test
    fun runReportsSeedAndShortenedOps() {
        val error = assertFailsWith<AssertionError> {
            OperationRunner.run("demo", 42L, (0 until 100).toList()) { ops -> check(55 !in ops) }
        }
        assertTrue("-Dseed=42" in error.message!!)
        assertTrue("from 100 to 1 ops" in error.message!!)
    }

    @Test
    fun runIsSilentWhenTheCheckPasses() {
        OperationRunner.run("demo", 1L, listOf(1, 2, 3)) { }
    }
}
