// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

/**
 * A view of `[from, to)` in a [KTypeList]. Changes pass through in both directions.
 *
 * The view has a fixed length, except that [clear] removes its range from the parent. After another
 * structural change to the parent, using the view throws `ConcurrentModificationException`.
 * Each call allocates one object, so hot loops should use the parent's range overloads instead.
 */
fun KTypeList.subList(from: Int, to: Int): KTypeSubList {
    checkRange(from, to, length)
    return KTypeSubList(this, from, to - from)
}

class KTypeSubList @PublishedApi internal constructor(
    @JvmField @PublishedApi internal val parent: KTypeList,
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

    operator fun get(index: Int): KType {
        checkParent()
        if (index < 0 || index >= length) throwIndexOutOfBounds(index, length)
        return parent.elements[offset + index]
    }

    operator fun set(index: Int, value: KType) {
        checkParent()
        if (index < 0 || index >= length) throwIndexOutOfBounds(index, length)
        parent.elements[offset + index] = value
    }

    fun first(): KType {
        checkParent()
        if (length == 0) throwEmpty()
        return parent.elements[offset]
    }

    fun last(): KType {
        checkParent()
        if (length == 0) throwEmpty()
        return parent.elements[offset + length - 1]
    }

    operator fun contains(value: KType): Boolean = indexOf(value) >= 0

    fun indexOf(value: KType): Int {
        checkParent()
        val index = parent.indexOf(value, offset, offset + length)
        return if (index < 0) -1 else index - offset
    }

    fun binarySearch(value: KType): Int {
        checkParent()
        val found = parent.binarySearch(value, offset, offset + length)
        return if (found >= 0) found - offset else found + offset
    }

    fun lowerBound(value: KType): Int {
        checkParent()
        return parent.lowerBound(value, offset, offset + length) - offset
    }

    fun upperBound(value: KType): Int {
        checkParent()
        return parent.upperBound(value, offset, offset + length) - offset
    }

    //#if K_NUMERIC
    fun sum(): K_SUM_TYPE {
        checkParent()
        return parent.sum(offset, offset + length)
    }
    //#endif

    fun min(): KType {
        checkParent()
        return parent.min(offset, offset + length)
    }

    fun max(): KType {
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

    fun sortWith(comparator: KTypeComparator) {
        checkParent()
        parent.sortWith(comparator, offset, offset + length)
    }

    /** Sorts stably by an Int key without a comparator. */
    inline fun sortBy(selector: (KType) -> Int) {
        val keys = LongArray(size)
        forEachIndexed { index, value -> keys[index] = packKeyAndIndex(selector(value), index) }
        applySortedIndices(sortedIndices(keys))
    }

    /** Sorts stably by a Long key without a comparator. */
    inline fun sortByLong(selector: (KType) -> Long) {
        val keys = LongArray(size)
        forEachIndexed { index, value -> keys[index] = selector(value) }
        applySortedIndices(sortedIndicesByKey(keys))
    }

    /** Sorts stably by a Double key without a comparator, in the order of `Double.compareTo`. */
    inline fun sortByDouble(selector: (KType) -> Double) {
        val keys = LongArray(size)
        forEachIndexed { index, value -> keys[index] = totalOrderKey(selector(value)) }
        applySortedIndices(sortedIndicesByKey(keys))
    }

    @PublishedApi
    internal fun applySortedIndices(indices: IntArray) {
        val source = toKTypeArray()
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

    fun fill(value: KType) {
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

    inline fun forEach(action: (KType) -> Unit) {
        var i = 0
        while (i < size) {
            action(parent.elements[offset + i])
            i++
        }
    }

    inline fun forEachIndexed(action: (index: Int, value: KType) -> Unit) {
        var i = 0
        while (i < size) {
            action(i, parent.elements[offset + i])
            i++
        }
    }

    inline fun count(predicate: (KType) -> Boolean): Int {
        var result = 0
        forEach { if (predicate(it)) result++ }
        return result
    }

    inline fun any(predicate: (KType) -> Boolean): Boolean {
        forEach { if (predicate(it)) return true }
        return false
    }

    inline fun all(predicate: (KType) -> Boolean): Boolean {
        forEach { if (!predicate(it)) return false }
        return true
    }

    operator fun iterator(): KTypeIterator = ViewIterator(this)

    private class ViewIterator(private val view: KTypeSubList) : KTypeIterator() {
        private var next = 0

        override fun hasNext(): Boolean = next < view.size

        override fun nextKType(): KType = view[next++]
    }

    /** Nested views point at the parent directly, so they do not add a level of indirection. */
    fun subList(from: Int, to: Int): KTypeSubList {
        checkParent()
        checkRange(from, to, length)
        return KTypeSubList(parent, offset + from, to - from)
    }

    fun toKTypeList(): KTypeList {
        checkParent()
        return parent.slice(offset, offset + length)
    }

    fun toKTypeArray(): KTypeArray {
        checkParent()
        return parent.elements.copyOfRange(offset, offset + length)
    }

    @BoxingApi
    fun toList(): List<KType> = toKTypeArray().toList()

    fun contentEquals(other: KTypeList): Boolean {
        checkParent()
        if (length != other.length) return false
        var i = 0
        while (i < length) {
            if (!K_EQ(parent.elements[offset + i], other.elements[i])) return false
            i++
        }
        return true
    }

    fun contentEquals(other: KTypeSubList): Boolean = toKTypeList() == other.toKTypeList()

    fun joinToString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = ""): String =
        toKTypeList().joinToString(separator, prefix, postfix)

    override fun toString(): String = joinToString(", ", "[", "]")

    /** Only another KTypeSubList can be equal, so `equals` stays symmetric. */
    override fun equals(other: Any?): Boolean = other is KTypeSubList && contentEquals(other)

    override fun hashCode(): Int = toKTypeList().hashCode()
}
