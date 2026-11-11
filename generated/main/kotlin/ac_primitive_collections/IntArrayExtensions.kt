// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeArrayExtensions.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

// Extensions on the standard IntArray, for what the standard library lacks or offers only with boxing.
// Their List counterparts are separate code, so that a submission that uses one does not carry the other.

/** Requires a sorted array. Returns the first index whose element is not less than [value]. */
fun IntArray.lowerBound(value: Int): Int = lowerBound(value, 0, size)

fun IntArray.lowerBound(value: Int, from: Int, to: Int): Int {
    checkRange(from, to, size)
    var lo = from
    var hi = to
    while (lo < hi) {
        val mid = (lo + hi) ushr 1
        if ((this[mid] < value)) lo = mid + 1 else hi = mid
    }
    return lo
}

/** Requires a sorted array. Returns the first index whose element is greater than [value]. */
fun IntArray.upperBound(value: Int): Int = upperBound(value, 0, size)

fun IntArray.upperBound(value: Int, from: Int, to: Int): Int {
    checkRange(from, to, size)
    var lo = from
    var hi = to
    while (lo < hi) {
        val mid = (lo + hi) ushr 1
        if ((value < this[mid])) hi = mid else lo = mid + 1
    }
    return lo
}

/** Returns the indices that sort the array, keeping equal elements in their original order. */
fun IntArray.argsort(): IntArray = this.argsortBy { it }

/** Returns the indices that sort the array by an Int key, keeping ties in their original order. */
inline fun IntArray.argsortBy(selector: (Int) -> Int): IntArray {
    val keys = LongArray(size)
    var i = 0
    while (i < size) {
        keys[i] = packKeyAndIndex(selector(this[i]), i)
        i++
    }
    return sortedIndices(keys)
}

/** Returns the indices that sort the array by a Long key, keeping ties in their original order. */
inline fun IntArray.argsortByLong(selector: (Int) -> Long): IntArray {
    val keys = LongArray(size)
    var i = 0
    while (i < size) {
        keys[i] = selector(this[i])
        i++
    }
    return sortedIndicesByKey(keys)
}

/** Returns the indices that sort the array by a Double key in the order of `Double.compareTo`, keeping ties in their original order. */
inline fun IntArray.argsortByDouble(selector: (Int) -> Double): IntArray = argsortByLong { totalOrderKey(selector(it)) }

/** Sorts stably by an Int key without a comparator. */
inline fun IntArray.sortBy(selector: (Int) -> Int) = permute(argsortBy(selector))

/** Sorts stably by a Long key without a comparator. */
inline fun IntArray.sortByLong(selector: (Int) -> Long) = permute(argsortByLong(selector))

/** Sorts stably by a Double key without a comparator, in the order of `Double.compareTo`. */
inline fun IntArray.sortByDouble(selector: (Int) -> Double) = permute(argsortByDouble(selector))

/** Puts the element at `indices[i]` at `i`. */
@PublishedApi
internal fun IntArray.permute(indices: IntArray) {
    val source = copyOf()
    var i = 0
    while (i < indices.size) {
        this[i] = source[indices[i]]
        i++
    }
}

/**
 * Sorts stably in O(n log n) with a merge sort.
 *
 * A comparator that is not a total order gives an unspecified order, but still a permutation of the input.
 */
fun IntArray.sortWith(comparator: IntComparator) = mergeSortInt(this, 0, size, comparator)

fun IntArray.sortWith(comparator: IntComparator, from: Int, to: Int) {
    checkRange(from, to, size)
    mergeSortInt(this, from, to, comparator)
}

/**
 * Moves the first of each run of equal adjacent elements to the front, in order, and returns how many there are.
 * An array cannot shrink, so the elements from the returned count on are left unspecified.
 * On a sorted array, the front then holds the distinct elements: `val m = a.dedupe()`, then use `a[0 until m]`.
 */
fun IntArray.dedupe(): Int {
    val n = size
    if (n < 2) return n
    var write = 1
    var read = 1
    while (read < n) {
        if (!(this[read] == this[write - 1])) {
            this[write] = this[read]
            write++
        }
        read++
    }
    return write
}

/**
 * Returns the rank of each element among the distinct elements, from 0: coordinate compression.
 * The array itself is left alone.
 *
 * To map a rank back to its element, sort a copy and dedupe it: `val sorted = a.copyOf(); sorted.sort()`,
 * then `sorted.dedupe()`, and `sorted[rank]`.
 */
fun IntArray.compress(): IntArray {
    val order = argsort()
    val ranks = IntArray(size)
    var rank = 0
    var i = 0
    while (i < order.size) {
        val index = order[i]
        if (i > 0 && !(this[order[i - 1]] == this[index])) rank++
        ranks[index] = rank
        i++
    }
    return ranks
}

/** Advances to the next permutation in lexicographic order. At the last one, wraps to the first and returns false. */
fun IntArray.nextPermutation(): Boolean {
    val n = size
    if (n < 2) return false
    var i = n - 2
    while (i >= 0 && !(this[i] < this[i + 1])) i--
    if (i >= 0) {
        var j = n - 1
        while (!(this[i] < this[j])) j--
        val t = this[i]
        this[i] = this[j]
        this[j] = t
    }
    var lo = i + 1
    var hi = n - 1
    while (lo < hi) {
        val t = this[lo]
        this[lo] = this[hi]
        this[hi] = t
        lo++
        hi--
    }
    return i >= 0
}

/** Returns the `size + 1` prefix sums: element `i` is the sum of the first `i` elements, so `[l, r)` sums to `s[r] - s[l]`. */
fun IntArray.prefixSum(): LongArray {
    val sums = LongArray(size + 1)
    var total = 0L
    var i = 0
    while (i < size) {
        total += this[i].toLong()
        sums[i + 1] = total
        i++
    }
    return sums
}
