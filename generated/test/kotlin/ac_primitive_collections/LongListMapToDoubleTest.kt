// SPDX-License-Identifier: CC0-1.0
// Generated from test-templates/KTypeListMapToVTypeTest.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LongListMapToDoubleTest {
    private fun v(i: Int): Long = i.toLong()

    @Test
    fun convertsEveryElementInOrder() {
        val mapped = longListOf(v(3), v(1), v(2)).mapToDouble { it.toInt().toDouble() }
        assertEquals(listOf(3.toDouble(), 1.toDouble(), 2.toDouble()), mapped.toList())
        assertEquals(3, mapped.capacity)
    }

    @Test
    fun convertsAnEmptyList() {
        assertTrue(LongList().mapToDouble { it.toInt().toDouble() }.isEmpty())
    }

    /** A plain try: inlining through assertFailsWith as well would hide this branch from the coverage tool. */
    @Test
    fun failsIfTheTransformChangesTheList() {
        val list = longListOf(v(1), v(2))
        val failed = try {
            list.mapToDouble {
                list.clear()
                it.toInt().toDouble()
            }
            false
        } catch (_: ConcurrentModificationException) {
            true
        }
        assertTrue(failed)
        assertFalse(list.isNotEmpty())
    }
}
