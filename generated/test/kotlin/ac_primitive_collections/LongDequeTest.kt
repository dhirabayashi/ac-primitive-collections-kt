// SPDX-License-Identifier: CC0-1.0
// Generated from test-templates/KTypeDequeTest.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Hand-written expectations. The differential test cannot catch a spec misread that java.util shares. */
class LongDequeTest {
    private fun v(i: Int): Long = i.toLong()

    private fun dequeOfInts(vararg values: Int): LongDeque {
        val deque = LongDeque()
        values.forEach { deque.addLast(v(it)) }
        return deque
    }

    private fun assertDeque(deque: LongDeque, vararg expected: Int) {
        assertEquals(expected.map { v(it) }, deque.toList())
    }

    /** Capacity 8 with the first element at slot 6, so the elements wrap past the end of the array. */
    private fun wrapped(): LongDeque {
        val deque = LongDeque(8)
        repeat(6) { deque.addLast(0L) }
        repeat(6) { deque.removeFirst() }
        for (i in 1..5) deque.addLast(v(i))
        assertEquals(6, deque.head)
        return deque
    }

    @Test
    fun constructorArgumentIsACapacityRoundedUpToAPowerOfTwo() {
        assertEquals(0, LongDeque(3).size)
        assertEquals(4, LongDeque(3).capacity)
        assertEquals(0, LongDeque(0).capacity)
        assertEquals(1, LongDeque(1).capacity)
        assertEquals(DEFAULT_CAPACITY, LongDeque().capacity)
        assertFailsWith<IllegalArgumentException> { LongDeque(-1) }
        assertFailsWith<IllegalArgumentException> { LongDeque(MAX_RING_CAPACITY + 1) }
    }

    @Test
    fun factories() {
        assertDeque(longDequeOf(v(1), v(2)), 1, 2)
        assertDeque(longDequeOf())
        assertEquals(2, longDequeOf(v(1), v(2)).capacity)
        val source = LongArray(3) { v(it) }
        val fromArray = source.toLongDeque()
        source[0] = v(9)
        assertDeque(fromArray, 0, 1, 2)
        assertEquals(4, fromArray.capacity)
        assertDeque(listOf(v(3), v(4)).toLongDeque(), 3, 4)
        val appended = dequeOfInts(1)
        appended.addAll(listOf(v(2), v(3)))
        assertDeque(appended, 1, 2, 3)
    }

    @Test
    fun fromProgression() {
        assertDeque((v(1)..v(3)).toLongDeque(), 1, 2, 3)
        assertDeque((v(1)..v(5) step 2).toLongDeque(), 1, 3, 5)
        assertDeque((v(3) downTo v(1)).toLongDeque(), 3, 2, 1)
        assertDeque((v(3)..v(1)).toLongDeque())
        assertDeque((v(1) downTo v(3)).toLongDeque())
        assertDeque((v(2) downTo v(2)).toLongDeque(), 2)
        val deque = dequeOfInts(0)
        deque.addAll(v(1)..v(2))
        deque.addAll(v(5) downTo v(4))
        deque.addAll(v(2)..v(1))
        deque.addAll(v(7)..v(7))
        deque.addAll(v(1)..v(4) step 2)
        deque.addAll(v(1) downTo v(1))
        assertDeque(deque, 0, 1, 2, 5, 4, 7, 1, 3, 1)
    }

    @Test
    fun bothEndsAsQueueAndStack() {
        val deque = LongDeque(0)
        deque.addLast(v(2))
        deque.addFirst(v(1))
        deque.add(v(3))
        deque += v(4)
        assertDeque(deque, 1, 2, 3, 4)
        assertEquals(v(1), deque.removeFirst())
        assertEquals(v(4), deque.removeLast())
        assertDeque(deque, 2, 3)
        assertEquals(v(2), deque.first())
        assertEquals(v(3), deque.last())
    }

    @Test
    fun addFirstWrapsAndGrowsInOrder() {
        val deque = LongDeque(2)
        for (i in 1..9) deque.addFirst(v(i))
        assertDeque(deque, 9, 8, 7, 6, 5, 4, 3, 2, 1)
        assertEquals(16, deque.capacity)
    }

    @Test
    fun growingAWrappedDequeKeepsTheOrder() {
        val deque = wrapped()
        deque.addLast(v(6))
        deque.addLast(v(7))
        deque.addLast(v(8))
        assertEquals(8, deque.capacity)
        deque.addLast(v(9))
        assertEquals(16, deque.capacity)
        assertDeque(deque, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        val front = wrapped()
        repeat(3) { front.addFirst(v(0)) }
        front.addFirst(v(-1))
        assertDeque(front, -1, 0, 0, 0, 1, 2, 3, 4, 5)
    }

    @Test
    fun readsAndWritesOnAWrappedDeque() {
        val deque = wrapped()
        assertEquals(v(1), deque[0])
        assertEquals(v(3), deque[2])
        assertEquals(v(5), deque.last())
        deque[2] = v(30)
        assertDeque(deque, 1, 2, 30, 4, 5)
        assertContentEquals(LongArray(5) { v(listOf(1, 2, 30, 4, 5)[it]) }, deque.toLongArray())
        assertEquals(1, deque.indexOf(v(2)))
        assertEquals(3, deque.indexOf(v(4)))
        assertEquals(-1, deque.indexOf(v(3)))
        assertTrue(v(5) in deque)
        assertFalse(v(6) in deque)
        assertEquals(v(5), deque.removeLast())
        assertEquals(v(1), deque.removeFirst())
        assertDeque(deque, 2, 30, 4)
    }

    @Test
    fun emptyDequeFails() {
        val deque = LongDeque()
        assertFailsWith<NoSuchElementException> { deque.first() }
        assertFailsWith<NoSuchElementException> { deque.last() }
        assertFailsWith<NoSuchElementException> { deque.removeFirst() }
        assertFailsWith<NoSuchElementException> { deque.removeLast() }
        assertFailsWith<IndexOutOfBoundsException> { deque.removeAt(0) }
        assertEquals("[]", deque.toString())
        assertTrue(deque.isEmpty())
        assertFalse(deque.isNotEmpty())
        assertEquals(-1, deque.lastIndex)
        assertTrue(deque.indices.isEmpty())
    }

    @Test
    fun defaultsAndFallbacks() {
        val empty = LongDeque()
        assertEquals(v(7), empty.firstOrDefault(v(7)))
        assertEquals(v(7), empty.lastOrDefault(v(7)))
        assertEquals(v(8), empty.firstOrElse { v(8) })
        assertEquals(v(8), empty.lastOrElse { v(8) })
        val deque = wrapped()
        assertEquals(v(1), deque.firstOrDefault(v(7)))
        assertEquals(v(5), deque.lastOrDefault(v(7)))
        assertEquals(v(1), deque.firstOrElse { v(8) })
        assertEquals(v(5), deque.lastOrElse { v(8) })
    }

    @Test
    fun indicesAreCheckedAgainstTheSize() {
        val deque = LongDeque(8)
        deque.addLast(v(1))
        for (bad in listOf(-1, 1, 7)) {
            assertFailsWith<IndexOutOfBoundsException> { deque[bad] }
            assertFailsWith<IndexOutOfBoundsException> { deque[bad] = v(0) }
            assertFailsWith<IndexOutOfBoundsException> { deque.removeAt(bad) }
        }
        assertEquals(0..0, deque.indices)
        assertEquals(0, deque.lastIndex)
    }

    @Test
    fun removeAtShiftsTheShorterSide() {
        for (index in 0 until 5) {
            val deque = wrapped()
            val head = deque.head
            assertEquals(v(index + 1), deque.removeAt(index))
            val expected = (1..5).filter { it != index + 1 }
            assertEquals(expected.map { v(it) }, deque.toList())
            // Removing from the front half moves the head; from the back half, it stays.
            val movedHead = if (index < 2) (head + 1) and 7 else head
            assertEquals(movedHead, deque.head, "index $index")
        }
    }

    @Test
    fun removeValueRemovesTheFirstOccurrence() {
        val deque = dequeOfInts(1, 2, 1, 3)
        assertTrue(deque.removeValue(v(1)))
        assertDeque(deque, 2, 1, 3)
        assertFalse(deque.removeValue(v(9)))
        assertDeque(deque, 2, 1, 3)
        deque -= v(3)
        assertDeque(deque, 2, 1)
    }

    @Test
    fun addAllVariants() {
        val deque = wrapped()
        deque.addAll(LongArray(4) { v(it + 6) })
        assertDeque(deque, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        deque.addAll(LongArray(4) { v(it + 10) }, 1, 3)
        assertDeque(deque, 1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 12)
        assertFailsWith<IndexOutOfBoundsException> { deque.addAll(LongArray(2), 1, 3) }
        assertFailsWith<IndexOutOfBoundsException> { deque.addAll(LongArray(2), -1, 1) }
        assertFailsWith<IndexOutOfBoundsException> { deque.addAll(LongArray(2), 2, 1) }
        deque += dequeOfInts(13)
        assertEquals(12, deque.size)
        assertEquals(v(13), deque.last())
    }

    @Test
    fun addAllOfItselfDoublesItEvenWhenWrapped() {
        val deque = wrapped()
        deque.addAll(deque)
        assertDeque(deque, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5)
        val full = wrapped()
        full.addLast(v(6))
        full.addLast(v(7))
        full += full
        assertDeque(full, 1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7)
    }

    @Test
    fun addingNothingIsNotAChange() {
        val deque = dequeOfInts(1, 2)
        var visits = 0
        deque.forEach {
            deque.addAll(LongArray(0))
            deque.addAll(LongDeque())
            deque -= dequeOfInts(9)
            deque.rotate(1)
            deque.ensureCapacity(100)
            visits++
        }
        assertEquals(2, visits)
        assertEquals(128, deque.capacity)
    }

    @Test
    fun capacityControl() {
        val deque = LongDeque(2)
        deque.ensureCapacity(2)
        assertEquals(2, deque.capacity)
        deque.ensureCapacity(50)
        assertEquals(64, deque.capacity)
        deque.addLast(v(1))
        deque.clear()
        assertEquals(0, deque.size)
        assertEquals(64, deque.capacity)
        deque.addLast(v(2))
        assertDeque(deque, 2)
        assertFailsWith<IllegalStateException> { deque.ensureCapacity(MAX_RING_CAPACITY + 1) }
        // Growing from zero goes to the default capacity, not to one.
        val zero = LongDeque(0)
        zero.addFirst(v(1))
        assertEquals(DEFAULT_CAPACITY, zero.capacity)
    }

    @Test
    fun rotateMovesElementsLikeCollectionsRotate() {
        for (distance in -7..7) {
            val deque = wrapped()
            deque.rotate(distance)
            val expected = MutableList(5) { v(it + 1) }
            java.util.Collections.rotate(expected, distance)
            assertEquals(expected, deque.toList(), "distance $distance")
        }
        val full = LongDeque(4)
        for (i in 1..4) full.addLast(v(i))
        full.rotate(1)
        assertDeque(full, 4, 1, 2, 3)
        full.rotate(-2)
        assertDeque(full, 2, 3, 4, 1)
        val empty = LongDeque()
        empty.rotate(3)
        assertTrue(empty.isEmpty())
        val big = dequeOfInts(1, 2, 3)
        big.rotate(Int.MIN_VALUE)
        assertDeque(big, 3, 1, 2)
    }

    @Test
    fun iterationVisitsInOrder() {
        val deque = wrapped()
        val forward = ArrayList<Long>()
        deque.forEach { forward.add(it) }
        val reversed = ArrayList<Long>()
        deque.forEachReversed { reversed.add(it) }
        val iterated = ArrayList<Long>()
        for (x in deque) iterated.add(x)
        val expected = (1..5).map { v(it) }
        assertEquals(expected, forward)
        assertEquals(expected.reversed(), reversed)
        assertEquals(expected, iterated)
        val indexed = ArrayList<Long>()
        for (i in deque.indices) indexed.add(deque[i])
        assertEquals(expected, indexed)
    }

    @Test
    fun iterationDetectsStructuralChanges() {
        val changes = listOf<(LongDeque) -> Unit>(
            { it.addFirst(v(9)) },
            { it.addLast(v(9)) },
            { it.addAll(LongArray(1)) },
            { it.addAll(it) },
            { it.removeFirst() },
            { it.removeLast() },
            { it.removeAt(1) },
            { it.removeValue(v(3)) },
            { it -= dequeOfInts(3) },
            { it.clear() },
        )
        for (change in changes) {
            for (loop in 0 until 3) {
                val deque = wrapped()
                var visits = 0
                assertFailsWith<ConcurrentModificationException> {
                    val action = { _: Long ->
                        visits++
                        if (visits == 2) change(deque)
                    }
                    when (loop) {
                        0 -> deque.forEach(action)
                        1 -> deque.forEachReversed(action)
                        else -> for (x in deque) action(x)
                    }
                }
                assertEquals(2, visits)
            }
        }
    }

    @Test
    fun iteratorDetectsAClearOnTheLastElement() {
        val deque = dequeOfInts(1)
        assertFailsWith<ConcurrentModificationException> {
            for (x in deque) deque.clear()
        }
        val iterator = dequeOfInts(1).iterator()
        iterator.next()
        assertFalse(iterator.hasNext())
        assertFailsWith<NoSuchElementException> { iterator.next() }
    }

    @Test
    fun setDuringIterationIsAllowedAndSeen() {
        val deque = dequeOfInts(1, 2, 3)
        val seen = ArrayList<Long>()
        deque.forEach {
            if (seen.isEmpty()) deque[2] = v(30)
            seen.add(it)
        }
        assertEquals(listOf(v(1), v(2), v(30)), seen)
    }

    @Test
    fun operatorsReturnNewDeques() {
        val a = dequeOfInts(1, 2, 1)
        val b = dequeOfInts(1, 3)
        assertDeque(a + v(4), 1, 2, 1, 4)
        assertDeque(a + b, 1, 2, 1, 1, 3)
        assertDeque(a - v(1), 2, 1)
        assertDeque(a - b, 2)
        assertDeque(a - v(9), 1, 2, 1)
        assertDeque(a, 1, 2, 1)
        assertDeque(b, 1, 3)
        assertDeque(LongDeque(0) + LongDeque(0))
        val wrappedSum = wrapped() + wrapped()
        assertDeque(wrappedSum, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5)
        assertEquals(16, wrappedSum.capacity)
        assertEquals(DEFAULT_CAPACITY, (LongDeque(0) + v(1)).capacity)
    }

    @Test
    fun minusAssignRemovesEveryOccurrence() {
        val deque = wrapped()
        deque.addLast(v(2))
        deque -= dequeOfInts(2, 4, 9)
        assertDeque(deque, 1, 3, 5)
        deque -= deque
        assertDeque(deque)
    }

    @Test
    fun copyIsIndependentAndKeepsTheCapacity() {
        val deque = wrapped()
        val copy = deque.copy()
        copy.addFirst(v(0))
        copy[1] = v(10)
        assertDeque(deque, 1, 2, 3, 4, 5)
        assertDeque(copy, 0, 10, 2, 3, 4, 5)
        assertEquals(deque.capacity, deque.copy().capacity)
    }

    @Test
    fun equalityIgnoresLayoutAndCapacity() {
        val a = wrapped()
        val b = dequeOfInts(1, 2, 3, 4, 5)
        b.ensureCapacity(100)
        assertEquals(a, a)
        assertEquals(a, b)
        assertEquals(b, a)
        assertEquals(a.hashCode(), b.hashCode())
        assertEquals(listOf(v(1), v(2), v(3), v(4), v(5)).hashCode(), a.hashCode())
        assertNotEquals(a, dequeOfInts(1, 2, 3, 4))
        assertNotEquals(a, dequeOfInts(1, 2, 3, 4, 6))
        assertFalse(a.equals(null))
        assertFalse(a.equals(a.toList()))
        assertEquals(1, LongDeque().hashCode())
    }

    @Test
    fun text() {
        val deque = wrapped()
        assertEquals(deque.toList().toString(), deque.toString())
        assertEquals(deque.toList().joinToString(""), deque.joinToString(""))
        assertEquals("<" + deque.toList().joinToString("-") + ">", deque.joinToString("-", "<", ">"))
        val builder = StringBuilder("x")
        deque.appendTo(builder)
        assertEquals("x" + deque.toList().joinToString(" "), builder.toString())
        val single = StringBuilder()
        dequeOfInts(7).appendTo(single, ",")
        assertEquals(v(7).toString(), single.toString())
    }

    @Test
    fun toMutableListIsACopy() {
        val deque = dequeOfInts(1, 2)
        val list = deque.toMutableList()
        list.add(v(3))
        assertDeque(deque, 1, 2)
    }

}
