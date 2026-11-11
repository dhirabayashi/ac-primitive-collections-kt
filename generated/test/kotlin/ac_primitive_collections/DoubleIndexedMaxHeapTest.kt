// SPDX-License-Identifier: CC0-1.0
// Generated from test-templates/KTypeIndexedMinHeapTest.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DoubleIndexedMaxHeapTest {
    private data class Op(val kind: Kind, val index: Int, val key: Double)

    private enum class Kind(val group: Int) {
        SET(0), DECREASE_KEY(0), INCREASE_KEY(0),
        REMOVE(1), REMOVE_FIRST(1),
        COPY_THEN_SWAP(2), CLEAR(2),
    }

    private fun randomOps(seed: Long, count: Int, distribution: Distribution, domain: ValueDomain): List<Op> {
        val random = Random(seed)
        val groups = Kind.entries.groupBy { it.group }
        return List(count) { step ->
            val group = groups.getValue(random.nextWeighted(distribution))
            val key: Double = when (domain) {
                ValueDomain.SMALL -> random.nextInt(5).toDouble()
                ValueDomain.FULL -> Double.fromBits(random.nextLong())
                ValueDomain.BOUNDARY -> BOUNDARY_KEYS[random.nextInt(BOUNDARY_KEYS.size)]
                ValueDomain.MONOTONIC -> step.toDouble()
            }
            Op(group[random.nextInt(group.size)], random.nextInt(INDEX_COUNT), key)
        }
    }

    /** The model maps each present index to its key. The first entry must hold the key that sorts first. */
    private fun check(ops: List<Op>) {
        var heap = DoubleIndexedMaxHeap(INDEX_COUNT)
        val model = HashMap<Int, Double>()
        fun expectedFirstKey() = model.values.minWith(ORDER)
        fun checkFirst() {
            assertEquals(expectedFirstKey(), heap.firstKey())
            assertEquals(0, ORDER.compare(model.getValue(heap.firstIndex()), heap.firstKey()))
        }
        for (op in ops) {
            val current = model[op.index]
            when (op.kind) {
                Kind.SET -> {
                    heap[op.index] = op.key
                    model[op.index] = op.key
                }
                Kind.DECREASE_KEY -> when {
                    current == null -> assertFailsWith<NoSuchElementException> { heap.decreaseKey(op.index, op.key) }
                    NATURAL.compare(op.key, current) > 0 -> assertFailsWith<IllegalArgumentException> { heap.decreaseKey(op.index, op.key) }
                    else -> {
                        heap.decreaseKey(op.index, op.key)
                        model[op.index] = op.key
                    }
                }
                Kind.INCREASE_KEY -> when {
                    current == null -> assertFailsWith<NoSuchElementException> { heap.increaseKey(op.index, op.key) }
                    NATURAL.compare(op.key, current) < 0 -> assertFailsWith<IllegalArgumentException> { heap.increaseKey(op.index, op.key) }
                    else -> {
                        heap.increaseKey(op.index, op.key)
                        model[op.index] = op.key
                    }
                }
                Kind.REMOVE -> assertEquals(model.remove(op.index) != null, heap.remove(op.index))
                Kind.REMOVE_FIRST -> if (model.isNotEmpty()) {
                    checkFirst()
                    assertEquals(heap.firstIndex(), heap.removeFirst().also { model.remove(it) })
                }
                Kind.COPY_THEN_SWAP -> {
                    val copy = heap.copy()
                    assertEquals(heap, copy)
                    heap = copy
                }
                Kind.CLEAR -> if (op.index < 2) {
                    heap.clear()
                    model.clear()
                }
            }
            assertEquals(model.size, heap.size)
            assertEquals(model.isEmpty(), heap.isEmpty())
            assertEquals(model.containsKey(op.index), op.index in heap)
            model[op.index]?.let { assertEquals(it, heap.keyOf(op.index)) }
            if (model.isNotEmpty()) checkFirst()
        }
        val rest = HashMap<Int, Double>()
        heap.forEachUnordered { index, key -> assertEquals(null, rest.put(index, key)) }
        assertEquals(model, rest)
        while (model.isNotEmpty()) {
            checkFirst()
            model.remove(heap.removeFirst())
        }
        assertTrue(heap.isEmpty())
        for (index in 0 until INDEX_COUNT) assertFalse(index in heap)
    }

    @Test
    fun matchesAModelUnderRandomOperations() {
        for (seed in TestConfig.seeds) {
            for (distribution in Distribution.entries) {
                for (domain in ValueDomain.entries) {
                    val ops = randomOps(seed, minOf(TestConfig.operations / 8, 3000), distribution, domain)
                    OperationRunner.run("DoubleIndexedMaxHeap $distribution $domain", seed, ops, ::check)
                }
            }
        }
    }

    @Test
    fun sortsHeapsOfEverySize() {
        // Sizes around the arity's powers exercise a last parent with one to four children.
        for (n in (0..30).toList() + listOf(84, 85, 86, 87, 341, 342, 343, 344, 345, 1000)) {
            val random = Random(n)
            val keys = List(n) { Double.fromBits(random.nextLong()) }
            val heap = DoubleIndexedMaxHeap(n)
            keys.forEachIndexed { i, k -> heap[i] = k }
            val out = ArrayList<Double>()
            while (heap.isNotEmpty()) {
                val key = heap.firstKey()
                assertEquals(key, keys[heap.removeFirst()])
                out += key
            }
            assertEquals(keys.sortedWith(ORDER), out)
        }
    }

    @Test
    fun changingKeysMovesEntriesBothWays() {
        val heap = DoubleIndexedMaxHeap(10)
        for (i in 0 until 10) heap[i] = rank(i * 10)
        // Toward the front, from deep in the heap.
        heap[9] = rank(-1)
        assertEquals(9, heap.firstIndex())
        // Toward the back, from the front.
        heap[9] = rank(100)
        assertEquals(0, heap.firstIndex())
        heap[0] = rank(55)
        assertEquals(1, heap.firstIndex())
        assertEquals(listOf(1, 2, 3, 4, 5, 0, 6, 7, 8, 9), List(10) { heap.removeFirst() })
    }

    @Test
    fun decreaseAndIncreaseCompareKeysNotPriority() {
        val heap = DoubleIndexedMaxHeap(3)
        heap[0] = 5.toDouble()
        heap[1] = 6.toDouble()
        heap.decreaseKey(0, 5.toDouble())
        heap.decreaseKey(0, 3.toDouble())
        heap.increaseKey(1, 6.toDouble())
        heap.increaseKey(1, 9.toDouble())
        assertEquals(3.toDouble(), heap.keyOf(0))
        assertEquals(9.toDouble(), heap.keyOf(1))
        assertFailsWith<IllegalArgumentException> { heap.decreaseKey(0, 4.toDouble()) }
        assertFailsWith<IllegalArgumentException> { heap.increaseKey(1, 8.toDouble()) }
        assertFailsWith<NoSuchElementException> { heap.decreaseKey(2, 0.toDouble()) }
        assertFailsWith<NoSuchElementException> { heap.increaseKey(2, 0.toDouble()) }
        assertEquals(1, heap.firstIndex())
    }

    @Test
    fun removingAnyEntryKeepsTheOrder() {
        // Removing from the middle moves the last entry into the hole, and it may belong above or below.
        for (n in 1..40) {
            for (victim in 0 until n) {
                val heap = DoubleIndexedMaxHeap(n)
                for (i in 0 until n) heap[i] = rank((i * 7) % n)
                assertTrue(heap.remove(victim))
                assertFalse(heap.remove(victim))
                assertFalse(victim in heap)
                val expected = (0 until n).filter { it != victim }.map { rank((it * 7) % n) }.sortedWith(ORDER)
                assertEquals(expected, List(n - 1) { heap.keyOf(heap.firstIndex()).also { heap.removeFirst() } })
            }
        }
    }

    @Test
    fun emptyAndAbsentFail() {
        val heap = DoubleIndexedMaxHeap(3)
        assertFailsWith<NoSuchElementException> { heap.firstIndex() }
        assertFailsWith<NoSuchElementException> { heap.firstKey() }
        assertFailsWith<NoSuchElementException> { heap.removeFirst() }
        assertFailsWith<NoSuchElementException> { heap.keyOf(1) }
        assertFalse(heap.remove(1))
        assertTrue(heap.isEmpty())
        assertFalse(heap.isNotEmpty())
        heap[1] = rank(1)
        assertTrue(heap.isNotEmpty())
        assertEquals("{1=${rank(1)}}", heap.toString())
    }

    @Test
    fun indicesOutsideTheRangeFail() {
        assertFailsWith<IllegalArgumentException> { DoubleIndexedMaxHeap(-1) }
        val heap = DoubleIndexedMaxHeap(3)
        assertEquals(3, heap.indexCount)
        for (index in listOf(-1, 3, Int.MIN_VALUE, Int.MAX_VALUE)) {
            // The check names the index. Without it, the array access would throw a less clear subclass.
            assertEquals(IndexOutOfBoundsException::class, assertFailsWith<IndexOutOfBoundsException> { index in heap }::class)
            assertFailsWith<IndexOutOfBoundsException> { heap[index] = 0.0 }
            assertFailsWith<IndexOutOfBoundsException> { heap.keyOf(index) }
            assertFailsWith<IndexOutOfBoundsException> { heap.remove(index) }
            assertFailsWith<IndexOutOfBoundsException> { heap.decreaseKey(index, 0.0) }
            assertFailsWith<IndexOutOfBoundsException> { heap.increaseKey(index, 0.0) }
        }
        val none = DoubleIndexedMaxHeap(0)
        assertEquals(0, none.indexCount)
        assertFailsWith<IndexOutOfBoundsException> { none[0] = 0.0 }
    }

    @Test
    fun clearForgetsEveryIndex() {
        val heap = DoubleIndexedMaxHeap(5)
        heap[1] = rank(1)
        heap[3] = rank(3)
        heap.clear()
        assertTrue(heap.isEmpty())
        for (i in 0 until 5) assertFalse(i in heap)
        heap[3] = rank(0)
        assertEquals(3, heap.firstIndex())
        assertEquals(1, heap.size)
        // A full heap has no slot after its last entry.
        for (i in 0 until 5) heap[i] = rank(i)
        heap.clear()
        assertTrue(heap.isEmpty())
        for (i in 0 until 5) assertFalse(i in heap)
    }

    @Test
    fun copyIsIndependent() {
        val heap = DoubleIndexedMaxHeap(4)
        heap[0] = rank(1)
        val copy = heap.copy()
        copy[1] = rank(0)
        copy[0] = rank(9)
        copy.remove(0)
        assertEquals(1, heap.size)
        assertEquals(rank(1), heap.keyOf(0))
        assertFalse(1 in heap)
    }

    private fun heapOfEight(): DoubleIndexedMaxHeap {
        val heap = DoubleIndexedMaxHeap(10)
        for (i in 0 until 8) heap[i] = rank(i)
        return heap
    }

    @Test
    fun forEachUnorderedDetectsEveryChangeToTheHeap() {
        // Without the check, these skip or repeat entries and finish normally.
        val changes = listOf<(DoubleIndexedMaxHeap) -> Unit>(
            { it[9] = rank(9) },
            { it[0] = rank(20) },
            { it.decreaseKey(7, (-100).toDouble()) },
            { it.increaseKey(0, 100.toDouble()) },
            { it.remove(3) },
            { it.removeFirst() },
            { it.clear() },
        )
        for (change in changes) {
            val heap = heapOfEight()
            var visits = 0
            assertFailsWith<ConcurrentModificationException> {
                heap.forEachUnordered { _, _ ->
                    visits++
                    if (visits == 2) change(heap)
                }
            }
            assertEquals(2, visits)
        }
    }

    @Test
    fun forEachUnorderedDetectsAChangeOnTheLastEntry() {
        val heap = DoubleIndexedMaxHeap(1)
        heap[0] = rank(1)
        assertFailsWith<ConcurrentModificationException> { heap.forEachUnordered { _, _ -> heap.removeFirst() } }
    }

    @Test
    fun forEachUnorderedAllowsChangesThatKeepTheEntries() {
        val heap = heapOfEight()
        var visits = 0
        heap.forEachUnordered { index, key ->
            assertEquals(key, heap.keyOf(index))
            assertFalse(heap.remove(9))
            heap.copy().clear()
            visits++
        }
        assertEquals(8, visits)
    }

    @Test
    fun textListsEntriesByIndex() {
        val heap = DoubleIndexedMaxHeap(6)
        assertEquals("{}", heap.toString())
        heap[4] = rank(0)
        heap[1] = rank(2)
        heap[2] = rank(1)
        assertEquals("{1=${rank(2)}, 2=${rank(1)}, 4=${rank(0)}}", heap.toString())
    }

    private fun heapOf(indexCount: Int, vararg entries: Pair<Int, Int>): DoubleIndexedMaxHeap {
        val heap = DoubleIndexedMaxHeap(indexCount)
        entries.forEach { (index, key) -> heap[index] = rank(key) }
        return heap
    }

    @Test
    fun equalityIgnoresLayout() {
        val a = heapOf(5, 0 to 1, 1 to 2, 2 to 2, 3 to 3)
        val b = heapOf(5, 3 to 3, 2 to 2, 1 to 2, 0 to 1)
        assertEquals(a, a)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertEquals(a, a.copy())
    }

    @Test
    fun equalityDetectsDifferences() {
        val a = heapOf(5, 0 to 1, 1 to 2)
        assertNotEquals(a, heapOf(5, 0 to 1))
        assertNotEquals(a, heapOf(5, 0 to 1, 2 to 2))
        assertNotEquals(a, heapOf(5, 0 to 1, 1 to 3))
        assertNotEquals(a, heapOf(6, 0 to 1, 1 to 2))
        assertFalse(a.equals(null))
        assertFalse(a.equals("x"))
        assertNotEquals(a.hashCode(), heapOf(5, 0 to 1, 1 to 3).hashCode())
        assertNotEquals(a.hashCode(), heapOf(5, 0 to 1, 2 to 2).hashCode())
    }

    @Test
    fun hashCodeFollowsTheFormula() {
        val heap = heapOf(5, 1 to 2, 3 to 4, 4 to 0)
        // rank(0) comes first in both directions.
        assertEquals(4, heap.removeFirst())
        val expected = 5 + (31 * 1 + rank(2).hashCode()) + (31 * 3 + rank(4).hashCode())
        assertEquals(expected, heap.hashCode())
    }

    @Test
    fun floatingKeysUseTheTotalOrder() {
        val a = DoubleIndexedMaxHeap(1)
        a[0] = -0.0.toDouble()
        val b = DoubleIndexedMaxHeap(1)
        b[0] = 0.0.toDouble()
        assertNotEquals(a, b)
        b.decreaseKey(0, -0.0.toDouble())
        assertEquals(a, b)
        assertFailsWith<IllegalArgumentException> { b.increaseKey(0, -Double.MIN_VALUE) }
        b.increaseKey(0, Double.NaN)
        // NaN equals itself in the total order, so this is not an increase.
        b.decreaseKey(0, Double.NaN)
        a[0] = Double.NaN
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    private fun rank(position: Int): Double {
        return (-position).toDouble()
    }

    private companion object {
        const val INDEX_COUNT = 40

        val NATURAL: Comparator<Double> = naturalOrder()

        val ORDER: Comparator<Double> = reverseOrder()

        // Written in ascending order by hand, so that the expectation does not come from a comparison under test.
        val BOUNDARY_KEYS = listOf(
            Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, (-1).toDouble(), -0.0.toDouble(), 0.0.toDouble(),
            Double.MIN_VALUE, 1.toDouble(), Double.MAX_VALUE, Double.POSITIVE_INFINITY, Double.NaN,
        )
    }
}
