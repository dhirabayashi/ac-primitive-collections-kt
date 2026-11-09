// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class KTypeIndexedMinHeapTest {
    private data class Op(val kind: Kind, val index: Int, val key: KType)

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
            val key: KType = when (domain) {
                ValueDomain.SMALL -> random.nextInt(5).toKType()
                ValueDomain.FULL -> K_RANDOM(random)
                ValueDomain.BOUNDARY -> BOUNDARY_KEYS[random.nextInt(BOUNDARY_KEYS.size)]
                ValueDomain.MONOTONIC -> step.toKType()
            }
            Op(group[random.nextInt(group.size)], random.nextInt(INDEX_COUNT), key)
        }
    }

    /** The model maps each present index to its key. The first entry must hold the key that sorts first. */
    private fun check(ops: List<Op>) {
        var heap = KTypeIndexedMinHeap(INDEX_COUNT)
        val model = HashMap<Int, KType>()
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
        val rest = HashMap<Int, KType>()
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
                    OperationRunner.run("KTypeIndexedMinHeap $distribution $domain", seed, ops, ::check)
                }
            }
        }
    }

    @Test
    fun sortsHeapsOfEverySize() {
        // Sizes around the arity's powers exercise a last parent with one to four children.
        for (n in (0..30).toList() + listOf(84, 85, 86, 87, 341, 342, 343, 344, 345, 1000)) {
            val random = Random(n)
            val keys = List(n) { K_RANDOM(random) }
            val heap = KTypeIndexedMinHeap(n)
            keys.forEachIndexed { i, k -> heap[i] = k }
            val out = ArrayList<KType>()
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
        val heap = KTypeIndexedMinHeap(10)
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
        val heap = KTypeIndexedMinHeap(3)
        heap[0] = 5.toKType()
        heap[1] = 6.toKType()
        heap.decreaseKey(0, 5.toKType())
        heap.decreaseKey(0, 3.toKType())
        heap.increaseKey(1, 6.toKType())
        heap.increaseKey(1, 9.toKType())
        assertEquals(3.toKType(), heap.keyOf(0))
        assertEquals(9.toKType(), heap.keyOf(1))
        assertFailsWith<IllegalArgumentException> { heap.decreaseKey(0, 4.toKType()) }
        assertFailsWith<IllegalArgumentException> { heap.increaseKey(1, 8.toKType()) }
        assertFailsWith<NoSuchElementException> { heap.decreaseKey(2, 0.toKType()) }
        assertFailsWith<NoSuchElementException> { heap.increaseKey(2, 0.toKType()) }
        //#if K_MAX_HEAP
        assertEquals(1, heap.firstIndex())
        //#else
        assertEquals(0, heap.firstIndex())
        //#endif
    }

    @Test
    fun removingAnyEntryKeepsTheOrder() {
        // Removing from the middle moves the last entry into the hole, and it may belong above or below.
        for (n in 1..40) {
            for (victim in 0 until n) {
                val heap = KTypeIndexedMinHeap(n)
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
        val heap = KTypeIndexedMinHeap(3)
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
        assertFailsWith<IllegalArgumentException> { KTypeIndexedMinHeap(-1) }
        val heap = KTypeIndexedMinHeap(3)
        assertEquals(3, heap.indexCount)
        for (index in listOf(-1, 3, Int.MIN_VALUE, Int.MAX_VALUE)) {
            // The check names the index. Without it, the array access would throw a less clear subclass.
            assertEquals(IndexOutOfBoundsException::class, assertFailsWith<IndexOutOfBoundsException> { index in heap }::class)
            assertFailsWith<IndexOutOfBoundsException> { heap[index] = K_ZERO }
            assertFailsWith<IndexOutOfBoundsException> { heap.keyOf(index) }
            assertFailsWith<IndexOutOfBoundsException> { heap.remove(index) }
            assertFailsWith<IndexOutOfBoundsException> { heap.decreaseKey(index, K_ZERO) }
            assertFailsWith<IndexOutOfBoundsException> { heap.increaseKey(index, K_ZERO) }
        }
        val none = KTypeIndexedMinHeap(0)
        assertEquals(0, none.indexCount)
        assertFailsWith<IndexOutOfBoundsException> { none[0] = K_ZERO }
    }

    @Test
    fun clearForgetsEveryIndex() {
        val heap = KTypeIndexedMinHeap(5)
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
        val heap = KTypeIndexedMinHeap(4)
        heap[0] = rank(1)
        val copy = heap.copy()
        copy[1] = rank(0)
        copy[0] = rank(9)
        copy.remove(0)
        assertEquals(1, heap.size)
        assertEquals(rank(1), heap.keyOf(0))
        assertFalse(1 in heap)
    }

    private fun heapOfEight(): KTypeIndexedMinHeap {
        val heap = KTypeIndexedMinHeap(10)
        for (i in 0 until 8) heap[i] = rank(i)
        return heap
    }

    @Test
    fun forEachUnorderedDetectsEveryChangeToTheHeap() {
        // Without the check, these skip or repeat entries and finish normally.
        val changes = listOf<(KTypeIndexedMinHeap) -> Unit>(
            { it[9] = rank(9) },
            { it[0] = rank(20) },
            { it.decreaseKey(7, (-100).toKType()) },
            { it.increaseKey(0, 100.toKType()) },
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
        val heap = KTypeIndexedMinHeap(1)
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
        val heap = KTypeIndexedMinHeap(6)
        assertEquals("{}", heap.toString())
        heap[4] = rank(0)
        heap[1] = rank(2)
        heap[2] = rank(1)
        assertEquals("{1=${rank(2)}, 2=${rank(1)}, 4=${rank(0)}}", heap.toString())
    }

    private fun heapOf(indexCount: Int, vararg entries: Pair<Int, Int>): KTypeIndexedMinHeap {
        val heap = KTypeIndexedMinHeap(indexCount)
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

    //#if K_FLOATING
    @Test
    fun floatingKeysUseTheTotalOrder() {
        val a = KTypeIndexedMinHeap(1)
        a[0] = -0.0.toKType()
        val b = KTypeIndexedMinHeap(1)
        b[0] = 0.0.toKType()
        assertNotEquals(a, b)
        b.decreaseKey(0, -0.0.toKType())
        assertEquals(a, b)
        assertFailsWith<IllegalArgumentException> { b.increaseKey(0, -KType.MIN_VALUE) }
        b.increaseKey(0, KType.NaN)
        // NaN equals itself in the total order, so this is not an increase.
        b.decreaseKey(0, KType.NaN)
        a[0] = KType.NaN
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
    //#endif

    private fun rank(position: Int): KType {
        //#if K_MAX_HEAP
        return (-position).toKType()
        //#else
        return position.toKType()
        //#endif
    }

    private companion object {
        const val INDEX_COUNT = 40

        val NATURAL: Comparator<KType> = naturalOrder()

        //#if K_MAX_HEAP
        val ORDER: Comparator<KType> = reverseOrder()
        //#else
        val ORDER: Comparator<KType> = naturalOrder()
        //#endif

        // Written in ascending order by hand, so that the expectation does not come from a comparison under test.
        //#if K_FLOATING
        val BOUNDARY_KEYS = listOf(
            KType.NEGATIVE_INFINITY, -KType.MAX_VALUE, (-1).toKType(), -0.0.toKType(), 0.0.toKType(),
            KType.MIN_VALUE, 1.toKType(), KType.MAX_VALUE, KType.POSITIVE_INFINITY, KType.NaN,
        )
        //#else
        val BOUNDARY_KEYS = listOf(KType.MIN_VALUE, (-1).toKType(), K_ZERO, 1.toKType(), KType.MAX_VALUE)
        //#endif
    }
}
