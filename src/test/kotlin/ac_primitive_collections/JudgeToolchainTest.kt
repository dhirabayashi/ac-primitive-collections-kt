// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

import kotlin.test.Test
import kotlin.test.assertEquals

class JudgeToolchainTest {
    @Test
    fun kotlinRuntimeMatchesJudge() {
        assertEquals("2.2.10", KotlinVersion.CURRENT.toString())
    }

    @Test
    fun jdkFeatureVersionMatchesJudge() {
        assertEquals(23, Runtime.version().feature())
    }
}
