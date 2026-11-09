// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeListSort.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/**
 * Sorts stably in O(n log n) with a merge sort.
 *
 * A comparator that is not a total order gives an unspecified order, but still a permutation of the input.
 */
fun LongList.sortWith(comparator: LongComparator) = sortWith(comparator, 0, length)

fun LongList.sortWith(comparator: LongComparator, from: Int, to: Int) {
    checkRange(from, to, length)
    val expected = modCount
    mergeSortLong(elements, from, to, comparator)
    if (modCount != expected) throwConcurrentModification()
}

/** Sorts stably by an Int key without a comparator. */
inline fun LongList.sortBy(selector: (Long) -> Int) {
    val keys = LongArray(length)
    forEachIndexed { index, value -> keys[index] = packKeyAndIndex(selector(value), index) }
    applySortedIndices(sortedIndices(keys))
}

/** Sorts stably by a Long key without a comparator. */
inline fun LongList.sortByLong(selector: (Long) -> Long) {
    val keys = LongArray(length)
    forEachIndexed { index, value -> keys[index] = selector(value) }
    applySortedIndices(sortedIndicesByKey(keys))
}

/** Sorts stably by a Double key without a comparator, in the order of `Double.compareTo`. */
inline fun LongList.sortByDouble(selector: (Long) -> Double) = sortByLong { totalOrderKey(selector(it)) }

/** Returns the indices that sort the list, keeping equal elements in their original order. */
fun LongList.argsort(): IntArray = this.argsortByLong { it }

/** Returns the indices that sort the list by an Int key, keeping ties in their original order. */
inline fun LongList.argsortBy(selector: (Long) -> Int): IntArray {
    val keys = LongArray(length)
    forEachIndexed { index, value -> keys[index] = packKeyAndIndex(selector(value), index) }
    return sortedIndices(keys)
}

/** Returns the indices that sort the list by a Long key, keeping ties in their original order. */
inline fun LongList.argsortByLong(selector: (Long) -> Long): IntArray {
    val keys = LongArray(length)
    forEachIndexed { index, value -> keys[index] = selector(value) }
    return sortedIndicesByKey(keys)
}

/** Returns the indices that sort the list by a Double key in the order of `Double.compareTo`, keeping ties in their original order. */
inline fun LongList.argsortByDouble(selector: (Long) -> Double): IntArray = argsortByLong { totalOrderKey(selector(it)) }

@PublishedApi
internal fun LongList.applySortedIndices(indices: IntArray) {
    val source = toLongArray()
    var i = 0
    while (i < indices.size) {
        elements[i] = source[indices[i]]
        i++
    }
}
