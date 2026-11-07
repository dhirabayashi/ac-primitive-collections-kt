// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeListSort.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/** A comparator that does not box. Calling it is a virtual call, so only comparator APIs use it. */
fun interface IntComparator {
    fun compare(a: Int, b: Int): Int
}

/**
 * Sorts stably in O(n log n) with a merge sort.
 *
 * A comparator that is not a total order gives an unspecified order, but still a permutation of the input.
 */
fun IntList.sortWith(comparator: IntComparator) = sortWith(comparator, 0, length)

fun IntList.sortWith(comparator: IntComparator, from: Int, to: Int) {
    checkRange(from, to, length)
    val expected = modCount
    mergeSortInt(elements, from, to, comparator)
    if (modCount != expected) throwConcurrentModification()
}

/** Sorts stably by an Int key without a comparator. */
inline fun IntList.sortBy(selector: (Int) -> Int) {
    val keys = LongArray(length)
    forEachIndexed { index, value -> keys[index] = packKeyAndIndex(selector(value), index) }
    applySortedIndices(sortedIndices(keys))
}

/** Sorts stably by a Long key without a comparator. */
inline fun IntList.sortByLong(selector: (Int) -> Long) {
    val keys = LongArray(length)
    forEachIndexed { index, value -> keys[index] = selector(value) }
    applySortedIndices(sortedIndicesByKey(keys))
}

/** Sorts stably by a Double key without a comparator, in the order of `Double.compareTo`. */
inline fun IntList.sortByDouble(selector: (Int) -> Double) {
    val keys = LongArray(length)
    forEachIndexed { index, value -> keys[index] = totalOrderKey(selector(value)) }
    applySortedIndices(sortedIndicesByKey(keys))
}

/** Returns the indices that sort the list, keeping equal elements in their original order. */
fun IntList.argsort(): IntArray = this.argsortBy { it }

/** Returns the indices that sort the list by an Int key, keeping ties in their original order. */
inline fun IntList.argsortBy(selector: (Int) -> Int): IntArray {
    val keys = LongArray(length)
    forEachIndexed { index, value -> keys[index] = packKeyAndIndex(selector(value), index) }
    return sortedIndices(keys)
}

/** Returns the indices that sort the list by a Long key, keeping ties in their original order. */
inline fun IntList.argsortByLong(selector: (Int) -> Long): IntArray {
    val keys = LongArray(length)
    forEachIndexed { index, value -> keys[index] = selector(value) }
    return sortedIndicesByKey(keys)
}

/** Returns the indices that sort the list by a Double key in the order of `Double.compareTo`, keeping ties in their original order. */
inline fun IntList.argsortByDouble(selector: (Int) -> Double): IntArray {
    val keys = LongArray(length)
    forEachIndexed { index, value -> keys[index] = totalOrderKey(selector(value)) }
    return sortedIndicesByKey(keys)
}

@PublishedApi
internal fun IntList.applySortedIndices(indices: IntArray) {
    val source = toIntArray()
    var i = 0
    while (i < indices.size) {
        elements[i] = source[indices[i]]
        i++
    }
}

private const val INT_INSERTION_RUN = 8

/** Iterative, so the stack stays fixed. Indices depend only on the range, so a bad comparator cannot leave it. */
internal fun mergeSortInt(a: IntArray, from: Int, to: Int, comparator: IntComparator) {
    val n = to - from
    if (n < 2) return
    var start = from
    while (to - start > INT_INSERTION_RUN) {
        insertionSortInt(a, start, start + INT_INSERTION_RUN, comparator)
        start += INT_INSERTION_RUN
    }
    insertionSortInt(a, start, to, comparator)
    if (n <= INT_INSERTION_RUN) return

    // The longest left run is the largest doubling of the run length that is still below n.
    var longestLeftRun = INT_INSERTION_RUN
    while (n - longestLeftRun > longestLeftRun) longestLeftRun *= 2
    val buffer = IntArray(longestLeftRun)
    var width = INT_INSERTION_RUN
    while (true) {
        var lo = from
        while (to - lo > width) {
            val mid = lo + width
            val hi = if (to - mid > width) mid + width else to
            mergeInt(a, lo, mid, hi, buffer, comparator)
            lo = hi
        }
        // A width of at least n / 2 just merged the last two runs. Doubling it could also overflow.
        if (n - width <= width) return
        width *= 2
    }
}

private fun insertionSortInt(a: IntArray, from: Int, to: Int, comparator: IntComparator) {
    var i = from + 1
    while (i < to) {
        val value = a[i]
        var j = i - 1
        while (j >= from && comparator.compare(a[j], value) > 0) {
            a[j + 1] = a[j]
            j--
        }
        a[j + 1] = value
        i++
    }
}

private fun mergeInt(a: IntArray, lo: Int, mid: Int, hi: Int, buffer: IntArray, comparator: IntComparator) {
    if (comparator.compare(a[mid - 1], a[mid]) <= 0) return
    val leftSize = mid - lo
    System.arraycopy(a, lo, buffer, 0, leftSize)
    var left = 0
    var right = mid
    var write = lo
    while (left < leftSize && right < hi) {
        // Taking the right element only when strictly smaller keeps the sort stable.
        if (comparator.compare(a[right], buffer[left]) < 0) {
            a[write] = a[right]
            right++
        } else {
            a[write] = buffer[left]
            left++
        }
        write++
    }
    System.arraycopy(buffer, left, a, write, leftSize - left)
}
