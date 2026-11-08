// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeDeque.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/**
 * A double-ended queue of Long that never boxes its elements. Replaces `ArrayDeque<Long>` as a queue or a stack.
 *
 * Adding or removing at either end is amortized O(1), and `d[i]` counts from the first element.
 *
 * `d + x` copies the whole deque, so a loop of `d = d + x` is O(n^2). Use `+=` in loops, and declare the
 * deque with `val`, because `d += x` on a `var` is ambiguous between `plus` and `plusAssign`.
 *
 * The constructor argument is a capacity, not a size: `LongDeque(3)` is empty. The capacity is a power of two.
 */
class LongDeque private constructor(
    @JvmField @PublishedApi internal var elements: LongArray,
    /** Where the first element lives. Element `i` lives at `(head + i) and (elements.size - 1)`. */
    @JvmField @PublishedApi internal var head: Int,
    @JvmField @PublishedApi internal var length: Int,
) {
    /** Counts structural changes, so that iteration can fail instead of skipping or repeating elements. */
    @JvmField
    @PublishedApi
    internal var modCount: Int = 0

    constructor(initialCapacity: Int = DEFAULT_CAPACITY) : this(LongArray(checkRingInitialCapacity(initialCapacity)), 0, 0)

    val size: Int get() = length
    val capacity: Int get() = elements.size
    val lastIndex: Int get() = length - 1

    /** Written as `IntRange(0, size - 1)` for the same reason as `LongList.indices`. */
    val indices: IntRange get() = IntRange(0, length - 1)

    fun isEmpty(): Boolean = length == 0
    fun isNotEmpty(): Boolean = length != 0

    @PublishedApi
    internal fun elementAt(index: Int): Long = elements[(head + index) and (elements.size - 1)]

    operator fun get(index: Int): Long {
        if (index < 0 || index >= length) throwIndexOutOfBounds(index, length)
        return elementAt(index)
    }

    operator fun set(index: Int, value: Long) {
        if (index < 0 || index >= length) throwIndexOutOfBounds(index, length)
        elements[(head + index) and (elements.size - 1)] = value
    }

    fun first(): Long {
        if (length == 0) throwEmpty()
        return elements[head]
    }

    fun last(): Long {
        if (length == 0) throwEmpty()
        return elementAt(length - 1)
    }

    fun firstOrDefault(default: Long): Long = if (length == 0) default else elements[head]
    fun lastOrDefault(default: Long): Long = if (length == 0) default else elementAt(length - 1)
    inline fun firstOrElse(default: () -> Long): Long = if (length == 0) default() else elements[head]
    inline fun lastOrElse(default: () -> Long): Long = if (length == 0) default() else elementAt(length - 1)

    fun addFirst(value: Long) {
        val n = length
        if (n == elements.size) grow(n + 1L)
        val h = (head - 1) and (elements.size - 1)
        elements[h] = value
        head = h
        length = n + 1
        modCount++
    }

    fun addLast(value: Long) {
        val n = length
        if (n == elements.size) grow(n + 1L)
        elements[(head + n) and (elements.size - 1)] = value
        length = n + 1
        modCount++
    }

    /** The same as [addLast], as in `ArrayDeque`. */
    fun add(value: Long) = addLast(value)

    operator fun plusAssign(value: Long) = addLast(value)
    operator fun plusAssign(other: LongDeque) = addAll(other)

    fun addAll(values: LongArray) = addAll(values, 0, values.size)

    fun addAll(values: LongArray, from: Int, to: Int) {
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
    fun addAll(other: LongDeque) {
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

    fun removeFirst(): Long {
        if (length == 0) throwEmpty()
        val value = elements[head]
        head = (head + 1) and (elements.size - 1)
        length--
        modCount++
        return value
    }

    fun removeLast(): Long {
        if (length == 0) throwEmpty()
        val value = elementAt(length - 1)
        length--
        modCount++
        return value
    }

    /** Moves the elements on the shorter side of [index], so removing near either end is fast. */
    fun removeAt(index: Int): Long {
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
    fun removeValue(value: Long): Boolean {
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

    operator fun contains(value: Long): Boolean = indexOf(value) >= 0

    fun indexOf(value: Long): Int {
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

    inline fun forEach(action: (Long) -> Unit) {
        val expected = modCount
        var i = 0
        while (i < length) {
            action(elementAt(i))
            if (modCount != expected) throwConcurrentModification()
            i++
        }
    }

    inline fun forEachReversed(action: (Long) -> Unit) {
        val expected = modCount
        var i = length - 1
        while (i >= 0) {
            action(elementAt(i))
            if (modCount != expected) throwConcurrentModification()
            i--
        }
    }

    /** Fails fast if the deque changes structurally. Prefer [forEach] where the first run's allocation matters. */
    operator fun iterator(): LongIterator = DequeIterator(this)

    private class DequeIterator(private val deque: LongDeque) : LongIterator() {
        private val expected = deque.modCount
        private var next = 0

        /** Checks at the end, where a clear would otherwise look like a finished loop, as `LongList` does. */
        override fun hasNext(): Boolean {
            if (next < deque.length) return true
            if (deque.modCount != expected) throwConcurrentModification()
            return false
        }

        override fun nextLong(): Long {
            if (deque.modCount != expected) throwConcurrentModification()
            if (next >= deque.length) throwEmpty()
            return deque.elementAt(next++)
        }
    }

    /** Returns a new deque. */
    operator fun plus(value: Long): LongDeque {
        val result = copyWithCapacity(length + 1L)
        result.addLast(value)
        return result
    }

    operator fun plus(other: LongDeque): LongDeque {
        val result = copyWithCapacity(length.toLong() + other.length)
        result.addAll(other)
        return result
    }

    /** Returns a new deque without the first occurrence of [value], as `minus` on `List` does. */
    operator fun minus(value: Long): LongDeque {
        val result = copy()
        result.removeValue(value)
        return result
    }

    /** Returns a new deque without any element of [other], as `minus` on `List` does. */
    operator fun minus(other: LongDeque): LongDeque {
        val result = copy()
        result -= other
        return result
    }

    operator fun minusAssign(value: Long) {
        removeValue(value)
    }

    operator fun minusAssign(other: LongDeque) {
        val excluded = other.toLongArray()
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

    /** Keeps the capacity, as `LongList.copy` does. */
    fun copy(): LongDeque = LongDeque(elements.copyOf(), head, length)

    /** Returns the elements from first to last, starting at index 0. */
    fun toLongArray(): LongArray {
        val result = LongArray(length)
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

    /** Only another LongDeque can be equal, whatever its capacity and layout. Use `toList()` to compare with a `List`. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LongDeque || length != other.length) return false
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
    private fun copyInOrder(target: LongArray) {
        val firstPart = minOf(length, elements.size - head)
        System.arraycopy(elements, head, target, 0, firstPart)
        System.arraycopy(elements, 0, target, firstPart, length - firstPart)
    }

    private fun copyWithCapacity(required: Long): LongDeque {
        val result = LongDeque(LongArray(ringCapacity(required)), 0, length)
        copyInOrder(result.elements)
        return result
    }

    /** Moves the elements to the start of a larger array. */
    private fun grow(required: Long) {
        val grown = LongArray(ringCapacity(required))
        copyInOrder(grown)
        elements = grown
        head = 0
    }
}

fun longDequeOf(vararg values: Long): LongDeque {
    val result = LongDeque(values.size)
    result.addAll(values)
    return result
}
