// SPDX-License-Identifier: CC0-1.0
// Generated from test-templates/KTypeListMapToVTypeTest.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LongListMapToCharTest {
    private fun v(i: Int): Long = i.toLong()

    @Test
    fun convertsEveryElementInOrder() {
        val mapped = longListOf(v(3), v(1), v(2)).mapToChar { it.toInt().toChar() }
        assertEquals(listOf(3.toChar(), 1.toChar(), 2.toChar()), mapped.toList())
        assertEquals(3, mapped.capacity)
    }

    @Test
    fun convertsAnEmptyList() {
        assertTrue(LongList().mapToChar { it.toInt().toChar() }.isEmpty())
    }

    /** A plain try: inlining through assertFailsWith as well would hide this branch from the coverage tool. */
    @Test
    fun failsIfTheTransformChangesTheList() {
        val list = longListOf(v(1), v(2))
        val failed = try {
            list.mapToChar {
                list.clear()
                it.toInt().toChar()
            }
            false
        } catch (_: ConcurrentModificationException) {
            true
        }
        assertTrue(failed)
        assertFalse(list.isNotEmpty())
    }
}
