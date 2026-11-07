// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeSubList.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/**
 * A view of `[from, to)` in a [IntList]. Changes pass through in both directions.
 *
 * The view has a fixed length, except that [clear] removes its range from the parent. After another
 * structural change to the parent, using the view throws `ConcurrentModificationException`.
 * Each call allocates one object, so hot loops should use the parent's range overloads instead.
 */
fun IntList.subList(from: Int, to: Int): IntSubList {
    checkRange(from, to, length)
    return IntSubList(this, from, to - from)
}

class IntSubList @PublishedApi internal constructor(
    @JvmField @PublishedApi internal val parent: IntList,
    @JvmField @PublishedApi internal val offset: Int,
    @JvmField @PublishedApi internal var length: Int,
) {
    @JvmField
    @PublishedApi
    internal var expectedModCount: Int = parent.modCount

    @PublishedApi
    internal fun checkParent() {
        if (parent.modCount != expectedModCount) throwConcurrentModification()
    }

    val size: Int
        get() {
            checkParent()
            return length
        }

    val lastIndex: Int get() = size - 1
    val indices: IntRange get() = IntRange(0, size - 1)

    fun isEmpty(): Boolean = size == 0
    fun isNotEmpty(): Boolean = size != 0

    operator fun get(index: Int): Int {
        checkParent()
        if (index < 0 || index >= length) throwIndexOutOfBounds(index, length)
        return parent.elements[offset + index]
    }

    operator fun set(index: Int, value: Int) {
        checkParent()
        if (index < 0 || index >= length) throwIndexOutOfBounds(index, length)
        parent.elements[offset + index] = value
    }

    fun first(): Int {
        checkParent()
        if (length == 0) throwEmpty()
        return parent.elements[offset]
    }

    fun last(): Int {
        checkParent()
        if (length == 0) throwEmpty()
        return parent.elements[offset + length - 1]
    }

    operator fun contains(value: Int): Boolean = indexOf(value) >= 0

    fun indexOf(value: Int): Int {
        checkParent()
        val index = parent.indexOf(value, offset, offset + length)
        return if (index < 0) -1 else index - offset
    }

    fun binarySearch(value: Int): Int {
        checkParent()
        val found = parent.binarySearch(value, offset, offset + length)
        return if (found >= 0) found - offset else found + offset
    }

    fun lowerBound(value: Int): Int {
        checkParent()
        return parent.lowerBound(value, offset, offset + length) - offset
    }

    fun upperBound(value: Int): Int {
        checkParent()
        return parent.upperBound(value, offset, offset + length) - offset
    }

    fun sum(): Long {
        checkParent()
        return parent.sum(offset, offset + length)
    }

    fun min(): Int {
        checkParent()
        return parent.min(offset, offset + length)
    }

    fun max(): Int {
        checkParent()
        return parent.max(offset, offset + length)
    }

    fun sort() {
        checkParent()
        parent.sort(offset, offset + length)
    }

    fun sortDescending() {
        checkParent()
        parent.sortDescending(offset, offset + length)
    }

    fun sortWith(comparator: IntComparator) {
        checkParent()
        parent.sortWith(comparator, offset, offset + length)
    }

    /** Sorts stably by an Int key without a comparator. */
    inline fun sortBy(selector: (Int) -> Int) {
        val keys = LongArray(size)
        forEachIndexed { index, value -> keys[index] = packKeyAndIndex(selector(value), index) }
        applySortedIndices(sortedIndices(keys))
    }

    /** Sorts stably by a Long key without a comparator. */
    inline fun sortByLong(selector: (Int) -> Long) {
        val keys = LongArray(size)
        forEachIndexed { index, value -> keys[index] = selector(value) }
        applySortedIndices(sortedIndicesByKey(keys))
    }

    /** Sorts stably by a Double key without a comparator, in the order of `Double.compareTo`. */
    inline fun sortByDouble(selector: (Int) -> Double) = sortByLong { totalOrderKey(selector(it)) }

    @PublishedApi
    internal fun applySortedIndices(indices: IntArray) {
        val source = toIntArray()
        var i = 0
        while (i < indices.size) {
            parent.elements[offset + i] = source[indices[i]]
            i++
        }
    }

    fun reverse() {
        checkParent()
        parent.reverse(offset, offset + length)
    }

    fun shuffle(random: kotlin.random.Random = kotlin.random.Random.Default) {
        checkParent()
        parent.shuffle(random, offset, offset + length)
    }

    fun fill(value: Int) {
        checkParent()
        parent.fill(value, offset, offset + length)
    }

    fun swap(i: Int, j: Int) {
        checkParent()
        if (i < 0 || i >= length) throwIndexOutOfBounds(i, length)
        if (j < 0 || j >= length) throwIndexOutOfBounds(j, length)
        parent.swap(offset + i, offset + j)
    }

    fun rotate(distance: Int) {
        checkParent()
        parent.rotate(distance, offset, offset + length)
    }

    /** Removes the range from the parent. The view stays valid and empty. */
    fun clear() {
        checkParent()
        parent.removeRange(offset, offset + length)
        length = 0
        expectedModCount = parent.modCount
    }

    /** [clear] moves expectedModCount along with the parent, so comparing it is what catches a clear from [action]. */
    inline fun forEach(action: (Int) -> Unit) {
        val expected = expectedModCount
        var i = 0
        while (i < size) {
            action(parent.elements[offset + i])
            if (expectedModCount != expected) throwConcurrentModification()
            i++
        }
    }

    inline fun forEachIndexed(action: (index: Int, value: Int) -> Unit) {
        val expected = expectedModCount
        var i = 0
        while (i < size) {
            action(i, parent.elements[offset + i])
            if (expectedModCount != expected) throwConcurrentModification()
            i++
        }
    }

    inline fun count(predicate: (Int) -> Boolean): Int {
        var result = 0
        forEach { if (predicate(it)) result++ }
        return result
    }

    inline fun any(predicate: (Int) -> Boolean): Boolean {
        forEach { if (predicate(it)) return true }
        return false
    }

    inline fun all(predicate: (Int) -> Boolean): Boolean {
        forEach { if (!predicate(it)) return false }
        return true
    }

    operator fun iterator(): IntIterator = ViewIterator(this)

    private class ViewIterator(private val view: IntSubList) : IntIterator() {
        private val expected = view.expectedModCount
        private var next = 0

        override fun hasNext(): Boolean {
            if (view.expectedModCount != expected) throwConcurrentModification()
            return next < view.size
        }

        override fun nextInt(): Int {
            if (view.expectedModCount != expected) throwConcurrentModification()
            return view[next++]
        }
    }

    /** Nested views point at the parent directly, so they do not add a level of indirection. */
    fun subList(from: Int, to: Int): IntSubList {
        checkParent()
        checkRange(from, to, length)
        return IntSubList(parent, offset + from, to - from)
    }

    fun toIntList(): IntList {
        checkParent()
        return parent.slice(offset, offset + length)
    }

    fun toIntArray(): IntArray {
        checkParent()
        return parent.elements.copyOfRange(offset, offset + length)
    }

    @BoxingApi
    fun toList(): List<Int> = toIntArray().toList()

    fun contentEquals(other: IntList): Boolean {
        checkParent()
        if (length != other.length) return false
        var i = 0
        while (i < length) {
            if (!(parent.elements[offset + i] == other.elements[i])) return false
            i++
        }
        return true
    }

    fun contentEquals(other: IntSubList): Boolean = toIntList() == other.toIntList()

    fun joinToString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = ""): String =
        toIntList().joinToString(separator, prefix, postfix)

    override fun toString(): String = joinToString(", ", "[", "]")

    /** Only another IntSubList can be equal, so `equals` stays symmetric. */
    override fun equals(other: Any?): Boolean = other is IntSubList && contentEquals(other)

    override fun hashCode(): Int = toIntList().hashCode()
}
