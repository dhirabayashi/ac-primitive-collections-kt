// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeListSort.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/** A comparator that does not box. Calling it is a virtual call, so only comparator APIs use it. */
fun interface DoubleComparator {
    fun compare(a: Double, b: Double): Int
}

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

private const val DOUBLE_INSERTION_RUN = 8

/** Iterative, so the stack stays fixed. Indices depend only on the range, so a bad comparator cannot leave it. */
internal fun mergeSortDouble(a: DoubleArray, from: Int, to: Int, comparator: DoubleComparator) {
    val n = to - from
    if (n < 2) return
    var start = from
    while (to - start > DOUBLE_INSERTION_RUN) {
        insertionSortDouble(a, start, start + DOUBLE_INSERTION_RUN, comparator)
        start += DOUBLE_INSERTION_RUN
    }
    insertionSortDouble(a, start, to, comparator)
    if (n <= DOUBLE_INSERTION_RUN) return

    // The longest left run is the largest doubling of the run length that is still below n.
    var longestLeftRun = DOUBLE_INSERTION_RUN
    while (n - longestLeftRun > longestLeftRun) longestLeftRun *= 2
    val buffer = DoubleArray(longestLeftRun)
    var width = DOUBLE_INSERTION_RUN
    while (true) {
        var lo = from
        while (to - lo > width) {
            val mid = lo + width
            val hi = if (to - mid > width) mid + width else to
            mergeDouble(a, lo, mid, hi, buffer, comparator)
            lo = hi
        }
        // A width of at least n / 2 just merged the last two runs. Doubling it could also overflow.
        if (n - width <= width) return
        width *= 2
    }
}

private fun insertionSortDouble(a: DoubleArray, from: Int, to: Int, comparator: DoubleComparator) {
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

private fun mergeDouble(a: DoubleArray, lo: Int, mid: Int, hi: Int, buffer: DoubleArray, comparator: DoubleComparator) {
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
