// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SupportTest {
    @Test
    fun nextCapacityGrowsAndClamps() {
        assertEquals(8, nextCapacity(0, 1))
        assertEquals(8, nextCapacity(4, 5))
        assertEquals(15, nextCapacity(10, 11))
        assertEquals(100, nextCapacity(10, 100))
        assertEquals(MAX_ARRAY_SIZE, nextCapacity(MAX_ARRAY_SIZE - 1, MAX_ARRAY_SIZE.toLong()))
        // current * 1.5 exceeds Int.MAX_VALUE here, and must clamp instead of wrapping.
        assertEquals(MAX_ARRAY_SIZE, nextCapacity(Int.MAX_VALUE / 3 * 2, Int.MAX_VALUE / 3 * 2 + 1L))
        assertEquals(MAX_ARRAY_SIZE, nextCapacity(0, MAX_ARRAY_SIZE.toLong()))
    }

    @Test
    fun nextCapacityRejectsImpossibleSizes() {
        assertFailsWith<IllegalStateException> { nextCapacity(0, MAX_ARRAY_SIZE + 1L) }
        assertFailsWith<IllegalStateException> { nextCapacity(MAX_ARRAY_SIZE, Long.MAX_VALUE) }
    }

    @Test
    fun checkInitialCapacityRejectsBadValues() {
        assertEquals(0, checkInitialCapacity(0))
        assertEquals(MAX_ARRAY_SIZE, checkInitialCapacity(MAX_ARRAY_SIZE))
        assertFailsWith<IllegalArgumentException> { checkInitialCapacity(-1) }
        assertFailsWith<IllegalArgumentException> { checkInitialCapacity(MAX_ARRAY_SIZE + 1) }
    }

    @Test
    fun checkNonNegativeSizeRejectsNegatives() {
        assertEquals(0, checkNonNegativeSize(0))
        assertFailsWith<IllegalArgumentException> { checkNonNegativeSize(-1) }
    }

    @Test
    fun checkRangeRejectsEveryBadShape() {
        checkRange(0, 0, 0)
        checkRange(1, 3, 3)
        assertFailsWith<IndexOutOfBoundsException> { checkRange(-1, 0, 3) }
        assertFailsWith<IndexOutOfBoundsException> { checkRange(0, 4, 3) }
        assertFailsWith<IndexOutOfBoundsException> { checkRange(2, 1, 3) }
    }

    @Test
    fun packKeepsKeyThenIndexOrder() {
        val keys = intArrayOf(Int.MIN_VALUE, -1, 0, 1, Int.MAX_VALUE)
        val indices = intArrayOf(0, 1, Int.MAX_VALUE)
        val packed = keys.flatMap { k -> indices.map { i -> packKeyAndIndex(k, i) } }
        assertEquals(packed.sorted(), packed)
        assertEquals(Int.MAX_VALUE, packKeyAndIndex(-1, Int.MAX_VALUE).toInt())
    }
}
