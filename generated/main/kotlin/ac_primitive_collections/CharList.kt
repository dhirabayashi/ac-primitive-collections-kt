// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeList.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/**
 * A growable Char array that never boxes its elements.
 *
 * `a + x` copies the whole list, so a loop of `a = a + x` is O(n^2). Use `+=` in loops, and declare the
 * list with `val`, because `a += x` on a `var` is ambiguous between `plus` and `plusAssign`.
 *
 * The constructor argument is a capacity, not a size: `CharList(3)` is empty.
 */
class CharList @PublishedApi internal constructor(
    @JvmField @PublishedApi internal var elements: CharArray,
    @JvmField @PublishedApi internal var length: Int,
) {
    /** Counts structural changes. Iteration and [subList] use it to detect changes they cannot survive. */
    @JvmField
    @PublishedApi
    internal var modCount: Int = 0

    constructor(initialCapacity: Int = DEFAULT_CAPACITY) : this(CharArray(checkInitialCapacity(initialCapacity)), 0)

    val size: Int get() = length
    val capacity: Int get() = elements.size
    val lastIndex: Int get() = length - 1

    /** Written as `IntRange(0, size - 1)` because `0 until size` kept allocating in measurements. */
    val indices: IntRange get() = IntRange(0, length - 1)

    fun isEmpty(): Boolean = length == 0
    fun isNotEmpty(): Boolean = length != 0

    operator fun get(index: Int): Char {
        if (index < 0 || index >= length) throwIndexOutOfBounds(index, length)
        return elements[index]
    }

    operator fun set(index: Int, value: Char) {
        if (index < 0 || index >= length) throwIndexOutOfBounds(index, length)
        elements[index] = value
    }

    fun first(): Char {
        if (length == 0) throwEmpty()
        return elements[0]
    }

    fun last(): Char {
        if (length == 0) throwEmpty()
        return elements[length - 1]
    }

    fun firstOrDefault(default: Char): Char = if (length == 0) default else elements[0]
    fun lastOrDefault(default: Char): Char = if (length == 0) default else elements[length - 1]
    inline fun firstOrElse(default: () -> Char): Char = if (length == 0) default() else elements[0]
    inline fun lastOrElse(default: () -> Char): Char = if (length == 0) default() else elements[length - 1]

    operator fun component1(): Char = get(0)
    operator fun component2(): Char = get(1)
    operator fun component3(): Char = get(2)
    operator fun component4(): Char = get(3)
    operator fun component5(): Char = get(4)

    fun add(value: Char) {
        val n = length
        var a = elements
        if (n == a.size) a = grow(n + 1L)
        a[n] = value
        length = n + 1
        modCount++
    }

    operator fun plusAssign(value: Char) = add(value)
    operator fun plusAssign(other: CharList) = addAll(other)

    fun addAll(values: CharArray) = addAll(values, 0, values.size)

    fun addAll(values: CharArray, from: Int, to: Int) {
        checkRange(from, to, values.size)
        val count = to - from
        openGap(length, count)
        System.arraycopy(values, from, elements, length - count, count)
    }

    /** Reads [other]'s array after growing, so `a.addAll(a)` works. */
    fun addAll(other: CharList) = addAll(other, 0, other.length)

    fun addAll(other: CharList, from: Int, to: Int) {
        checkRange(from, to, other.length)
        val count = to - from
        openGap(length, count)
        System.arraycopy(other.elements, from, elements, length - count, count)
    }

    fun insert(index: Int, value: Char) {
        if (index < 0 || index > length) throwIndexOutOfBounds(index, length)
        openGap(index, 1)
        elements[index] = value
    }

    fun insertAll(index: Int, values: CharArray) {
        if (index < 0 || index > length) throwIndexOutOfBounds(index, length)
        openGap(index, values.size)
        System.arraycopy(values, 0, elements, index, values.size)
    }

    fun insertAll(index: Int, other: CharList) {
        // Opening the gap moves the elements of a self-insert, so take the copy first.
        insertAll(index, if (other === this) toCharArray() else other.elements.copyOf(other.length))
    }

    fun removeAt(index: Int): Char {
        if (index < 0 || index >= length) throwIndexOutOfBounds(index, length)
        val removed = elements[index]
        removeRange(index, index + 1)
        return removed
    }

    fun removeFirst(): Char {
        if (length == 0) throwEmpty()
        return removeAt(0)
    }

    fun removeLast(): Char {
        if (length == 0) throwEmpty()
        modCount++
        length--
        return elements[length]
    }

    /** Removes the first occurrence of [value], as `MutableList.remove` does. */
    fun removeValue(value: Char): Boolean {
        val index = indexOf(value)
        if (index < 0) return false
        removeAt(index)
        return true
    }

    fun removeRange(from: Int, to: Int) {
        checkRange(from, to, length)
        if (from == to) return
        modCount++
        System.arraycopy(elements, to, elements, from, length - to)
        length -= to - from
    }

    /** Removes in O(1) by moving the last element into [index]. Does not keep the order. */
    fun swapRemove(index: Int): Char {
        if (index < 0 || index >= length) throwIndexOutOfBounds(index, length)
        val removed = elements[index]
        modCount++
        length--
        elements[index] = elements[length]
        return removed
    }

    /**
     * Removes the elements that match [predicate] and returns whether any matched.
     *
     * If [predicate] throws or returns from the enclosing function, the elements it has not removed stay.
     */
    inline fun removeIf(predicate: (Char) -> Boolean): Boolean {
        val expected = modCount
        val n = length
        var read = 0
        var write = 0
        try {
            while (read < n) {
                val value = elements[read]
                val matches = predicate(value)
                if (modCount != expected) throwConcurrentModification()
                if (!matches) {
                    elements[write] = value
                    write++
                }
                read++
            }
        } finally {
            finishRemoveIf(expected, read, write)
        }
        return write != n
    }

    @PublishedApi
    internal fun finishRemoveIf(expected: Int, read: Int, write: Int) {
        // A structural change from the predicate is already being reported, and the indices are stale.
        if (modCount != expected || read == write) return
        modCount++
        System.arraycopy(elements, read, elements, write, length - read)
        length -= read - write
    }

    /** Keeps the first [newSize] elements. */
    fun truncate(newSize: Int) {
        if (newSize < 0 || newSize > length) throwIndexOutOfBounds(newSize, length)
        if (newSize == length) return
        modCount++
        length = newSize
    }

    fun clear() {
        modCount++
        length = 0
    }

    fun ensureCapacity(minCapacity: Int) {
        if (minCapacity > elements.size) grow(minCapacity.toLong())
    }

    fun trimToSize() {
        if (length != elements.size) elements = elements.copyOf(length)
    }

    /** Sets the size to [newSize], filling new positions with [fill]. */
    fun resize(newSize: Int, fill: Char = '\u0000') {
        if (newSize < 0) throw IllegalArgumentException("Negative size: $newSize")
        val old = length
        if (newSize <= old) {
            truncate(newSize)
            return
        }
        openGap(old, newSize - old)
        elements.fill(fill, old, newSize)
    }

    operator fun contains(value: Char): Boolean = indexOf(value) >= 0

    fun indexOf(value: Char): Int = indexOf(value, 0, length)

    fun indexOf(value: Char, from: Int, to: Int): Int {
        checkRange(from, to, length)
        val a = elements
        var i = from
        while (i < to) {
            if ((a[i] == value)) return i
            i++
        }
        return -1
    }

    fun lastIndexOf(value: Char): Int {
        val a = elements
        var i = length - 1
        while (i >= 0) {
            if ((a[i] == value)) return i
            i--
        }
        return -1
    }

    /** Requires a sorted list. Returns as `Arrays.binarySearch` does: `-(insertionPoint) - 1` when absent. */
    fun binarySearch(value: Char): Int = binarySearch(value, 0, length)

    fun binarySearch(value: Char, from: Int, to: Int): Int {
        checkRange(from, to, length)
        return java.util.Arrays.binarySearch(elements, from, to, value)
    }

    /** Requires a sorted list. Returns the first index whose element is not less than [value]. */
    fun lowerBound(value: Char): Int = lowerBound(value, 0, length)

    fun lowerBound(value: Char, from: Int, to: Int): Int {
        checkRange(from, to, length)
        val a = elements
        var lo = from
        var hi = to
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if ((a[mid] < value)) lo = mid + 1 else hi = mid
        }
        return lo
    }

    /** Requires a sorted list. Returns the first index whose element is greater than [value]. */
    fun upperBound(value: Char): Int = upperBound(value, 0, length)

    fun upperBound(value: Char, from: Int, to: Int): Int {
        checkRange(from, to, length)
        val a = elements
        var lo = from
        var hi = to
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if ((value < a[mid])) hi = mid else lo = mid + 1
        }
        return lo
    }

    inline fun indexOfFirst(predicate: (Char) -> Boolean): Int {
        forEachIndexed { index, value -> if (predicate(value)) return index }
        return -1
    }

    inline fun indexOfLast(predicate: (Char) -> Boolean): Int {
        val expected = modCount
        var i = length - 1
        while (i >= 0) {
            val matches = predicate(elements[i])
            if (modCount != expected) throwConcurrentModification()
            if (matches) return i
            i--
        }
        return -1
    }

    fun sort() = sort(0, length)

    fun sort(from: Int, to: Int) {
        checkRange(from, to, length)
        java.util.Arrays.sort(elements, from, to)
    }

    fun sortDescending() = sortDescending(0, length)

    /** Sorts ascending and reverses. Negating the elements would break on `MIN_VALUE`. */
    fun sortDescending(from: Int, to: Int) {
        sort(from, to)
        reverse(from, to)
    }

    fun reverse() = reverse(0, length)

    fun reverse(from: Int, to: Int) {
        checkRange(from, to, length)
        val a = elements
        var i = from
        var j = to - 1
        while (i < j) {
            val t = a[i]
            a[i] = a[j]
            a[j] = t
            i++
            j--
        }
    }

    fun shuffle(random: kotlin.random.Random = kotlin.random.Random.Default) = shuffle(random, 0, length)

    fun shuffle(random: kotlin.random.Random, from: Int, to: Int) {
        checkRange(from, to, length)
        val a = elements
        var i = to - 1
        while (i > from) {
            val j = random.nextInt(from, i + 1)
            val t = a[i]
            a[i] = a[j]
            a[j] = t
            i--
        }
    }

    fun swap(i: Int, j: Int) {
        if (i < 0 || i >= length) throwIndexOutOfBounds(i, length)
        if (j < 0 || j >= length) throwIndexOutOfBounds(j, length)
        val a = elements
        val t = a[i]
        a[i] = a[j]
        a[j] = t
    }

    /** Moves the element at `i` to `(i + distance) mod size`, as `Collections.rotate` does. */
    fun rotate(distance: Int) = rotate(distance, 0, length)

    fun rotate(distance: Int, from: Int, to: Int) {
        checkRange(from, to, length)
        val n = to - from
        if (n == 0) return
        val d = Math.floorMod(distance, n)
        if (d == 0) return
        reverse(from, to)
        reverse(from, from + d)
        reverse(from + d, to)
    }

    fun fill(value: Char) = fill(value, 0, length)

    fun fill(value: Char, from: Int, to: Int) {
        checkRange(from, to, length)
        elements.fill(value, from, to)
    }

    /** Removes adjacent duplicates, so a sorted list ends up with distinct elements. */
    fun dedupe() {
        val n = length
        if (n < 2) return
        val a = elements
        var write = 1
        var read = 1
        while (read < n) {
            if (!(a[read] == a[write - 1])) {
                a[write] = a[read]
                write++
            }
            read++
        }
        truncate(write)
    }

    /** Advances to the next permutation in lexicographic order. At the last one, wraps to the first and returns false. */
    fun nextPermutation(): Boolean {
        if (length < 2) return false
        val a = elements
        var i = length - 2
        while (i >= 0 && !(a[i] < a[i + 1])) i--
        if (i >= 0) {
            var j = length - 1
            while (!(a[i] < a[j])) j--
            val t = a[i]
            a[i] = a[j]
            a[j] = t
        }
        reverse(i + 1, length)
        return i >= 0
    }

    inline fun forEach(action: (Char) -> Unit) {
        val expected = modCount
        var i = 0
        while (i < length) {
            action(elements[i])
            if (modCount != expected) throwConcurrentModification()
            i++
        }
    }

    inline fun forEachIndexed(action: (index: Int, value: Char) -> Unit) {
        val expected = modCount
        var i = 0
        while (i < length) {
            action(i, elements[i])
            if (modCount != expected) throwConcurrentModification()
            i++
        }
    }

    inline fun forEachReversed(action: (Char) -> Unit) {
        val expected = modCount
        var i = length - 1
        while (i >= 0) {
            action(elements[i])
            if (modCount != expected) throwConcurrentModification()
            i--
        }
    }

    /** Fails fast if the list changes structurally. Prefer [forEach] where the first run's allocation matters. */
    operator fun iterator(): CharIterator = ListIterator(this)

    private class ListIterator(private val list: CharList) : CharIterator() {
        private val expected = list.modCount
        private var next = 0

        /** Checks here too: after a clear, the size alone would end the loop as if it had finished. */
        override fun hasNext(): Boolean {
            if (list.modCount != expected) throwConcurrentModification()
            return next < list.length
        }

        override fun nextChar(): Char {
            if (list.modCount != expected) throwConcurrentModification()
            if (next >= list.length) throwEmpty()
            return list.elements[next++]
        }
    }

    /** Returns a same-typed list. Conversions to other element types are `mapToLong` and so on. */
    inline fun map(transform: (Char) -> Char): CharList {
        val result = CharList(length)
        forEach { result.add(transform(it)) }
        return result
    }

    inline fun filter(predicate: (Char) -> Boolean): CharList {
        val result = CharList()
        forEach { if (predicate(it)) result.add(it) }
        return result
    }

    inline fun any(predicate: (Char) -> Boolean): Boolean {
        forEach { if (predicate(it)) return true }
        return false
    }

    inline fun all(predicate: (Char) -> Boolean): Boolean {
        forEach { if (!predicate(it)) return false }
        return true
    }

    inline fun none(predicate: (Char) -> Boolean): Boolean {
        forEach { if (predicate(it)) return false }
        return true
    }

    inline fun count(predicate: (Char) -> Boolean): Int {
        var result = 0
        forEach { if (predicate(it)) result++ }
        return result
    }

    /**
     * Generic like the standard one. The compiler removes the boxing once this is inlined, but only with
     * this direct loop: built on [forEach], the accumulator stays boxed. The boxing check guards this.
     */
    inline fun <R> fold(initial: R, operation: (R, Char) -> R): R {
        val expected = modCount
        var result = initial
        var i = 0
        while (i < length) {
            result = operation(result, elements[i])
            if (modCount != expected) throwConcurrentModification()
            i++
        }
        return result
    }

    /** Takes a Long selector, so an Int selector is written `sumOf { it.toLong() }`. */
    inline fun sumOf(selector: (Char) -> Long): Long {
        var result = 0L
        forEach { result += selector(it) }
        return result
    }


    fun min(): Char = min(0, length)

    fun min(from: Int, to: Int): Char = elements[indexOfMin(from, to)]

    fun max(): Char = max(0, length)

    fun max(from: Int, to: Int): Char = elements[indexOfMax(from, to)]

    fun minOrDefault(default: Char): Char = if (length == 0) default else min()
    fun maxOrDefault(default: Char): Char = if (length == 0) default else max()

    /** Returns the first index of the minimum. */
    fun indexOfMin(): Int = indexOfMin(0, length)

    fun indexOfMin(from: Int, to: Int): Int {
        checkRange(from, to, length)
        if (from == to) throwEmpty()
        val a = elements
        var best = from
        var i = from + 1
        while (i < to) {
            if ((a[i] < a[best])) best = i
            i++
        }
        return best
    }

    /** Returns the first index of the maximum. */
    fun indexOfMax(): Int = indexOfMax(0, length)

    fun indexOfMax(from: Int, to: Int): Int {
        checkRange(from, to, length)
        if (from == to) throwEmpty()
        val a = elements
        var best = from
        var i = from + 1
        while (i < to) {
            if ((a[best] < a[i])) best = i
            i++
        }
        return best
    }

    /** Returns a new list. The element behavior matches `plus` on `List`. */
    operator fun plus(value: Char): CharList {
        val result = CharList(elements.copyOf(nextCapacity(0, length + 1L)), length)
        result.add(value)
        return result
    }

    operator fun plus(other: CharList): CharList {
        val result = CharList(elements.copyOf(nextCapacity(0, length.toLong() + other.length)), length)
        result.addAll(other)
        return result
    }

    /** Returns a new list without the first occurrence of [value], as `minus` on `List` does. */
    operator fun minus(value: Char): CharList {
        val result = copy()
        result.removeValue(value)
        return result
    }

    /** Returns a new list without any element of [other], as `minus` on `List` does. */
    operator fun minus(other: CharList): CharList {
        val result = copy()
        result -= other
        return result
    }

    operator fun minusAssign(value: Char) {
        removeValue(value)
    }

    operator fun minusAssign(other: CharList) {
        val excluded = other.toCharArray()
        java.util.Arrays.sort(excluded)
        val a = elements
        var write = 0
        var read = 0
        while (read < length) {
            val value = a[read]
            if (java.util.Arrays.binarySearch(excluded, value) < 0) {
                a[write] = value
                write++
            }
            read++
        }
        truncate(write)
    }

    fun copy(): CharList = CharList(elements.copyOf(), length)

    fun slice(from: Int, to: Int): CharList {
        checkRange(from, to, length)
        return CharList(elements.copyOfRange(from, to), to - from)
    }

    fun take(n: Int): CharList {
        if (n < 0) throw IllegalArgumentException("Negative count: $n")
        return slice(0, minOf(n, length))
    }

    fun drop(n: Int): CharList {
        if (n < 0) throw IllegalArgumentException("Negative count: $n")
        return slice(minOf(n, length), length)
    }

    fun sorted(): CharList {
        val result = copy()
        result.sort()
        return result
    }

    fun sortedDescending(): CharList {
        val result = copy()
        result.sortDescending()
        return result
    }

    fun reversed(): CharList {
        val result = copy()
        result.reverse()
        return result
    }

    /** Keeps the first occurrence of each value, in the original order, in O(n log n). */
    fun distinct(): CharList {
        val sortedValues = toCharArray()
        java.util.Arrays.sort(sortedValues)
        val seen = BooleanArray(length)
        val result = CharList()
        var i = 0
        while (i < length) {
            val value = elements[i]
            val position = java.util.Arrays.binarySearch(sortedValues, value)
            // binarySearch can land on any duplicate, so walk to the first one.
            var first = position
            while (first > 0 && (sortedValues[first - 1] == value)) first--
            if (!seen[first]) {
                seen[first] = true
                result.add(value)
            }
            i++
        }
        return result
    }

    fun toCharArray(): CharArray = elements.copyOf(length)

    /** Appends the elements without creating a temporary String for the whole list. */
    fun appendTo(builder: StringBuilder, separator: CharSequence = " ") {
        var i = 0
        while (i < length) {
            if (i > 0) builder.append(separator)
            builder.append(elements[i])
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

    /** Only another CharList can be equal. Use `toList()` to compare with a `List`. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CharList || length != other.length) return false
        var i = 0
        while (i < length) {
            if (!(elements[i] == other.elements[i])) return false
            i++
        }
        return true
    }

    /** Matches `List.hashCode`. */
    override fun hashCode(): Int {
        var result = 1
        var i = 0
        while (i < length) {
            result = 31 * result + elements[i].hashCode()
            i++
        }
        return result
    }

    /** Makes room for [count] elements at [index]. The new positions hold stale values. */
    private fun openGap(index: Int, count: Int) {
        if (count == 0) return
        modCount++
        val n = length
        val required = n.toLong() + count
        if (required > elements.size) grow(required)
        System.arraycopy(elements, index, elements, index + count, n - index)
        length = n + count
    }

    private fun grow(required: Long): CharArray {
        val grown = elements.copyOf(nextCapacity(elements.size, required))
        elements = grown
        return grown
    }
}

/** Builds a list of [size] elements. Compare with the constructor, whose argument is a capacity. */
inline fun CharList(size: Int, init: (index: Int) -> Char): CharList {
    val result = CharList(checkNonNegativeSize(size))
    var i = 0
    while (i < size) {
        result.add(init(i))
        i++
    }
    return result
}

fun charListOf(vararg values: Char): CharList = CharList(values.copyOf(), values.size)

inline fun buildCharList(builder: CharList.() -> Unit): CharList {
    val result = CharList()
    result.builder()
    return result
}
