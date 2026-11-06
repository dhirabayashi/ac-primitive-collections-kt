// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeVTypeMinHeap.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/**
 * A priority queue of (key, value) pairs without `Pair`, or boxing of the key.
 * The largest key comes first.
 *
 * Replaces `PriorityQueue<Pair<Double, Long>>` with a reversed comparator.
 * Keys follow the total order of `compareTo`, the same as `sort`: `-0.0` is below `0.0`, and `NaN` is above everything.
 * The order among equal keys is unspecified.
 *
 * ```
 * val queue = DoubleLongMaxHeap()
 * queue.add(0.0, start)
 * while (queue.isNotEmpty()) {
 *     val distance = queue.firstKey()
 *     val vertex = queue.firstValue()
 *     queue.removeFirst()
 * }
 * ```
 */
@Suppress("UNCHECKED_CAST")
class DoubleLongMaxHeap private constructor(
    private var keys: DoubleArray,
    private var values: LongArray,
    private var length: Int,
) {
    constructor(initialCapacity: Int = DEFAULT_CAPACITY) : this(
        DoubleArray(checkInitialCapacity(initialCapacity)),
        LongArray(initialCapacity),
        0,
    )

    /** Changes with every change to the pairs, so that [forEachUnordered] can fail instead of skipping some. */
    @PublishedApi
    internal var modCount: Int = 0

    val size: Int get() = length
    val capacity: Int get() = keys.size

    fun isEmpty(): Boolean = length == 0
    fun isNotEmpty(): Boolean = length != 0

    fun firstKey(): Double {
        if (length == 0) throwEmpty()
        return keys[0]
    }

    fun firstValue(): Long {
        if (length == 0) throwEmpty()
        return values[0]
    }

    fun add(key: Double, value: Long) {
        val n = length
        if (n == keys.size) grow(n + 1L)
        length = n + 1
        siftUp(n, key, value)
        modCount++
    }

    fun removeFirst() {
        if (length == 0) throwEmpty()
        val last = --length
        if (last > 0) siftDown(keys[last], values[last])
        modCount++
    }

    /** Does the work of `removeFirst` and `add` with one sift. */
    fun replaceFirst(key: Double, value: Long) {
        if (length == 0) throwEmpty()
        siftDown(key, value)
        modCount++
    }

    fun clear() {
        length = 0
        modCount++
    }

    fun ensureCapacity(minCapacity: Int) {
        if (minCapacity > keys.size) grow(minCapacity.toLong())
    }

    fun copy(): DoubleLongMaxHeap = DoubleLongMaxHeap(keys.copyOf(), values.copyOf(), length)

    /** Visits every pair in no particular order. Changing the heap from [action] throws `ConcurrentModificationException`. */
    inline fun forEachUnordered(action: (key: Double, value: Long) -> Unit) {
        val expected = modCount
        var i = 0
        while (i < size) {
            action(keyAt(i), valueAt(i))
            if (modCount != expected) throwConcurrentModification()
            i++
        }
    }

    @PublishedApi
    internal fun keyAt(index: Int): Double = keys[index]

    @PublishedApi
    internal fun valueAt(index: Int): Long = values[index]

    /** Moves a hole at [start] toward the root, then fills it. */
    private fun siftUp(start: Int, key: Double, value: Long) {
        val k = keys
        val v = values
        var hole = start
        while (hole > 0) {
            val parent = (hole - 1) ushr HEAP_ARITY_SHIFT
            if (!(k[parent].compareTo(key) < 0)) break
            k[hole] = k[parent]
            v[hole] = v[parent]
            hole = parent
        }
        k[hole] = key
        v[hole] = value
    }

    /** Moves a hole at the root toward the leaves, then fills it. */
    private fun siftDown(key: Double, value: Long) {
        val k = keys
        val v = values
        val n = length
        // Comparing with the last parent first means the child index below cannot overflow.
        val lastParent = (n - 2) shr HEAP_ARITY_SHIFT
        var hole = 0
        while (hole <= lastParent) {
            val firstChild = (hole shl HEAP_ARITY_SHIFT) + 1
            val endChild = if (n - firstChild > HEAP_ARITY) firstChild + HEAP_ARITY else n
            var best = firstChild
            var child = firstChild + 1
            while (child < endChild) {
                if ((k[best].compareTo(k[child]) < 0)) best = child
                child++
            }
            if (!(key.compareTo(k[best]) < 0)) break
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
        if (other !is DoubleLongMaxHeap || length != other.length) return false
        val mine = copyOfUsedPart()
        val theirs = other.copyOfUsedPart()
        val myRun = LongArray(length)
        val theirRun = LongArray(length)
        while (mine.length > 0) {
            // Values under equal keys come out in an arbitrary order, so compare each run as a multiset.
            val key = mine.firstKey()
            var run = 0
            while (mine.length > 0 && (mine.firstKey().compareTo(key) == 0)) {
                // The sizes match and each step removes one from both, so theirs is not empty here.
                if (!(theirs.firstKey().compareTo(key) == 0)) return false
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

    /** For [equals], whose cost should not depend on a large reserved capacity. */
    private fun copyOfUsedPart(): DoubleLongMaxHeap = DoubleLongMaxHeap(keys.copyOf(length), values.copyOf(length), length)

    /** Compares the first [n] items of both arrays in any order. May reorder them. */
    private fun sameValues(mine: LongArray, theirs: LongArray, n: Int): Boolean {
        java.util.Arrays.sort(mine, 0, n)
        java.util.Arrays.sort(theirs, 0, n)
        return java.util.Arrays.equals(mine, 0, n, theirs, 0, n)
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
}
