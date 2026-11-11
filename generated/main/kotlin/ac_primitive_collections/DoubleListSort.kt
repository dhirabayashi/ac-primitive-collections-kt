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
fun DoubleList.sortWith(comparator: DoubleComparator) = sortWith(comparator, 0, length)

fun DoubleList.sortWith(comparator: DoubleComparator, from: Int, to: Int) {
    checkRange(from, to, length)
    val expected = modCount
    mergeSortDouble(elements, from, to, comparator)
    if (modCount != expected) throwConcurrentModification()
}

/** Sorts stably by an Int key without a comparator. */
inline fun DoubleList.sortBy(selector: (Double) -> Int) {
    val keys = LongArray(length)
    forEachIndexed { index, value -> keys[index] = packKeyAndIndex(selector(value), index) }
    applySortedIndices(sortedIndices(keys))
}

/** Sorts stably by a Long key without a comparator. */
inline fun DoubleList.sortByLong(selector: (Double) -> Long) {
    val keys = LongArray(length)
    forEachIndexed { index, value -> keys[index] = selector(value) }
    applySortedIndices(sortedIndicesByKey(keys))
}

/** Sorts stably by a Double key without a comparator, in the order of `Double.compareTo`. */
inline fun DoubleList.sortByDouble(selector: (Double) -> Double) = sortByLong { totalOrderKey(selector(it)) }

/** Returns the indices that sort the list, keeping equal elements in their original order. */
fun DoubleList.argsort(): IntArray = this.argsortByDouble { it }

/** Returns the indices that sort the list by an Int key, keeping ties in their original order. */
inline fun DoubleList.argsortBy(selector: (Double) -> Int): IntArray {
    val keys = LongArray(length)
    forEachIndexed { index, value -> keys[index] = packKeyAndIndex(selector(value), index) }
    return sortedIndices(keys)
}

/** Returns the indices that sort the list by a Long key, keeping ties in their original order. */
inline fun DoubleList.argsortByLong(selector: (Double) -> Long): IntArray {
    val keys = LongArray(length)
    forEachIndexed { index, value -> keys[index] = selector(value) }
    return sortedIndicesByKey(keys)
}

/** Returns the indices that sort the list by a Double key in the order of `Double.compareTo`, keeping ties in their original order. */
inline fun DoubleList.argsortByDouble(selector: (Double) -> Double): IntArray = argsortByLong { totalOrderKey(selector(it)) }

@PublishedApi
internal fun DoubleList.applySortedIndices(indices: IntArray) {
    val source = toDoubleArray()
    var i = 0
    while (i < indices.size) {
        elements[i] = source[indices[i]]
        i++
    }
}
