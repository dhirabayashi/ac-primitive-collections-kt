// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class KTypeVTypeMinHeapTest {
    private data class Op(val kind: Kind, val key: KType, val value: VType)

    private enum class Kind(val group: Int) { ADD(0), REMOVE_FIRST(1), REPLACE_FIRST(2), COPY_THEN_SWAP(2), CLEAR(2) }

    private fun randomOps(seed: Long, count: Int, distribution: Distribution, domain: ValueDomain): List<Op> {
        val random = Random(seed)
        val groups = Kind.entries.groupBy { it.group }
        return List(count) { step ->
            val group = groups.getValue(random.nextWeighted(distribution))
            val key: KType = when (domain) {
                ValueDomain.SMALL -> random.nextInt(5).toKType()
                ValueDomain.FULL -> random.nextLong().toKType()
                ValueDomain.BOUNDARY -> BOUNDARY_KEYS[random.nextInt(BOUNDARY_KEYS.size)]
                ValueDomain.MONOTONIC -> step.toKType()
            }
            Op(group[random.nextInt(group.size)], key, random.nextInt(1000).toVType())
        }
    }

    /** The model is a plain list. Its minimum key is the expected first key, and the first pair must be in it. */
    private fun check(ops: List<Op>) {
        var heap = KTypeVTypeMinHeap()
        val model = ArrayList<Pair<KType, VType>>()
        fun removeFirstFromModel(): Pair<KType, VType> {
            val minimum = model.minOf { it.first }
            assertEquals(minimum, heap.firstKey())
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
            if (model.isNotEmpty()) assertEquals(model.minOf { it.first }, heap.firstKey())
        }
        val rest = ArrayList<Pair<KType, VType>>()
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
                    OperationRunner.run("KTypeVTypeMinHeap $distribution $domain", seed, ops, ::check)
                }
            }
        }
    }

    @Test
    fun everyInsertionOrderOfSmallHeapsSorts() {
        val base = IntList(7) { it }
        do {
            val heap = KTypeVTypeMinHeap()
            base.forEach { heap.add(it.toKType(), (it * 2).toVType()) }
            for (expected in 0 until 7) {
                assertEquals(expected.toKType(), heap.firstKey())
                assertEquals((expected * 2).toVType(), heap.firstValue())
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
            val keys = List(n) { random.nextLong().toKType() }
            val heap = KTypeVTypeMinHeap(0)
            keys.forEachIndexed { i, k -> heap.add(k, i.toVType()) }
            val out = ArrayList<KType>()
            while (heap.isNotEmpty()) {
                val k = heap.firstKey()
                assertEquals(k, keys[heap.firstValue().toInt()])
                out += k
                heap.removeFirst()
            }
            assertEquals(keys.sorted(), out)
        }
    }

    @Test
    fun boundaryKeysOrderCorrectly() {
        val heap = KTypeVTypeMinHeap()
        BOUNDARY_KEYS.reversed().forEachIndexed { i, k -> heap.add(k, i.toVType()) }
        for (expected in BOUNDARY_KEYS) {
            assertEquals(expected, heap.firstKey())
            heap.removeFirst()
        }
    }

    @Test
    fun emptyHeapFails() {
        val heap = KTypeVTypeMinHeap()
        assertFailsWith<NoSuchElementException> { heap.firstKey() }
        assertFailsWith<NoSuchElementException> { heap.firstValue() }
        assertFailsWith<NoSuchElementException> { heap.removeFirst() }
        assertFailsWith<NoSuchElementException> { heap.replaceFirst(K_ZERO, 0.toVType()) }
        assertEquals("[]", heap.toString())
    }

    @Test
    fun replaceFirstKeepsTheOrder() {
        val heap = KTypeVTypeMinHeap()
        heap.add(1.toKType(), 1.toVType())
        heap.add(2.toKType(), 2.toVType())
        heap.add(3.toKType(), 3.toVType())
        heap.replaceFirst(5.toKType(), 5.toVType())
        assertEquals(2.toKType(), heap.firstKey())
        assertEquals(3, heap.size)
        heap.replaceFirst(0.toKType(), 0.toVType())
        assertEquals(0.toKType(), heap.firstKey())
        assertEquals(0.toVType(), heap.firstValue())
    }

    @Test
    fun removingTheOnlyElementLeavesItEmpty() {
        val heap = KTypeVTypeMinHeap()
        heap.add(1.toKType(), 1.toVType())
        heap.removeFirst()
        assertTrue(heap.isEmpty())
        assertFalse(heap.isNotEmpty())
        heap.add(2.toKType(), 2.toVType())
        assertTrue(heap.isNotEmpty())
        assertEquals(2.toKType(), heap.firstKey())
    }

    @Test
    fun capacityControl() {
        assertFailsWith<IllegalArgumentException> { KTypeVTypeMinHeap(-1) }
        val heap = KTypeVTypeMinHeap(2)
        assertEquals(2, heap.capacity)
        heap.ensureCapacity(2)
        assertEquals(2, heap.capacity)
        heap.ensureCapacity(50)
        assertEquals(50, heap.capacity)
        heap.add(1.toKType(), 1.toVType())
        heap.clear()
        assertEquals(0, heap.size)
        assertEquals(50, heap.capacity)
        assertEquals(DEFAULT_CAPACITY, KTypeVTypeMinHeap().capacity)
    }

    @Test
    fun copyIsIndependent() {
        val heap = KTypeVTypeMinHeap()
        heap.add(1.toKType(), 1.toVType())
        val copy = heap.copy()
        copy.add(0.toKType(), 0.toVType())
        copy.replaceFirst(9.toKType(), 9.toVType())
        assertEquals(1, heap.size)
        assertEquals(1.toKType(), heap.firstKey())
        assertEquals(1.toVType(), heap.firstValue())
    }

    @Test
    fun text() {
        val heap = KTypeVTypeMinHeap()
        heap.add(1.toKType(), 2.toVType())
        heap.add(3.toKType(), 4.toVType())
        assertEquals("[1=2, 3=4]", heap.toString())
    }

    private fun heapOf(vararg pairs: Pair<Int, Int>): KTypeVTypeMinHeap {
        val heap = KTypeVTypeMinHeap()
        pairs.forEach { (k, v) -> heap.add(k.toKType(), v.toVType()) }
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
        val expected = 2 + (31 * 3.toKType().hashCode() + 4.toVType().hashCode()) + (31 * 5.toKType().hashCode() + 6.toVType().hashCode())
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

    private companion object {
        val BOUNDARY_KEYS = listOf(KType.MIN_VALUE, (-1).toKType(), K_ZERO, 1.toKType(), KType.MAX_VALUE)
    }
}
