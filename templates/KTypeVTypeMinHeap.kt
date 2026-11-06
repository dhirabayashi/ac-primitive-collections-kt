// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

/**
 * A priority queue of (key, value) pairs without `Pair`, or boxing of the key.
//#if K_MAX_HEAP
 * The largest key comes first.
 *
 * Replaces `PriorityQueue<Pair<KType, VType>>` with a reversed comparator.
//#else
 * The smallest key comes first.
 *
 * Replaces `PriorityQueue<Pair<KType, VType>>`.
//#endif
//#if K_FLOATING
 * Keys follow the total order of `compareTo`, the same as `sort`: `-0.0` is below `0.0`, and `NaN` is above everything.
//#endif
 * The order among equal keys is unspecified.
 *
 * ```
 * val queue = KTypeVTypeMinHeap/*<V>*/()
 * queue.add(K_ZERO, start)
 * while (queue.isNotEmpty()) {
 *     val distance = queue.firstKey()
 *     val vertex = queue.firstValue()
 *     queue.removeFirst()
 * }
 * ```
 */
@Suppress("UNCHECKED_CAST")
class KTypeVTypeMinHeap/*<V>*/ private constructor(
    private var keys: KTypeArray,
    private var values: VTypeArray,
    private var length: Int,
) {
    constructor(initialCapacity: Int = DEFAULT_CAPACITY) : this(
        KTypeArray(checkInitialCapacity(initialCapacity)),
        V_NEW_ARRAY(initialCapacity),
        0,
    )

    /** Changes with every change to the pairs, so that [forEachUnordered] can fail instead of skipping some. */
    @PublishedApi
    internal var modCount: Int = 0

    val size: Int get() = length
    val capacity: Int get() = keys.size

    fun isEmpty(): Boolean = length == 0
    fun isNotEmpty(): Boolean = length != 0

    fun firstKey(): KType {
        if (length == 0) throwEmpty()
        return keys[0]
    }

    fun firstValue(): VType {
        if (length == 0) throwEmpty()
        return V_CAST(values[0])
    }

    fun add(key: KType, value: VType) {
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
        //#if V_OBJ
        values[last] = V_NULL
        //#endif
        modCount++
    }

    /** Does the work of `removeFirst` and `add` with one sift. */
    fun replaceFirst(key: KType, value: VType) {
        if (length == 0) throwEmpty()
        siftDown(key, value)
        modCount++
    }

    fun clear() {
        //#if V_OBJ
        values.fill(V_NULL, 0, length)
        //#endif
        length = 0
        modCount++
    }

    fun ensureCapacity(minCapacity: Int) {
        if (minCapacity > keys.size) grow(minCapacity.toLong())
    }

    fun copy(): KTypeVTypeMinHeap/*<V>*/ = KTypeVTypeMinHeap(keys.copyOf(), values.copyOf(), length)

    /** Visits every pair in no particular order. Changing the heap from [action] throws `ConcurrentModificationException`. */
    inline fun forEachUnordered(action: (key: KType, value: VType) -> Unit) {
        val expected = modCount
        var i = 0
        while (i < size) {
            action(keyAt(i), valueAt(i))
            if (modCount != expected) throwConcurrentModification()
            i++
        }
    }

    @PublishedApi
    internal fun keyAt(index: Int): KType = keys[index]

    @PublishedApi
    internal fun valueAt(index: Int): VType = V_CAST(values[index])

    /** Moves a hole at [start] toward the root, then fills it. */
    private fun siftUp(start: Int, key: KType, value: VStored) {
        val k = keys
        val v = values
        var hole = start
        while (hole > 0) {
            val parent = (hole - 1) ushr HEAP_ARITY_SHIFT
            if (!K_BEFORE(key, k[parent])) break
            k[hole] = k[parent]
            v[hole] = v[parent]
            hole = parent
        }
        k[hole] = key
        v[hole] = value
    }

    /** Moves a hole at the root toward the leaves, then fills it. */
    private fun siftDown(key: KType, value: VStored) {
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
                if (K_BEFORE(k[child], k[best])) best = child
                child++
            }
            if (!K_BEFORE(k[best], key)) break
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
        if (other !is KTypeVTypeMinHeap/*<*>*/ || length != other.length) return false
        val mine = copyOfUsedPart()
        val theirs = other.copyOfUsedPart()
        val myRun = V_NEW_ARRAY(length)
        val theirRun = V_NEW_ARRAY(length)
        while (mine.length > 0) {
            // Values under equal keys come out in an arbitrary order, so compare each run as a multiset.
            val key = mine.firstKey()
            var run = 0
            while (mine.length > 0 && K_EQ(mine.firstKey(), key)) {
                // The sizes match and each step removes one from both, so theirs is not empty here.
                if (!K_EQ(theirs.firstKey(), key)) return false
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
    private fun copyOfUsedPart(): KTypeVTypeMinHeap/*<V>*/ = KTypeVTypeMinHeap(keys.copyOf(length), values.copyOf(length), length)

    /** Compares the first [n] items of both arrays in any order. May reorder them. */
    private fun sameValues(mine: VTypeArray, theirs: VTypeArray, n: Int): Boolean {
        //#if V_OBJ
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
        //#else
        java.util.Arrays.sort(mine, 0, n)
        java.util.Arrays.sort(theirs, 0, n)
        return java.util.Arrays.equals(mine, 0, n, theirs, 0, n)
        //#endif
    }

    /** Independent of the internal layout, to match [equals]. */
    override fun hashCode(): Int {
        var result = length
        var i = 0
        while (i < length) {
            result += 31 * K_HASH(keys[i]) + values[i].hashCode()
            i++
        }
        return result
    }
}
