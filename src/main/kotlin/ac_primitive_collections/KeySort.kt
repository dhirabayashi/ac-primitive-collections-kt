// SPDX-License-Identifier: CC0-1.0
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/** Maps a Double to a Long that sorts the same way as `Double.compareTo`, including -0.0 and NaN. */
@PublishedApi
internal fun totalOrderKey(key: Double): Long {
    val bits = key.toBits()
    return bits xor ((bits shr 63) ushr 1)
}

private const val RADIX_BITS = 8
private const val RADIX = 1 shl RADIX_BITS

/**
 * Returns the indices that sort [keys] stably, with an LSD radix sort. A Long key leaves no room for
 * the index that [packKeyAndIndex] adds, and the indices cannot go through `Arrays.sort`.
 */
@PublishedApi
internal fun sortedIndicesByKey(keys: LongArray): IntArray {
    val n = keys.size
    var order = IntArray(n)
    var i = 0
    while (i < n) {
        order[i] = i
        i++
    }
    var buffer = IntArray(n)
    val counts = IntArray(RADIX)
    var shift = 0
    while (shift < Long.SIZE_BITS) {
        if (radixPass(keys, order, buffer, counts, shift)) {
            val swapped = order
            order = buffer
            buffer = swapped
        }
        shift += RADIX_BITS
    }
    return order
}

/** Moves [order] into [target] by one byte of the keys. Returns false, having moved nothing, if all keys share that byte. */
private fun radixPass(keys: LongArray, order: IntArray, target: IntArray, counts: IntArray, shift: Int): Boolean {
    counts.fill(0)
    var i = 0
    while (i < keys.size) {
        counts[radixDigit(keys[i], shift)]++
        i++
    }
    var total = 0
    var digit = 0
    while (digit < RADIX) {
        val count = counts[digit]
        if (count == keys.size) return false
        counts[digit] = total
        total += count
        digit++
    }
    i = 0
    while (i < order.size) {
        val index = order[i]
        target[counts[radixDigit(keys[index], shift)]++] = index
        i++
    }
    return true
}

/** Flipping the sign bit makes the unsigned digits sort the same way as the signed keys. */
private fun radixDigit(key: Long, shift: Int): Int = ((key xor Long.MIN_VALUE) ushr shift).toInt() and (RADIX - 1)
