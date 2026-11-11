// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

/**
 * A priority queue of KType values that never boxes them.
//#if K_MAX_HEAP
 * The largest value comes first.
 *
 * Replaces `PriorityQueue<KType>` with a reversed comparator.
//#else
 * The smallest value comes first.
 *
 * Replaces `PriorityQueue<KType>`.
//#endif
//#if K_FLOATING
 * Values follow the total order of `compareTo`, the same as `sort`: `-0.0` is below `0.0`, and `NaN` is above everything.
//#endif
 *
 * ```
 * val queue = KTypeMinHeap()
 * queue += x
 * while (queue.isNotEmpty()) {
 *     val first = queue.removeFirst()
 * }
 * ```
 *
 * The constructor argument is a capacity, not a size: `KTypeMinHeap(3)` is empty.
 */
class KTypeMinHeap @PublishedApi internal constructor(
    private var elements: KTypeArray,
    private var length: Int,
) {
    constructor(initialCapacity: Int = DEFAULT_CAPACITY) : this(KTypeArray(checkInitialCapacity(initialCapacity)), 0)

    /** Changes with every change to the values, so that [forEachUnordered] can fail instead of skipping some. */
    @PublishedApi
    internal var modCount: Int = 0

    val size: Int get() = length
    val capacity: Int get() = elements.size

    fun isEmpty(): Boolean = length == 0
    fun isNotEmpty(): Boolean = length != 0

    fun first(): KType {
        if (length == 0) throwEmpty()
        return elements[0]
    }

    fun firstOrDefault(default: KType): KType = if (length == 0) default else elements[0]

    fun add(value: KType) {
        val n = length
        if (n == elements.size) grow(n + 1L)
        length = n + 1
        siftUp(n, value)
        modCount++
    }

    operator fun plusAssign(value: KType) = add(value)

    fun addAll(values: KTypeArray) = addAll(values, 0, values.size)

    /**
     * Adds `values[from until to]`. Rebuilds the heap in O(size) instead when that adds at least as many
     * values as the heap holds, so filling an empty heap is O(n).
     */
    fun addAll(values: KTypeArray, from: Int, to: Int) {
        checkRange(from, to, values.size)
        val count = to - from
        if (count == 0) return
        val old = length
        if (elements.size - old < count) grow(old.toLong() + count)
        System.arraycopy(values, from, elements, old, count)
        settleAppended(old, old + count)
    }

    fun addAll(other: KTypeMinHeap) {
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

    fun removeFirst(): KType {
        if (length == 0) throwEmpty()
        return removeRoot()
    }

    fun removeFirstOrDefault(default: KType): KType = if (length == 0) default else removeRoot()

    private fun removeRoot(): KType {
        val first = elements[0]
        val last = --length
        if (last > 0) siftDown(0, elements[last])
        modCount++
        return first
    }

    /** Does the work of `removeFirst` and `add` with one sift. Returns the value it removed. */
    fun replaceFirst(value: KType): KType {
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

    fun copy(): KTypeMinHeap = KTypeMinHeap(elements.copyOf(), length)

    /** Visits every value in no particular order. Changing the heap from [action] throws `ConcurrentModificationException`. */
    inline fun forEachUnordered(action: (KType) -> Unit) {
        val expected = modCount
        var i = 0
        while (i < size) {
            action(elementAt(i))
            if (modCount != expected) throwConcurrentModification()
            i++
        }
    }

    @PublishedApi
    internal fun elementAt(index: Int): KType = elements[index]

    /** Returns the values in no particular order. */
    fun toKTypeArray(): KTypeArray = elements.copyOf(length)

    /** Returns the values in the order that [removeFirst] would return them, without changing the heap. O(n log n). */
    fun toSortedArray(): KTypeArray {
        val sorted = elements.copyOf(length)
        java.util.Arrays.sort(sorted)
        //#if K_MAX_HEAP
        var i = 0
        var j = sorted.size - 1
        while (i < j) {
            val t = sorted[i]
            sorted[i] = sorted[j]
            sorted[j] = t
            i++
            j--
        }
        //#endif
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
        if (other !is KTypeMinHeap || length != other.length) return false
        return toSortedArray().contentEquals(other.toSortedArray())
    }

    /** Independent of the internal layout, to match [equals]. */
    override fun hashCode(): Int {
        var result = length
        var i = 0
        while (i < length) {
            result += K_HASH(elements[i])
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
    private fun siftUp(start: Int, value: KType) {
        val a = elements
        var hole = start
        while (hole > 0) {
            val parent = (hole - 1) ushr HEAP_ARITY_SHIFT
            if (!K_BEFORE(value, a[parent])) break
            a[hole] = a[parent]
            hole = parent
        }
        a[hole] = value
    }

    /** Moves a hole at [start] toward the leaves, then fills it. */
    private fun siftDown(start: Int, value: KType) {
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
                if (K_BEFORE(a[child], a[best])) best = child
                child++
            }
            if (!K_BEFORE(a[best], value)) break
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
fun ktypeMinHeapOf(vararg values: KType): KTypeMinHeap = values.copyOf().heapOfKTypeMinHeap()

/** Builds the heap in O(n). */
fun KTypeArray.toKTypeMinHeap(): KTypeMinHeap = copyOf().heapOfKTypeMinHeap()

/** Takes this array as the heap's own. */
private fun KTypeArray.heapOfKTypeMinHeap(): KTypeMinHeap {
    val heap = KTypeMinHeap(this, size)
    heap.heapify()
    return heap
}
