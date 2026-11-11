// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

/**
 * A comparator that does not box. Calling it is a virtual call, so only comparator APIs use it.
 *
 * Lives apart from the List, with its sort, so that sorting an array with it does not bring the List into a submission.
 */
fun interface KTypeComparator {
    fun compare(a: KType, b: KType): Int
}

private const val KTYPE_INSERTION_RUN = 8

/** Iterative, so the stack stays fixed. Indices depend only on the range, so a bad comparator cannot leave it. */
internal fun mergeSortKType(a: KTypeArray, from: Int, to: Int, comparator: KTypeComparator) {
    val n = to - from
    if (n < 2) return
    var start = from
    while (to - start > KTYPE_INSERTION_RUN) {
        insertionSortKType(a, start, start + KTYPE_INSERTION_RUN, comparator)
        start += KTYPE_INSERTION_RUN
    }
    insertionSortKType(a, start, to, comparator)
    if (n <= KTYPE_INSERTION_RUN) return

    // The longest left run is the largest doubling of the run length that is still below n.
    var longestLeftRun = KTYPE_INSERTION_RUN
    while (n - longestLeftRun > longestLeftRun) longestLeftRun *= 2
    val buffer = KTypeArray(longestLeftRun)
    var width = KTYPE_INSERTION_RUN
    while (true) {
        var lo = from
        while (to - lo > width) {
            val mid = lo + width
            val hi = if (to - mid > width) mid + width else to
            mergeKType(a, lo, mid, hi, buffer, comparator)
            lo = hi
        }
        // A width of at least n / 2 just merged the last two runs. Doubling it could also overflow.
        if (n - width <= width) return
        width *= 2
    }
}

private fun insertionSortKType(a: KTypeArray, from: Int, to: Int, comparator: KTypeComparator) {
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

private fun mergeKType(a: KTypeArray, lo: Int, mid: Int, hi: Int, buffer: KTypeArray, comparator: KTypeComparator) {
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
