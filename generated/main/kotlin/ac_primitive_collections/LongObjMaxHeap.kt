// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeVTypeMinHeap.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/**
 * A priority queue of (key, value) pairs without `Pair`, or boxing of the key.
 * The largest key comes first.
 *
 * Replaces `PriorityQueue<Pair<Long, V>>` with a reversed comparator.
 * The order among equal keys is unspecified.
 *
 * ```
 * val queue = LongObjMaxHeap<V>()
 * queue.add(0L, start)
 * while (queue.isNotEmpty()) {
 *     val distance = queue.firstKey()
 *     val vertex = queue.firstValue()
 *     queue.removeFirst()
 * }
 * ```
 */
@Suppress("UNCHECKED_CAST")
class LongObjMaxHeap<V> private constructor(
    private var keys: LongArray,
    private var values: Array<Any?>,
    private var length: Int,
) {
    constructor(initialCapacity: Int = DEFAULT_CAPACITY) : this(
        LongArray(checkInitialCapacity(initialCapacity)),
        arrayOfNulls<Any?>(initialCapacity),
        0,
    )

    val size: Int get() = length
    val capacity: Int get() = keys.size

    fun isEmpty(): Boolean = length == 0
    fun isNotEmpty(): Boolean = length != 0

    fun firstKey(): Long {
        if (length == 0) throwEmpty()
        return keys[0]
    }

    fun firstValue(): V {
        if (length == 0) throwEmpty()
        return (values[0] as V)
    }

    fun add(key: Long, value: V) {
        val n = length
        if (n == keys.size) grow(n + 1L)
        length = n + 1
        siftUp(n, key, value)
    }

    fun removeFirst() {
        if (length == 0) throwEmpty()
        val last = --length
        if (last > 0) siftDown(keys[last], values[last])
        values[last] = null
    }

    /** Does the work of `removeFirst` and `add` with one sift. */
    fun replaceFirst(key: Long, value: V) {
        if (length == 0) throwEmpty()
        siftDown(key, value)
    }

    fun clear() {
        values.fill(null, 0, length)
        length = 0
    }

    fun ensureCapacity(minCapacity: Int) {
        if (minCapacity > keys.size) grow(minCapacity.toLong())
    }

    fun copy(): LongObjMaxHeap<V> = LongObjMaxHeap(keys.copyOf(), values.copyOf(), length)

    /** Visits every pair in no particular order. */
    inline fun forEachUnordered(action: (key: Long, value: V) -> Unit) {
        var i = 0
        while (i < size) {
            action(keyAt(i), valueAt(i))
            i++
        }
    }

    @PublishedApi
    internal fun keyAt(index: Int): Long = keys[index]

    @PublishedApi
    internal fun valueAt(index: Int): V = (values[index] as V)

    /** Moves a hole at [start] toward the root, then fills it. */
    private fun siftUp(start: Int, key: Long, value: Any?) {
        val k = keys
        val v = values
        var hole = start
        while (hole > 0) {
            val parent = (hole - 1) ushr ARITY_SHIFT
            if (!(k[parent] < key)) break
            k[hole] = k[parent]
            v[hole] = v[parent]
            hole = parent
        }
        k[hole] = key
        v[hole] = value
    }

    /** Moves a hole at the root toward the leaves, then fills it. */
    private fun siftDown(key: Long, value: Any?) {
        val k = keys
        val v = values
        val n = length
        // Comparing with the last parent first means the child index below cannot overflow.
        val lastParent = (n - 2) shr ARITY_SHIFT
        var hole = 0
        while (hole <= lastParent) {
            val firstChild = (hole shl ARITY_SHIFT) + 1
            val endChild = if (n - firstChild > ARITY) firstChild + ARITY else n
            var best = firstChild
            var child = firstChild + 1
            while (child < endChild) {
                if ((k[best] < k[child])) best = child
                child++
            }
            if (!(key < k[best])) break
            k[hole] = k[best]
            v[hole] = v[best]
            hole = best
        }
        k[hole] = key
        v[hole] = value
    }

    private fun grow(required: Long) {
        val next = nextCapacity(keys.size, required)
        keys = keys.copyOf(next)
        values = values.copyOf(next)
    }

    /** Lists the pairs in the unordered array order, for debugging. */
    override fun toString(): String {
        val builder = StringBuilder("[")
        var i = 0
        while (i < length) {
            if (i > 0) builder.append(", ")
            builder.append(keys[i]).append('=').append(values[i])
            i++
        }
        return builder.append(']').toString()
    }

    /** Two heaps are equal if they hold the same pairs, whatever their internal layout. O(n log n). */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LongObjMaxHeap<*> || length != other.length) return false
        val mine = copy()
        val theirs = other.copy()
        val myRun = arrayOfNulls<Any?>(length)
        val theirRun = arrayOfNulls<Any?>(length)
        while (mine.length > 0) {
            // Values under equal keys come out in an arbitrary order, so compare each run as a multiset.
            val key = mine.firstKey()
            var run = 0
            while (mine.length > 0 && (mine.firstKey() == key)) {
                // The sizes match and each step removes one from both, so theirs is not empty here.
                if (!(theirs.firstKey() == key)) return false
                myRun[run] = mine.firstValue()
                theirRun[run] = theirs.firstValue()
                run++
                mine.removeFirst()
                theirs.removeFirst()
            }
            if (!sameValues(myRun, theirRun, run)) return false
        }
        return true
    }

    /** Compares the first [n] items of both arrays in any order. May reorder them. */
    private fun sameValues(mine: Array<Any?>, theirs: Array<Any?>, n: Int): Boolean {
        // Values need not be comparable, so this takes O(n^2) for n values under one key.
        var remaining = n
        var i = 0
        while (i < n) {
            var j = 0
            while (j < remaining && mine[i] != theirs[j]) j++
            if (j == remaining) return false
            remaining--
            theirs[j] = theirs[remaining]
            i++
        }
        return true
    }

    /** Independent of the internal layout, to match [equals]. */
    override fun hashCode(): Int {
        var result = length
        var i = 0
        while (i < length) {
            result += 31 * keys[i].hashCode() + values[i].hashCode()
            i++
        }
        return result
    }

    private companion object {
        /** A 4-ary heap is shallower, and its children share a cache line. See docs/judge-env.md. */
        const val ARITY_SHIFT = 2
        const val ARITY = 1 shl ARITY_SHIFT
    }
}
