// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KTypeListMapToVTypeTest {
    private fun v(i: Int): KType = i.toKType()

    @Test
    fun convertsEveryElementInOrder() {
        val mapped = ktypeListOf(v(3), v(1), v(2)).mapToVType { K_TO_INT(it).toVType() }
        assertEquals(listOf(3.toVType(), 1.toVType(), 2.toVType()), mapped.toList())
        assertEquals(3, mapped.capacity)
    }

    @Test
    fun convertsAnEmptyList() {
        assertTrue(KTypeList().mapToVType { K_TO_INT(it).toVType() }.isEmpty())
    }

    /** A plain try: inlining through assertFailsWith as well would hide this branch from the coverage tool. */
    @Test
    fun failsIfTheTransformChangesTheList() {
        val list = ktypeListOf(v(1), v(2))
        val failed = try {
            list.mapToVType {
                list.clear()
                K_TO_INT(it).toVType()
            }
            false
        } catch (_: ConcurrentModificationException) {
            true
        }
        assertTrue(failed)
        assertFalse(list.isNotEmpty())
    }
}
