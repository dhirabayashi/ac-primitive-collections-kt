// SPDX-License-Identifier: CC0-1.0
// Generated from test-templates/KTypeListMapToVTypeTest.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CharListMapToLongTest {
    private fun v(i: Int): Char = i.toChar()

    @Test
    fun convertsEveryElementInOrder() {
        val mapped = charListOf(v(3), v(1), v(2)).mapToLong { it.code.toLong() }
        assertEquals(listOf(3.toLong(), 1.toLong(), 2.toLong()), mapped.toList())
        assertEquals(3, mapped.capacity)
    }

    @Test
    fun convertsAnEmptyList() {
        assertTrue(CharList().mapToLong { it.code.toLong() }.isEmpty())
    }

    /** A plain try: inlining through assertFailsWith as well would hide this branch from the coverage tool. */
    @Test
    fun failsIfTheTransformChangesTheList() {
        val list = charListOf(v(1), v(2))
        val failed = try {
            list.mapToLong {
                list.clear()
                it.code.toLong()
            }
            false
        } catch (_: ConcurrentModificationException) {
            true
        }
        assertTrue(failed)
        assertFalse(list.isNotEmpty())
    }
}
