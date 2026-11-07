// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeListSort.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/** A comparator that does not box. Calling it is a virtual call, so only comparator APIs use it. */
fun interface LongComparator {
    fun compare(a: Long, b: Long): Int
}

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

private const val LONG_INSERTION_RUN = 8

/** Iterative, so the stack stays fixed. Indices depend only on the range, so a bad comparator cannot leave it. */
internal fun mergeSortLong(a: LongArray, from: Int, to: Int, comparator: LongComparator) {
    val n = to - from
    if (n < 2) return
    var start = from
    while (to - start > LONG_INSERTION_RUN) {
        insertionSortLong(a, start, start + LONG_INSERTION_RUN, comparator)
        start += LONG_INSERTION_RUN
    }
    insertionSortLong(a, start, to, comparator)
    if (n <= LONG_INSERTION_RUN) return

    // The longest left run is the largest doubling of the run length that is still below n.
    var longestLeftRun = LONG_INSERTION_RUN
    while (n - longestLeftRun > longestLeftRun) longestLeftRun *= 2
    val buffer = LongArray(longestLeftRun)
    var width = LONG_INSERTION_RUN
    while (true) {
        var lo = from
        while (to - lo > width) {
            val mid = lo + width
            val hi = if (to - mid > width) mid + width else to
            mergeLong(a, lo, mid, hi, buffer, comparator)
            lo = hi
        }
        // A width of at least n / 2 just merged the last two runs. Doubling it could also overflow.
        if (n - width <= width) return
        width *= 2
    }
}

private fun insertionSortLong(a: LongArray, from: Int, to: Int, comparator: LongComparator) {
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

private fun mergeLong(a: LongArray, lo: Int, mid: Int, hi: Int, buffer: LongArray, comparator: LongComparator) {
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
