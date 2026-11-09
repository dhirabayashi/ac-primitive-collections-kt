// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeMinHeap.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/**
 * A priority queue of Long values that never boxes them.
 * The smallest value comes first.
 *
 * Replaces `PriorityQueue<Long>`.
 *
 * ```
 * val queue = LongMinHeap()
 * queue += x
 * while (queue.isNotEmpty()) {
 *     val first = queue.removeFirst()
 * }
 * ```
 *
 * The constructor argument is a capacity, not a size: `LongMinHeap(3)` is empty.
 */
class LongMinHeap @PublishedApi internal constructor(
    private var elements: LongArray,
    private var length: Int,
) {
    constructor(initialCapacity: Int = DEFAULT_CAPACITY) : this(LongArray(checkInitialCapacity(initialCapacity)), 0)

    /** Changes with every change to the values, so that [forEachUnordered] can fail instead of skipping some. */
    @PublishedApi
    internal var modCount: Int = 0

    val size: Int get() = length
    val capacity: Int get() = elements.size

    fun isEmpty(): Boolean = length == 0
    fun isNotEmpty(): Boolean = length != 0

    fun first(): Long {
        if (length == 0) throwEmpty()
        return elements[0]
    }

    fun firstOrDefault(default: Long): Long = if (length == 0) default else elements[0]

    fun add(value: Long) {
        val n = length
        if (n == elements.size) grow(n + 1L)
        length = n + 1
        siftUp(n, value)
        modCount++
    }

    operator fun plusAssign(value: Long) = add(value)

    fun addAll(values: LongArray) = addAll(values, 0, values.size)

    /**
     * Adds `values[from until to]`. Rebuilds the heap in O(size) instead when that adds at least as many
     * values as the heap holds, so filling an empty heap is O(n).
     */
    fun addAll(values: LongArray, from: Int, to: Int) {
        checkRange(from, to, values.size)
        val count = to - from
        if (count == 0) return
        val old = length
        if (elements.size - old < count) grow(old.toLong() + count)
        System.arraycopy(values, from, elements, old, count)
        settleAppended(old, old + count)
    }

    fun addAll(other: LongMinHeap) {
        // Read the length first: adding a heap to itself must add each value once.
        val count = other.length
        if (count == 0) return
        val old = length
        if (elements.size - old < count) grow(old.toLong() + count)
        System.arraycopy(other.elements, 0, elements, old, count)
        settleAppended(old, old + count)
    }

    /** Restores the heap after values were copied into `[old, end)`. */
    private fun settleAppended(old: Int, end: Int) {
        length = end
        if (end - old >= old) {
            heapify()
        } else {
            var i = old
            while (i < end) {
                siftUp(i, elements[i])
                i++
            }
        }
        modCount++
    }

    fun removeFirst(): Long {
        if (length == 0) throwEmpty()
        return removeRoot()
    }

    fun removeFirstOrDefault(default: Long): Long = if (length == 0) default else removeRoot()

    private fun removeRoot(): Long {
        val first = elements[0]
        val last = --length
        if (last > 0) siftDown(0, elements[last])
        modCount++
        return first
    }

    /** Does the work of `removeFirst` and `add` with one sift. Returns the value it removed. */
    fun replaceFirst(value: Long): Long {
        if (length == 0) throwEmpty()
        val first = elements[0]
        siftDown(0, value)
        modCount++
        return first
    }

    fun clear() {
        length = 0
        modCount++
    }

    fun ensureCapacity(minCapacity: Int) {
        if (minCapacity > elements.size) grow(minCapacity.toLong())
    }

    fun copy(): LongMinHeap = LongMinHeap(elements.copyOf(), length)

    /** Visits every value in no particular order. Changing the heap from [action] throws `ConcurrentModificationException`. */
    inline fun forEachUnordered(action: (Long) -> Unit) {
        val expected = modCount
        var i = 0
        while (i < size) {
            action(elementAt(i))
            if (modCount != expected) throwConcurrentModification()
            i++
        }
    }

    @PublishedApi
    internal fun elementAt(index: Int): Long = elements[index]

    /** Returns the values in no particular order. */
    fun toLongArray(): LongArray = elements.copyOf(length)

    /** Returns the values in the order that [removeFirst] would return them, without changing the heap. O(n log n). */
    fun toSortedArray(): LongArray {
        val sorted = elements.copyOf(length)
        java.util.Arrays.sort(sorted)
        return sorted
    }

    /** Appends the values in the order that [removeFirst] would return them. */
    fun appendTo(builder: StringBuilder, separator: CharSequence = " ") {
        val sorted = toSortedArray()
        var i = 0
        while (i < sorted.size) {
            if (i > 0) builder.append(separator)
            builder.append(sorted[i])
            i++
        }
    }

    /** Lists the values in the order that [removeFirst] would return them. */
    override fun toString(): String {
        val builder = StringBuilder("[")
        appendTo(builder, ", ")
        return builder.append(']').toString()
    }

    /** Two heaps are equal if they hold the same values, whatever their internal layout. O(n log n). */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LongMinHeap || length != other.length) return false
        return toSortedArray().contentEquals(other.toSortedArray())
    }

    /** Independent of the internal layout, to match [equals]. */
    override fun hashCode(): Int {
        var result = length
        var i = 0
        while (i < length) {
            result += elements[i].hashCode()
            i++
        }
        return result
    }

    /** Floyd's bottom-up construction: O(size). */
    @PublishedApi
    internal fun heapify() {
        var parent = (length - 2) shr HEAP_ARITY_SHIFT
        while (parent >= 0) {
            siftDown(parent, elements[parent])
            parent--
        }
    }

    /** Moves a hole at [start] toward the root, then fills it. */
    private fun siftUp(start: Int, value: Long) {
        val a = elements
        var hole = start
        while (hole > 0) {
            val parent = (hole - 1) ushr HEAP_ARITY_SHIFT
            if (!(value < a[parent])) break
            a[hole] = a[parent]
            hole = parent
        }
        a[hole] = value
    }

    /** Moves a hole at [start] toward the leaves, then fills it. */
    private fun siftDown(start: Int, value: Long) {
        val a = elements
        val n = length
        // Comparing with the last parent first means the child index below cannot overflow.
        val lastParent = (n - 2) shr HEAP_ARITY_SHIFT
        var hole = start
        while (hole <= lastParent) {
            val firstChild = (hole shl HEAP_ARITY_SHIFT) + 1
            val endChild = if (n - firstChild > HEAP_ARITY) firstChild + HEAP_ARITY else n
            var best = firstChild
            var child = firstChild + 1
            while (child < endChild) {
                if ((a[child] < a[best])) best = child
                child++
            }
            if (!(a[best] < value)) break
            a[hole] = a[best]
            hole = best
        }
        a[hole] = value
    }

    private fun grow(required: Long) {
        elements = elements.copyOf(nextCapacity(elements.size, required))
    }
}

/** Builds the heap in O(n). */
fun longMinHeapOf(vararg values: Long): LongMinHeap = values.copyOf().heapOfLongMinHeap()

/** Builds the heap in O(n). */
fun LongArray.toLongMinHeap(): LongMinHeap = copyOf().heapOfLongMinHeap()

/** Takes this array as the heap's own. */
private fun LongArray.heapOfLongMinHeap(): LongMinHeap {
    val heap = LongMinHeap(this, size)
    heap.heapify()
    return heap
}
