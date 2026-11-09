// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

/**
 * Sorts stably in O(n log n) with a merge sort.
 *
 * A comparator that is not a total order gives an unspecified order, but still a permutation of the input.
 */
fun KTypeList.sortWith(comparator: KTypeComparator) = sortWith(comparator, 0, length)

fun KTypeList.sortWith(comparator: KTypeComparator, from: Int, to: Int) {
    checkRange(from, to, length)
    val expected = modCount
    mergeSortKType(elements, from, to, comparator)
    if (modCount != expected) throwConcurrentModification()
}

/** Sorts stably by an Int key without a comparator. */
inline fun KTypeList.sortBy(selector: (KType) -> Int) {
    val keys = LongArray(length)
    forEachIndexed { index, value -> keys[index] = packKeyAndIndex(selector(value), index) }
    applySortedIndices(sortedIndices(keys))
}

/** Sorts stably by a Long key without a comparator. */
inline fun KTypeList.sortByLong(selector: (KType) -> Long) {
    val keys = LongArray(length)
    forEachIndexed { index, value -> keys[index] = selector(value) }
    applySortedIndices(sortedIndicesByKey(keys))
}

/** Sorts stably by a Double key without a comparator, in the order of `Double.compareTo`. */
inline fun KTypeList.sortByDouble(selector: (KType) -> Double) = sortByLong { totalOrderKey(selector(it)) }

/** Returns the indices that sort the list, keeping equal elements in their original order. */
fun KTypeList.argsort(): IntArray = K_ARGSORT(this)

/** Returns the indices that sort the list by an Int key, keeping ties in their original order. */
inline fun KTypeList.argsortBy(selector: (KType) -> Int): IntArray {
    val keys = LongArray(length)
    forEachIndexed { index, value -> keys[index] = packKeyAndIndex(selector(value), index) }
    return sortedIndices(keys)
}

/** Returns the indices that sort the list by a Long key, keeping ties in their original order. */
inline fun KTypeList.argsortByLong(selector: (KType) -> Long): IntArray {
    val keys = LongArray(length)
    forEachIndexed { index, value -> keys[index] = selector(value) }
    return sortedIndicesByKey(keys)
}

/** Returns the indices that sort the list by a Double key in the order of `Double.compareTo`, keeping ties in their original order. */
inline fun KTypeList.argsortByDouble(selector: (KType) -> Double): IntArray = argsortByLong { totalOrderKey(selector(it)) }

@PublishedApi
internal fun KTypeList.applySortedIndices(indices: IntArray) {
    val source = toKTypeArray()
    var i = 0
    while (i < indices.size) {
        elements[i] = source[indices[i]]
        i++
    }
}
