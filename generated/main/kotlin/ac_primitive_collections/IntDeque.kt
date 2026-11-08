// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeDeque.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/**
 * A double-ended queue of Int that never boxes its elements. Replaces `ArrayDeque<Int>` as a queue or a stack.
 *
 * Adding or removing at either end is amortized O(1), and `d[i]` counts from the first element.
 *
 * `d + x` copies the whole deque, so a loop of `d = d + x` is O(n^2). Use `+=` in loops, and declare the
 * deque with `val`, because `d += x` on a `var` is ambiguous between `plus` and `plusAssign`.
 *
 * The constructor argument is a capacity, not a size: `IntDeque(3)` is empty. The capacity is a power of two.
 */
class IntDeque private constructor(
    @JvmField @PublishedApi internal var elements: IntArray,
    /** Where the first element lives. Element `i` lives at `(head + i) and (elements.size - 1)`. */
    @JvmField @PublishedApi internal var head: Int,
    @JvmField @PublishedApi internal var length: Int,
) {
    /** Counts structural changes, so that iteration can fail instead of skipping or repeating elements. */
    @JvmField
    @PublishedApi
    internal var modCount: Int = 0

    constructor(initialCapacity: Int = DEFAULT_CAPACITY) : this(IntArray(checkRingInitialCapacity(initialCapacity)), 0, 0)

    val size: Int get() = length
    val capacity: Int get() = elements.size
    val lastIndex: Int get() = length - 1

    /** Written as `IntRange(0, size - 1)` for the same reason as `IntList.indices`. */
    val indices: IntRange get() = IntRange(0, length - 1)

    fun isEmpty(): Boolean = length == 0
    fun isNotEmpty(): Boolean = length != 0

    @PublishedApi
    internal fun elementAt(index: Int): Int = elements[(head + index) and (elements.size - 1)]

    operator fun get(index: Int): Int {
        if (index < 0 || index >= length) throwIndexOutOfBounds(index, length)
        return elementAt(index)
    }

    operator fun set(index: Int, value: Int) {
        if (index < 0 || index >= length) throwIndexOutOfBounds(index, length)
        elements[(head + index) and (elements.size - 1)] = value
    }

    fun first(): Int {
        if (length == 0) throwEmpty()
        return elements[head]
    }

    fun last(): Int {
        if (length == 0) throwEmpty()
        return elementAt(length - 1)
    }

    fun firstOrDefault(default: Int): Int = if (length == 0) default else elements[head]
    fun lastOrDefault(default: Int): Int = if (length == 0) default else elementAt(length - 1)
    inline fun firstOrElse(default: () -> Int): Int = if (length == 0) default() else elements[head]
    inline fun lastOrElse(default: () -> Int): Int = if (length == 0) default() else elementAt(length - 1)

    fun addFirst(value: Int) {
        val n = length
        if (n == elements.size) grow(n + 1L)
        val h = (head - 1) and (elements.size - 1)
        elements[h] = value
        head = h
        length = n + 1
        modCount++
    }

    fun addLast(value: Int) {
        val n = length
        if (n == elements.size) grow(n + 1L)
        elements[(head + n) and (elements.size - 1)] = value
        length = n + 1
        modCount++
    }

    /** The same as [addLast], as in `ArrayDeque`. */
    fun add(value: Int) = addLast(value)

    operator fun plusAssign(value: Int) = addLast(value)
    operator fun plusAssign(other: IntDeque) = addAll(other)

    fun addAll(values: IntArray) = addAll(values, 0, values.size)

    fun addAll(values: IntArray, from: Int, to: Int) {
        checkRange(from, to, values.size)
        val n = length
        val count = to - from
        ensureCapacity(n.toLong() + count)
        val a = elements
        val mask = a.size - 1
        var i = 0
        while (i < count) {
            a[(head + n + i) and mask] = values[from + i]
            i++
        }
        finishAdd(n, count)
    }

    /** Appends at the end. Reads only positions that it does not write, so `d.addAll(d)` works. */
    fun addAll(other: IntDeque) {
        val n = length
        val count = other.length
        ensureCapacity(n.toLong() + count)
        val a = elements
        val mask = a.size - 1
        var i = 0
        while (i < count) {
            a[(head + n + i) and mask] = other.elementAt(i)
            i++
        }
        finishAdd(n, count)
    }

    private fun finishAdd(oldLength: Int, count: Int) {
        if (count == 0) return
        length = oldLength + count
        modCount++
    }

    fun removeFirst(): Int {
        if (length == 0) throwEmpty()
        val value = elements[head]
        head = (head + 1) and (elements.size - 1)
        length--
        modCount++
        return value
    }

    fun removeLast(): Int {
        if (length == 0) throwEmpty()
        val value = elementAt(length - 1)
        length--
        modCount++
        return value
    }

    /** Moves the elements on the shorter side of [index], so removing near either end is fast. */
    fun removeAt(index: Int): Int {
        if (index < 0 || index >= length) throwIndexOutOfBounds(index, length)
        val a = elements
        val mask = a.size - 1
        val h = head
        val removed = a[(h + index) and mask]
        if (index < length - 1 - index) {
            var i = index
            while (i > 0) {
                a[(h + i) and mask] = a[(h + i - 1) and mask]
                i--
            }
            head = (h + 1) and mask
        } else {
            var i = index
            while (i < length - 1) {
                a[(h + i) and mask] = a[(h + i + 1) and mask]
                i++
            }
        }
        length--
        modCount++
        return removed
    }

    /** Removes the first occurrence of [value], as `ArrayDeque.remove` does. */
    fun removeValue(value: Int): Boolean {
        val index = indexOf(value)
        if (index < 0) return false
        removeAt(index)
        return true
    }

    fun clear() {
        length = 0
        modCount++
    }

    /** Rounds up to a power of two. */
    fun ensureCapacity(minCapacity: Int) = ensureCapacity(minCapacity.toLong())

    private fun ensureCapacity(required: Long) {
        if (required > elements.size) grow(required)
    }

    operator fun contains(value: Int): Boolean = indexOf(value) >= 0

    fun indexOf(value: Int): Int {
        var i = 0
        while (i < length) {
            if ((elementAt(i) == value)) return i
            i++
        }
        return -1
    }

    /** Moves the element at `i` to `(i + distance) mod size`, as `Collections.rotate` does. O(min(d, size - d)). */
    fun rotate(distance: Int) {
        val n = length
        if (n == 0) return
        val right = Math.floorMod(distance, n)
        val a = elements
        val mask = a.size - 1
        var h = head
        if (right <= n - right) {
            // The last element moves to the slot before the first, which held nothing or this element itself.
            repeat(right) {
                h = (h - 1) and mask
                a[h] = a[(h + n) and mask]
            }
        } else {
            repeat(n - right) {
                a[(h + n) and mask] = a[h]
                h = (h + 1) and mask
            }
        }
        head = h
    }

    inline fun forEach(action: (Int) -> Unit) {
        val expected = modCount
        var i = 0
        while (i < length) {
            action(elementAt(i))
            if (modCount != expected) throwConcurrentModification()
            i++
        }
    }

    inline fun forEachReversed(action: (Int) -> Unit) {
        val expected = modCount
        var i = length - 1
        while (i >= 0) {
            action(elementAt(i))
            if (modCount != expected) throwConcurrentModification()
            i--
        }
    }

    /** Fails fast if the deque changes structurally. Prefer [forEach] where the first run's allocation matters. */
    operator fun iterator(): IntIterator = DequeIterator(this)

    private class DequeIterator(private val deque: IntDeque) : IntIterator() {
        private val expected = deque.modCount
        private var next = 0

        /** Checks at the end, where a clear would otherwise look like a finished loop, as `IntList` does. */
        override fun hasNext(): Boolean {
            if (next < deque.length) return true
            if (deque.modCount != expected) throwConcurrentModification()
            return false
        }

        override fun nextInt(): Int {
            if (deque.modCount != expected) throwConcurrentModification()
            if (next >= deque.length) throwEmpty()
            return deque.elementAt(next++)
        }
    }

    /** Returns a new deque. */
    operator fun plus(value: Int): IntDeque {
        val result = copyWithCapacity(length + 1L)
        result.addLast(value)
        return result
    }

    operator fun plus(other: IntDeque): IntDeque {
        val result = copyWithCapacity(length.toLong() + other.length)
        result.addAll(other)
        return result
    }

    /** Returns a new deque without the first occurrence of [value], as `minus` on `List` does. */
    operator fun minus(value: Int): IntDeque {
        val result = copy()
        result.removeValue(value)
        return result
    }

    /** Returns a new deque without any element of [other], as `minus` on `List` does. */
    operator fun minus(other: IntDeque): IntDeque {
        val result = copy()
        result -= other
        return result
    }

    operator fun minusAssign(value: Int) {
        removeValue(value)
    }

    operator fun minusAssign(other: IntDeque) {
        val excluded = other.toIntArray()
        java.util.Arrays.sort(excluded)
        val a = elements
        val mask = a.size - 1
        val h = head
        var write = 0
        var read = 0
        while (read < length) {
            val value = a[(h + read) and mask]
            if (java.util.Arrays.binarySearch(excluded, value) < 0) {
                a[(h + write) and mask] = value
                write++
            }
            read++
        }
        if (write == length) return
        length = write
        modCount++
    }

    /** Keeps the capacity, as `IntList.copy` does. */
    fun copy(): IntDeque = IntDeque(elements.copyOf(), head, length)

    /** Returns the elements from first to last, starting at index 0. */
    fun toIntArray(): IntArray {
        val result = IntArray(length)
        copyInOrder(result)
        return result
    }

    /** Appends the elements without creating a temporary String for the whole deque. */
    fun appendTo(builder: StringBuilder, separator: CharSequence = " ") {
        var i = 0
        while (i < length) {
            if (i > 0) builder.append(separator)
            builder.append(elementAt(i))
            i++
        }
    }

    fun joinToString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = ""): String {
        val builder = StringBuilder()
        builder.append(prefix)
        appendTo(builder, separator)
        builder.append(postfix)
        return builder.toString()
    }

    override fun toString(): String = joinToString(", ", "[", "]")

    /** Only another IntDeque can be equal, whatever its capacity and layout. Use `toList()` to compare with a `List`. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IntDeque || length != other.length) return false
        var i = 0
        while (i < length) {
            if (!(elementAt(i) == other.elementAt(i))) return false
            i++
        }
        return true
    }

    /** Matches `List.hashCode`. */
    override fun hashCode(): Int {
        var result = 1
        var i = 0
        while (i < length) {
            result = 31 * result + elementAt(i).hashCode()
            i++
        }
        return result
    }

    /** Copies the elements in order to the start of [target], which must hold them. */
    private fun copyInOrder(target: IntArray) {
        val firstPart = minOf(length, elements.size - head)
        System.arraycopy(elements, head, target, 0, firstPart)
        System.arraycopy(elements, 0, target, firstPart, length - firstPart)
    }

    private fun copyWithCapacity(required: Long): IntDeque {
        val result = IntDeque(IntArray(ringCapacity(required)), 0, length)
        copyInOrder(result.elements)
        return result
    }

    /** Moves the elements to the start of a larger array. */
    private fun grow(required: Long) {
        val grown = IntArray(ringCapacity(required))
        copyInOrder(grown)
        elements = grown
        head = 0
    }
}

fun intDequeOf(vararg values: Int): IntDeque {
    val result = IntDeque(values.size)
    result.addAll(values)
    return result
}
