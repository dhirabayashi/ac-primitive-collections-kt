// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeSubList.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/**
 * A view of `[from, to)` in a [LongList]. Changes pass through in both directions.
 *
 * The view has a fixed length, except that [clear] removes its range from the parent. After another
 * structural change to the parent, using the view throws `ConcurrentModificationException`.
 * Each call allocates one object, so hot loops should use the parent's range overloads instead.
 */
fun LongList.subList(from: Int, to: Int): LongSubList {
    checkRange(from, to, length)
    return LongSubList(this, from, to - from)
}

class LongSubList @PublishedApi internal constructor(
    @JvmField @PublishedApi internal val parent: LongList,
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

    operator fun get(index: Int): Long {
        checkParent()
        if (index < 0 || index >= length) throwIndexOutOfBounds(index, length)
        return parent.elements[offset + index]
    }

    operator fun set(index: Int, value: Long) {
        checkParent()
        if (index < 0 || index >= length) throwIndexOutOfBounds(index, length)
        parent.elements[offset + index] = value
    }

    fun first(): Long {
        checkParent()
        if (length == 0) throwEmpty()
        return parent.elements[offset]
    }

    fun last(): Long {
        checkParent()
        if (length == 0) throwEmpty()
        return parent.elements[offset + length - 1]
    }

    operator fun contains(value: Long): Boolean = indexOf(value) >= 0

    fun indexOf(value: Long): Int {
        checkParent()
        val index = parent.indexOf(value, offset, offset + length)
        return if (index < 0) -1 else index - offset
    }

    fun binarySearch(value: Long): Int {
        checkParent()
        val found = parent.binarySearch(value, offset, offset + length)
        return if (found >= 0) found - offset else found + offset
    }

    fun lowerBound(value: Long): Int {
        checkParent()
        return parent.lowerBound(value, offset, offset + length) - offset
    }

    fun upperBound(value: Long): Int {
        checkParent()
        return parent.upperBound(value, offset, offset + length) - offset
    }

    fun sum(): Long {
        checkParent()
        return parent.sum(offset, offset + length)
    }

    fun min(): Long {
        checkParent()
        return parent.min(offset, offset + length)
    }

    fun max(): Long {
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

    fun sortWith(comparator: LongComparator) {
        checkParent()
        parent.sortWith(comparator, offset, offset + length)
    }

    /** Sorts stably by an Int key without a comparator. */
    inline fun sortBy(selector: (Long) -> Int) {
        val keys = LongArray(size)
        forEachIndexed { index, value -> keys[index] = packKeyAndIndex(selector(value), index) }
        applySortedIndices(sortedIndices(keys))
    }

    /** Sorts stably by a Long key without a comparator. */
    inline fun sortByLong(selector: (Long) -> Long) {
        val keys = LongArray(size)
        forEachIndexed { index, value -> keys[index] = selector(value) }
        applySortedIndices(sortedIndicesByKey(keys))
    }

    /** Sorts stably by a Double key without a comparator, in the order of `Double.compareTo`. */
    inline fun sortByDouble(selector: (Long) -> Double) = sortByLong { totalOrderKey(selector(it)) }

    @PublishedApi
    internal fun applySortedIndices(indices: IntArray) {
        val source = toLongArray()
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

    fun fill(value: Long) {
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
    inline fun forEach(action: (Long) -> Unit) {
        val expected = expectedModCount
        var i = 0
        while (i < size) {
            action(parent.elements[offset + i])
            if (expectedModCount != expected) throwConcurrentModification()
            i++
        }
    }

    inline fun forEachIndexed(action: (index: Int, value: Long) -> Unit) {
        val expected = expectedModCount
        var i = 0
        while (i < size) {
            action(i, parent.elements[offset + i])
            if (expectedModCount != expected) throwConcurrentModification()
            i++
        }
    }

    inline fun count(predicate: (Long) -> Boolean): Int {
        var result = 0
        forEach { if (predicate(it)) result++ }
        return result
    }

    inline fun any(predicate: (Long) -> Boolean): Boolean {
        forEach { if (predicate(it)) return true }
        return false
    }

    inline fun all(predicate: (Long) -> Boolean): Boolean {
        forEach { if (!predicate(it)) return false }
        return true
    }

    operator fun iterator(): LongIterator = ViewIterator(this)

    private class ViewIterator(private val view: LongSubList) : LongIterator() {
        private val expected = view.expectedModCount
        private var next = 0

        override fun hasNext(): Boolean {
            if (view.expectedModCount != expected) throwConcurrentModification()
            return next < view.size
        }

        override fun nextLong(): Long {
            if (view.expectedModCount != expected) throwConcurrentModification()
            return view[next++]
        }
    }

    /** Nested views point at the parent directly, so they do not add a level of indirection. */
    fun subList(from: Int, to: Int): LongSubList {
        checkParent()
        checkRange(from, to, length)
        return LongSubList(parent, offset + from, to - from)
    }

    fun toLongList(): LongList {
        checkParent()
        return parent.slice(offset, offset + length)
    }

    fun toLongArray(): LongArray {
        checkParent()
        return parent.elements.copyOfRange(offset, offset + length)
    }

    @BoxingApi
    fun toList(): List<Long> = toLongArray().toList()

    fun contentEquals(other: LongList): Boolean {
        checkParent()
        if (length != other.length) return false
        var i = 0
        while (i < length) {
            if (!(parent.elements[offset + i] == other.elements[i])) return false
            i++
        }
        return true
    }

    fun contentEquals(other: LongSubList): Boolean = toLongList() == other.toLongList()

    fun joinToString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = ""): String =
        toLongList().joinToString(separator, prefix, postfix)

    override fun toString(): String = joinToString(", ", "[", "]")

    /** Only another LongSubList can be equal, so `equals` stays symmetric. */
    override fun equals(other: Any?): Boolean = other is LongSubList && contentEquals(other)

    override fun hashCode(): Int = toLongList().hashCode()
}
