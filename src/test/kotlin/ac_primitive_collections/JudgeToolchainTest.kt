// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

import kotlin.test.Test
import kotlin.test.assertEquals

class JudgeToolchainTest {
    @Test
    fun kotlinStdlibMatchesJudge() {
        assertEquals(System.getProperty("judge.kotlin"), KotlinVersion.CURRENT.toString())
    }

    @Test
    fun jdkFeatureVersionMatchesJudge() {
        assertEquals(System.getProperty("judge.jdk").toInt(), Runtime.version().feature())
    }
}
