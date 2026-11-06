// SPDX-License-Identifier: CC0-1.0
// Generated from test-templates/KTypeVTypeMinHeapTest.kt. Do not edit; run ./gradlew generateSources.
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

class DoubleLongMinHeapTest {
    private data class Op(val kind: Kind, val key: Double, val value: Long)

    private enum class Kind(val group: Int) { ADD(0), REMOVE_FIRST(1), REPLACE_FIRST(2), COPY_THEN_SWAP(2), CLEAR(2) }

    private fun randomOps(seed: Long, count: Int, distribution: Distribution, domain: ValueDomain): List<Op> {
        val random = Random(seed)
        val groups = Kind.entries.groupBy { it.group }
        return List(count) { step ->
            val group = groups.getValue(random.nextWeighted(distribution))
            val key: Double = when (domain) {
                ValueDomain.SMALL -> random.nextInt(5).toDouble()
                ValueDomain.FULL -> random.nextLong().toDouble()
                ValueDomain.BOUNDARY -> BOUNDARY_KEYS[random.nextInt(BOUNDARY_KEYS.size)]
                ValueDomain.MONOTONIC -> step.toDouble()
            }
            Op(group[random.nextInt(group.size)], key, random.nextInt(1000).toLong())
        }
    }

    /** The model is a plain list. The key that sorts first is the expected first key, and the first pair must be in it. */
    private fun check(ops: List<Op>) {
        var heap = DoubleLongMinHeap()
        val model = ArrayList<Pair<Double, Long>>()
        fun expectedFirstKey() = model.minWith(compareBy(ORDER) { it.first }).first
        fun removeFirstFromModel(): Pair<Double, Long> {
            assertEquals(expectedFirstKey(), heap.firstKey())
            val first = heap.firstKey() to heap.firstValue()
            assertTrue(model.remove(first), "$first is not in the model")
            return first
        }
        for (op in ops) {
            when (op.kind) {
                Kind.ADD -> {
                    heap.add(op.key, op.value)
                    model.add(op.key to op.value)
                }
                Kind.REMOVE_FIRST -> if (model.isNotEmpty()) {
                    removeFirstFromModel()
                    heap.removeFirst()
                }
                Kind.REPLACE_FIRST -> if (model.isNotEmpty()) {
                    removeFirstFromModel()
                    heap.replaceFirst(op.key, op.value)
                    model.add(op.key to op.value)
                }
                Kind.COPY_THEN_SWAP -> {
                    val copy = heap.copy()
                    assertEquals(heap, copy)
                    heap = copy
                }
                Kind.CLEAR -> if (op.value.toInt() < 5) {
                    heap.clear()
                    model.clear()
                }
            }
            assertEquals(model.size, heap.size)
            assertEquals(model.isEmpty(), heap.isEmpty())
            if (model.isNotEmpty()) assertEquals(expectedFirstKey(), heap.firstKey())
        }
        val rest = ArrayList<Pair<Double, Long>>()
        heap.forEachUnordered { k, v -> rest += k to v }
        assertEquals(model.sortedWith(compareBy({ it.first }, { it.second })), rest.sortedWith(compareBy({ it.first }, { it.second })))
        while (model.isNotEmpty()) {
            removeFirstFromModel()
            heap.removeFirst()
        }
        assertTrue(heap.isEmpty())
    }

    @Test
    fun matchesAModelUnderRandomOperations() {
        for (seed in TestConfig.seeds) {
            for (distribution in Distribution.entries) {
                for (domain in ValueDomain.entries) {
                    // The model's minimum is O(n), so keep the runs short and rely on many of them.
                    val ops = randomOps(seed, minOf(TestConfig.operations / 8, 3000), distribution, domain)
                    OperationRunner.run("DoubleLongMinHeap $distribution $domain", seed, ops, ::check)
                }
            }
        }
    }

    @Test
    fun everyInsertionOrderOfSmallHeapsSorts() {
        val base = IntList(7) { it }
        do {
            val heap = DoubleLongMinHeap()
            base.forEach { heap.add(rank(it), (it * 2).toLong()) }
            for (expected in 0 until 7) {
                assertEquals(rank(expected), heap.firstKey())
                assertEquals((expected * 2).toLong(), heap.firstValue())
                heap.removeFirst()
            }
            assertTrue(heap.isEmpty())
        } while (base.nextPermutation())
    }

    @Test
    fun sortsLargeHeapsOfEverySize() {
        // Sizes around the arity's powers exercise a last parent with one to four children.
        for (n in (0..30).toList() + listOf(84, 85, 86, 87, 341, 342, 343, 344, 345, 1000, 5000)) {
            val random = Random(n)
            val keys = List(n) { random.nextLong().toDouble() }
            val heap = DoubleLongMinHeap(0)
            keys.forEachIndexed { i, k -> heap.add(k, i.toLong()) }
            val out = ArrayList<Double>()
            while (heap.isNotEmpty()) {
                val k = heap.firstKey()
                assertEquals(k, keys[heap.firstValue().toInt()])
                out += k
                heap.removeFirst()
            }
            assertEquals(keys.sortedWith(ORDER), out)
        }
    }

    @Test
    fun boundaryKeysOrderCorrectly() {
        for (input in listOf(BOUNDARY_KEYS, BOUNDARY_KEYS.reversed(), BOUNDARY_KEYS.shuffled(Random(1)))) {
            val heap = DoubleLongMinHeap()
            input.forEachIndexed { i, k -> heap.add(k, i.toLong()) }
            for (expected in BOUNDARY_KEYS_IN_HEAP_ORDER) {
                assertEquals(expected, heap.firstKey())
                heap.removeFirst()
            }
            assertTrue(heap.isEmpty())
        }
    }

    @Test
    fun emptyHeapFails() {
        val heap = DoubleLongMinHeap()
        assertFailsWith<NoSuchElementException> { heap.firstKey() }
        assertFailsWith<NoSuchElementException> { heap.firstValue() }
        assertFailsWith<NoSuchElementException> { heap.removeFirst() }
        assertFailsWith<NoSuchElementException> { heap.replaceFirst(0.0, 0.toLong()) }
        assertEquals("[]", heap.toString())
    }

    @Test
    fun replaceFirstKeepsTheOrder() {
        val heap = DoubleLongMinHeap()
        heap.add(rank(1), 1.toLong())
        heap.add(rank(2), 2.toLong())
        heap.add(rank(3), 3.toLong())
        heap.replaceFirst(rank(5), 5.toLong())
        assertEquals(rank(2), heap.firstKey())
        assertEquals(3, heap.size)
        heap.replaceFirst(rank(0), 0.toLong())
        assertEquals(rank(0), heap.firstKey())
        assertEquals(0.toLong(), heap.firstValue())
    }

    @Test
    fun removingTheOnlyElementLeavesItEmpty() {
        val heap = DoubleLongMinHeap()
        heap.add(rank(1), 1.toLong())
        heap.removeFirst()
        assertTrue(heap.isEmpty())
        assertFalse(heap.isNotEmpty())
        heap.add(rank(2), 2.toLong())
        assertTrue(heap.isNotEmpty())
        assertEquals(rank(2), heap.firstKey())
    }

    @Test
    fun capacityControl() {
        assertFailsWith<IllegalArgumentException> { DoubleLongMinHeap(-1) }
        val heap = DoubleLongMinHeap(2)
        assertEquals(2, heap.capacity)
        heap.ensureCapacity(2)
        assertEquals(2, heap.capacity)
        heap.ensureCapacity(50)
        assertEquals(50, heap.capacity)
        heap.add(rank(1), 1.toLong())
        heap.clear()
        assertEquals(0, heap.size)
        assertEquals(50, heap.capacity)
        assertEquals(DEFAULT_CAPACITY, DoubleLongMinHeap().capacity)
    }

    @Test
    fun copyIsIndependent() {
        val heap = DoubleLongMinHeap()
        heap.add(rank(1), 1.toLong())
        val copy = heap.copy()
        copy.add(rank(0), 0.toLong())
        copy.replaceFirst(rank(9), 9.toLong())
        assertEquals(1, heap.size)
        assertEquals(rank(1), heap.firstKey())
        assertEquals(1.toLong(), heap.firstValue())
    }

    private fun heapOfEight(): DoubleLongMinHeap {
        val heap = DoubleLongMinHeap()
        for (i in 1..8) heap.add(rank(i), i.toLong())
        return heap
    }

    @Test
    fun forEachUnorderedDetectsEveryChangeToTheHeap() {
        // Without the check, these skip or repeat pairs and finish normally.
        val changes = listOf<(DoubleLongMinHeap) -> Unit>(
            { it.add(rank(9), 9.toLong()) },
            { it.removeFirst() },
            { it.replaceFirst(rank(9), 9.toLong()) },
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
    fun forEachUnorderedDetectsAChangeOnTheLastPair() {
        val heap = DoubleLongMinHeap()
        heap.add(rank(1), 1.toLong())
        assertFailsWith<ConcurrentModificationException> { heap.forEachUnordered { _, _ -> heap.removeFirst() } }
    }

    @Test
    fun forEachUnorderedAllowsChangesThatKeepThePairs() {
        val heap = heapOfEight()
        var visits = 0
        heap.forEachUnordered { _, _ ->
            heap.ensureCapacity(100)
            heap.copy().clear()
            visits++
        }
        assertEquals(8, visits)
    }

    @Test
    fun forEachUnorderedWorksAgainAfterAChange() {
        val heap = heapOfEight()
        heap.removeFirst()
        var visits = 0
        heap.forEachUnordered { _, _ -> visits++ }
        assertEquals(7, visits)
    }

    @Test
    fun equalityIgnoresTheReservedCapacityAndLeavesItAlone() {
        val a = heapOf(1 to 1, 2 to 2, 3 to 3)
        val b = DoubleLongMinHeap(3)
        b.add(rank(3), 3.toLong())
        b.add(rank(2), 2.toLong())
        b.add(rank(1), 1.toLong())
        a.ensureCapacity(1000)
        assertEquals(a, b)
        assertEquals(1000, a.capacity)
        assertEquals(3, b.capacity)
        assertEquals(a, a.copy())
        assertEquals(1000, a.copy().capacity)
    }

    @Test
    fun text() {
        val heap = DoubleLongMinHeap()
        heap.add(rank(1), 2.toLong())
        heap.add(rank(3), 4.toLong())
        assertEquals("[${rank(1)}=${2.toLong()}, ${rank(3)}=${4.toLong()}]", heap.toString())
    }

    private fun heapOf(vararg pairs: Pair<Int, Int>): DoubleLongMinHeap {
        val heap = DoubleLongMinHeap()
        pairs.forEach { (k, v) -> heap.add(rank(k), v.toLong()) }
        return heap
    }

    @Test
    fun equalityIgnoresLayoutAndTieOrder() {
        val a = heapOf(1 to 1, 2 to 2, 2 to 3, 3 to 4)
        val b = heapOf(3 to 4, 2 to 3, 2 to 2, 1 to 1)
        assertEquals(a, a)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertEquals(a.size, b.size)
        assertEquals(a.toString().length, b.toString().length)
    }

    @Test
    fun equalityDetectsDifferences() {
        val a = heapOf(1 to 1, 2 to 2)
        assertNotEquals(a, heapOf(1 to 1))
        assertNotEquals(a, heapOf(1 to 1, 3 to 2))
        assertNotEquals(a, heapOf(1 to 1, 2 to 3))
        assertNotEquals(heapOf(1 to 1, 1 to 2), heapOf(1 to 1, 2 to 2))
        assertNotEquals(heapOf(1 to 1, 2 to 2), heapOf(1 to 1, 1 to 2))
        assertFalse(a.equals(null))
        assertFalse(a.equals("x"))
        assertNotEquals(a.hashCode(), heapOf(1 to 1, 2 to 3).hashCode())
    }

    @Test
    fun hashCodeFollowsTheFormulaAndIgnoresStaleSlots() {
        val heap = heapOf(1 to 2, 3 to 4, 5 to 6)
        heap.removeFirst()
        val expected = 2 + (31 * rank(3).hashCode() + 4.toLong().hashCode()) + (31 * rank(5).hashCode() + 6.toLong().hashCode())
        assertEquals(expected, heap.hashCode())
        assertEquals(heapOf(3 to 4, 5 to 6).hashCode(), heap.hashCode())
    }

    @Test
    fun equalityLeavesTheHeapsIntact() {
        val a = heapOf(1 to 1, 2 to 2)
        val b = heapOf(2 to 2, 1 to 1)
        assertEquals(a, b)
        assertEquals(2, a.size)
        assertEquals(2, b.size)
    }

    @Test
    fun floatingKeysUseTheTotalOrder() {
        val a = DoubleLongMinHeap()
        a.add(-0.0.toDouble(), 1.toLong())
        val b = DoubleLongMinHeap()
        b.add(0.0.toDouble(), 1.toLong())
        assertNotEquals(a, b)
        val c = DoubleLongMinHeap()
        c.add(Double.NaN, 1.toLong())
        val d = DoubleLongMinHeap()
        d.add(Double.NaN, 1.toLong())
        assertEquals(c, d)
        assertEquals(c.hashCode(), d.hashCode())
    }


    private fun rank(position: Int): Double {
        return position.toDouble()
    }

    private companion object {
        val ORDER: Comparator<Double> = naturalOrder()

        // Written in ascending order by hand, so that the expectation does not come from a comparison under test.
        val BOUNDARY_KEYS = listOf(
            Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, (-1).toDouble(), -0.0.toDouble(), 0.0.toDouble(),
            Double.MIN_VALUE, 1.toDouble(), Double.MAX_VALUE, Double.POSITIVE_INFINITY, Double.NaN,
        )

        val BOUNDARY_KEYS_IN_HEAP_ORDER = BOUNDARY_KEYS
    }
}
