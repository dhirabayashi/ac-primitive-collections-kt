// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

import kotlin.test.Test
import kotlin.test.assertEquals

class KeySortTest {
    private fun assertSortsStably(keys: LongArray) {
        val input = keys.copyOf()
        assertEquals(keys.indices.sortedBy { keys[it] }, sortedIndicesByKey(keys).toList())
        assertEquals(input.toList(), keys.toList(), "The keys must stay as they were")
    }

    @Test
    fun sortedIndicesByKeyIsStableForAnyKeys() {
        assertSortsStably(longArrayOf())
        assertSortsStably(longArrayOf(5))
        assertSortsStably(LongArray(10) { 7 })
        assertSortsStably(longArrayOf(Long.MAX_VALUE, Long.MIN_VALUE, -1, 0, 1, -1, Long.MIN_VALUE))
        // Each differs in one byte only, so that every pass has to do its share.
        assertSortsStably(LongArray(64) { 1L shl (63 - it) })
        val random = kotlin.random.Random(1)
        assertSortsStably(LongArray(5000) { random.nextLong() })
        assertSortsStably(LongArray(5000) { random.nextLong(4) shl 56 })
    }

    @Test
    fun totalOrderKeyMatchesCompareTo() {
        val ordered = listOf(
            Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, -1.5, -Double.MIN_VALUE, -0.0, 0.0, Double.MIN_VALUE, 1.5,
            Double.MAX_VALUE, Double.POSITIVE_INFINITY, Double.NaN,
        )
        assertEquals(ordered.sorted(), ordered, "The list above must be in the order of compareTo")
        for (i in 1 until ordered.size) {
            assertEquals(true, totalOrderKey(ordered[i - 1]) < totalOrderKey(ordered[i]), "${ordered[i - 1]} and ${ordered[i]}")
        }
        // Every NaN is equal under compareTo, whatever its bits.
        assertEquals(totalOrderKey(Double.NaN), totalOrderKey(Double.fromBits(-1L)))
        val random = kotlin.random.Random(2)
        repeat(10000) {
            val a = Double.fromBits(random.nextLong())
            val b = Double.fromBits(random.nextLong())
            assertEquals(a.compareTo(b), totalOrderKey(a).compareTo(totalOrderKey(b)), "$a and $b")
        }
    }
}
