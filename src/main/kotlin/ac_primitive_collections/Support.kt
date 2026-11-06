// SPDX-License-Identifier: CC0-1.0
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/** Marks APIs that box on purpose, such as conversions to `List<Int>`. The boxing check skips them. */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
internal annotation class BoxingApi

/** Some VMs reserve header words, so larger arrays can fail. */
internal const val MAX_ARRAY_SIZE: Int = Int.MAX_VALUE - 8

internal const val DEFAULT_CAPACITY: Int = 8

/**
 * Returns the next array size that holds [required] elements.
 *
 * The growth factor is provisional until it is measured on the judge. See docs/PLAN.md, section 7.6.
 */
internal fun nextCapacity(current: Int, required: Long): Int {
    if (required > MAX_ARRAY_SIZE) throw IllegalStateException("Required capacity is too large: $required")
    val grown = current.toLong() + (current shr 1)
    return maxOf(required, minOf(grown, MAX_ARRAY_SIZE.toLong()), DEFAULT_CAPACITY.toLong()).toInt()
}

internal fun checkInitialCapacity(capacity: Int): Int {
    if (capacity < 0 || capacity > MAX_ARRAY_SIZE) throw IllegalArgumentException("Invalid capacity: $capacity")
    return capacity
}

@PublishedApi
internal fun checkNonNegativeSize(size: Int): Int {
    if (size < 0) throw IllegalArgumentException("Negative size: $size")
    return size
}

internal fun checkRange(from: Int, to: Int, size: Int) {
    if (from < 0 || to > size || from > to) {
        throw IndexOutOfBoundsException("Invalid range [$from, $to) for size $size")
    }
}

@PublishedApi
internal fun throwIndexOutOfBounds(index: Int, size: Int): Nothing =
    throw IndexOutOfBoundsException("Index $index is out of bounds for size $size")

@PublishedApi
internal fun throwEmpty(): Nothing = throw NoSuchElementException("The collection is empty")

@PublishedApi
internal fun throwConcurrentModification(): Nothing =
    throw ConcurrentModificationException("The collection changed during the operation")

/** The key is signed in the high bits and the index non-negative in the low bits, so Long order is (key, index). */
@PublishedApi
internal fun packKeyAndIndex(key: Int, index: Int): Long = (key.toLong() shl 32) or index.toLong()

@PublishedApi
internal fun sortedIndices(keys: LongArray): IntArray {
    java.util.Arrays.sort(keys)
    val indices = IntArray(keys.size)
    var i = 0
    while (i < keys.size) {
        indices[i] = keys[i].toInt()
        i++
    }
    return indices
}
