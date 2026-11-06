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

class LongLongMinHeapTest {
    private data class Op(val kind: Kind, val key: Long, val value: Long)

    private enum class Kind(val group: Int) { ADD(0), REMOVE_FIRST(1), REPLACE_FIRST(2), COPY_THEN_SWAP(2), CLEAR(2) }

    private fun randomOps(seed: Long, count: Int, distribution: Distribution, domain: ValueDomain): List<Op> {
        val random = Random(seed)
        val groups = Kind.entries.groupBy { it.group }
        return List(count) { step ->
            val group = groups.getValue(random.nextWeighted(distribution))
            val key: Long = when (domain) {
                ValueDomain.SMALL -> random.nextInt(5).toLong()
                ValueDomain.FULL -> random.nextLong().toLong()
                ValueDomain.BOUNDARY -> BOUNDARY_KEYS[random.nextInt(BOUNDARY_KEYS.size)]
                ValueDomain.MONOTONIC -> step.toLong()
            }
            Op(group[random.nextInt(group.size)], key, random.nextInt(1000).toLong())
        }
    }

    /** The model is a plain list. The key that sorts first is the expected first key, and the first pair must be in it. */
    private fun check(ops: List<Op>) {
        var heap = LongLongMinHeap()
        val model = ArrayList<Pair<Long, Long>>()
        fun expectedFirstKey() = model.minWith(compareBy(ORDER) { it.first }).first
        fun removeFirstFromModel(): Pair<Long, Long> {
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
        val rest = ArrayList<Pair<Long, Long>>()
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
                    OperationRunner.run("LongLongMinHeap $distribution $domain", seed, ops, ::check)
                }
            }
        }
    }

    @Test
    fun everyInsertionOrderOfSmallHeapsSorts() {
        val base = IntList(7) { it }
        do {
            val heap = LongLongMinHeap()
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
            val keys = List(n) { random.nextLong().toLong() }
            val heap = LongLongMinHeap(0)
            keys.forEachIndexed { i, k -> heap.add(k, i.toLong()) }
            val out = ArrayList<Long>()
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
            val heap = LongLongMinHeap()
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
        val heap = LongLongMinHeap()
        assertFailsWith<NoSuchElementException> { heap.firstKey() }
        assertFailsWith<NoSuchElementException> { heap.firstValue() }
        assertFailsWith<NoSuchElementException> { heap.removeFirst() }
        assertFailsWith<NoSuchElementException> { heap.replaceFirst(0L, 0.toLong()) }
        assertEquals("[]", heap.toString())
    }

    @Test
    fun replaceFirstKeepsTheOrder() {
        val heap = LongLongMinHeap()
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
        val heap = LongLongMinHeap()
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
        assertFailsWith<IllegalArgumentException> { LongLongMinHeap(-1) }
        val heap = LongLongMinHeap(2)
        assertEquals(2, heap.capacity)
        heap.ensureCapacity(2)
        assertEquals(2, heap.capacity)
        heap.ensureCapacity(50)
        assertEquals(50, heap.capacity)
        heap.add(rank(1), 1.toLong())
        heap.clear()
        assertEquals(0, heap.size)
        assertEquals(50, heap.capacity)
        assertEquals(DEFAULT_CAPACITY, LongLongMinHeap().capacity)
    }

    @Test
    fun copyIsIndependent() {
        val heap = LongLongMinHeap()
        heap.add(rank(1), 1.toLong())
        val copy = heap.copy()
        copy.add(rank(0), 0.toLong())
        copy.replaceFirst(rank(9), 9.toLong())
        assertEquals(1, heap.size)
        assertEquals(rank(1), heap.firstKey())
        assertEquals(1.toLong(), heap.firstValue())
    }

    @Test
    fun text() {
        val heap = LongLongMinHeap()
        heap.add(rank(1), 2.toLong())
        heap.add(rank(3), 4.toLong())
        assertEquals("[${rank(1)}=${2.toLong()}, ${rank(3)}=${4.toLong()}]", heap.toString())
    }

    private fun heapOf(vararg pairs: Pair<Int, Int>): LongLongMinHeap {
        val heap = LongLongMinHeap()
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



    private fun rank(position: Int): Long {
        return position.toLong()
    }

    private companion object {
        val ORDER: Comparator<Long> = naturalOrder()

        // Written in ascending order by hand, so that the expectation does not come from a comparison under test.
        val BOUNDARY_KEYS = listOf(Long.MIN_VALUE, (-1).toLong(), 0L, 1.toLong(), Long.MAX_VALUE)

        val BOUNDARY_KEYS_IN_HEAP_ORDER = BOUNDARY_KEYS
    }
}
