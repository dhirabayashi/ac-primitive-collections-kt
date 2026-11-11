// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeIndexedMinHeap.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/**
 * A priority queue over the indices `0 until indexCount`, each with a Long key, that can change or remove
 * the key of any index. Holds each index at most once.
 * The index with the largest key comes first.
 * The order among equal keys is unspecified.
 *
 * Unlike a heap of pairs, it never holds stale entries, so its size stays at most `indexCount`.
 * It reserves three arrays of `indexCount` up front.
 *
 * ```
 * val queue = LongIndexedMaxHeap(n)
 * queue[start] = 0L
 * while (queue.isNotEmpty()) {
 *     val distance = queue.firstKey()
 *     val vertex = queue.removeFirst()
 *     // For a better distance d to u: queue[u] = d
 * }
 * ```
 */
class LongIndexedMaxHeap private constructor(
    /** The index at each heap position. */
    private val heap: IntArray,
    /** The key at each heap position, next to [heap] so that sifting reads one array per step. */
    private val keys: LongArray,
    /** The heap position of each index, or -1 when absent. */
    private val positions: IntArray,
    private var length: Int,
) {
    constructor(indexCount: Int) : this(
        IntArray(checkInitialCapacity(indexCount)),
        LongArray(indexCount),
        IntArray(indexCount).also { it.fill(-1) },
        0,
    )

    /** Changes with every change to the entries, so that [forEachUnordered] can fail instead of skipping some. */
    @PublishedApi
    internal var modCount: Int = 0

    val size: Int get() = length

    /** The heap accepts the indices `0 until indexCount`. */
    val indexCount: Int get() = positions.size

    fun isEmpty(): Boolean = length == 0
    fun isNotEmpty(): Boolean = length != 0

    operator fun contains(index: Int): Boolean = positions[checkIndex(index)] >= 0

    /** Throws `NoSuchElementException` if [index] is absent. */
    fun keyOf(index: Int): Long = keys[positionOf(index)]

    fun firstIndex(): Int {
        if (length == 0) throwEmpty()
        return heap[0]
    }

    fun firstKey(): Long {
        if (length == 0) throwEmpty()
        return keys[0]
    }

    /** Adds [index] with [key], or changes its key if present. */
    operator fun set(index: Int, key: Long) {
        val position = positions[checkIndex(index)]
        if (position < 0) {
            val n = length
            length = n + 1
            siftUp(n, index, key)
        } else {
            changeKey(position, index, key)
        }
        modCount++
    }

    /** Changes the key of [index] to a key that is not greater. Throws if [index] is absent or [key] is greater. */
    fun decreaseKey(index: Int, key: Long) {
        val position = positionOf(index)
        if ((keys[position] < key)) throw IllegalArgumentException("$key is greater than the key of $index, ${keys[position]}")
        changeKey(position, index, key)
        modCount++
    }

    /** Changes the key of [index] to a key that is not less. Throws if [index] is absent or [key] is less. */
    fun increaseKey(index: Int, key: Long) {
        val position = positionOf(index)
        if ((key < keys[position])) throw IllegalArgumentException("$key is less than the key of $index, ${keys[position]}")
        changeKey(position, index, key)
        modCount++
    }

    /** Removes [index] and returns true, or returns false if it is absent. */
    fun remove(index: Int): Boolean {
        val position = positions[checkIndex(index)]
        if (position < 0) return false
        removeAt(position)
        return true
    }

    /** Removes the first entry and returns its index. */
    fun removeFirst(): Int {
        if (length == 0) throwEmpty()
        val index = heap[0]
        removeAt(0)
        return index
    }

    /** O(size), not O(indexCount). */
    fun clear() {
        var i = 0
        while (i < length) {
            positions[heap[i]] = -1
            i++
        }
        length = 0
        modCount++
    }

    fun copy(): LongIndexedMaxHeap = LongIndexedMaxHeap(heap.copyOf(), keys.copyOf(), positions.copyOf(), length)

    /** Visits every entry in no particular order. Changing the heap from [action] throws `ConcurrentModificationException`. */
    inline fun forEachUnordered(action: (index: Int, key: Long) -> Unit) {
        val expected = modCount
        var i = 0
        while (i < size) {
            action(indexAt(i), keyAt(i))
            if (modCount != expected) throwConcurrentModification()
            i++
        }
    }

    @PublishedApi
    internal fun indexAt(position: Int): Int = heap[position]

    @PublishedApi
    internal fun keyAt(position: Int): Long = keys[position]

    /** Lists the entries in index order, as `{index=key, ...}`. O(indexCount). */
    override fun toString(): String {
        val builder = StringBuilder("{")
        var index = 0
        while (index < positions.size) {
            val position = positions[index]
            if (position >= 0) {
                if (builder.length > 1) builder.append(", ")
                builder.append(index).append('=').append(keys[position])
            }
            index++
        }
        return builder.append('}').toString()
    }

    /** Two heaps are equal if they accept the same indices and hold the same keys for them, whatever their layout. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LongIndexedMaxHeap || length != other.length || indexCount != other.indexCount) return false
        var i = 0
        while (i < length) {
            val theirs = other.positions[heap[i]]
            if (theirs < 0 || !(keys[i] == other.keys[theirs])) return false
            i++
        }
        return true
    }

    /** Independent of the internal layout, to match [equals]. */
    override fun hashCode(): Int {
        var result = indexCount
        var i = 0
        while (i < length) {
            result += 31 * heap[i] + keys[i].hashCode()
            i++
        }
        return result
    }

    private fun checkIndex(index: Int): Int {
        if (index < 0 || index >= positions.size) throwIndexOutOfBounds(index, positions.size)
        return index
    }

    private fun positionOf(index: Int): Int {
        val position = positions[checkIndex(index)]
        if (position < 0) throw NoSuchElementException("Index $index is not in the heap")
        return position
    }

    private fun changeKey(position: Int, index: Int, key: Long) {
        if ((keys[position] < key)) siftUp(position, index, key) else siftDown(position, index, key)
    }

    private fun removeAt(position: Int) {
        positions[heap[position]] = -1
        val last = --length
        if (position != last) {
            // The last entry can belong above or below the hole, as with any key change.
            changeKey(position, heap[last], keys[last])
        }
        modCount++
    }

    /** Moves a hole at [start] toward the root, then fills it with [index] and [key]. */
    private fun siftUp(start: Int, index: Int, key: Long) {
        val h = heap
        val k = keys
        val p = positions
        var hole = start
        while (hole > 0) {
            val parent = (hole - 1) ushr HEAP_ARITY_SHIFT
            if (!(k[parent] < key)) break
            h[hole] = h[parent]
            k[hole] = k[parent]
            p[h[hole]] = hole
            hole = parent
        }
        h[hole] = index
        k[hole] = key
        p[index] = hole
    }

    /** Moves a hole at [start] toward the leaves, then fills it with [index] and [key]. */
    private fun siftDown(start: Int, index: Int, key: Long) {
        val h = heap
        val k = keys
        val p = positions
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
                if ((k[best] < k[child])) best = child
                child++
            }
            if (!(key < k[best])) break
            h[hole] = h[best]
            k[hole] = k[best]
            p[h[hole]] = hole
            hole = best
        }
        h[hole] = index
        k[hole] = key
        p[index] = hole
    }
}
