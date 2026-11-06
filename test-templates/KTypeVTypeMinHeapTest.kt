// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
//#if V_OBJ
import kotlin.test.assertNull
//#endif
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

    /** The model is a plain list. The key that sorts first is the expected first key, and the first pair must be in it. */
    private fun check(ops: List<Op>) {
        var heap = KTypeVTypeMinHeap()
        val model = ArrayList<Pair<KType, VType>>()
        fun expectedFirstKey() = model.minWith(compareBy(ORDER) { it.first }).first
        fun removeFirstFromModel(): Pair<KType, VType> {
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
            base.forEach { heap.add(rank(it), (it * 2).toVType()) }
            for (expected in 0 until 7) {
                assertEquals(rank(expected), heap.firstKey())
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
            assertEquals(keys.sortedWith(ORDER), out)
        }
    }

    @Test
    fun boundaryKeysOrderCorrectly() {
        for (input in listOf(BOUNDARY_KEYS, BOUNDARY_KEYS.reversed(), BOUNDARY_KEYS.shuffled(Random(1)))) {
            val heap = KTypeVTypeMinHeap()
            input.forEachIndexed { i, k -> heap.add(k, i.toVType()) }
            for (expected in BOUNDARY_KEYS_IN_HEAP_ORDER) {
                assertEquals(expected, heap.firstKey())
                heap.removeFirst()
            }
            assertTrue(heap.isEmpty())
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
        heap.add(rank(1), 1.toVType())
        heap.add(rank(2), 2.toVType())
        heap.add(rank(3), 3.toVType())
        heap.replaceFirst(rank(5), 5.toVType())
        assertEquals(rank(2), heap.firstKey())
        assertEquals(3, heap.size)
        heap.replaceFirst(rank(0), 0.toVType())
        assertEquals(rank(0), heap.firstKey())
        assertEquals(0.toVType(), heap.firstValue())
    }

    @Test
    fun removingTheOnlyElementLeavesItEmpty() {
        val heap = KTypeVTypeMinHeap()
        heap.add(rank(1), 1.toVType())
        heap.removeFirst()
        assertTrue(heap.isEmpty())
        assertFalse(heap.isNotEmpty())
        heap.add(rank(2), 2.toVType())
        assertTrue(heap.isNotEmpty())
        assertEquals(rank(2), heap.firstKey())
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
        heap.add(rank(1), 1.toVType())
        heap.clear()
        assertEquals(0, heap.size)
        assertEquals(50, heap.capacity)
        assertEquals(DEFAULT_CAPACITY, KTypeVTypeMinHeap().capacity)
    }

    @Test
    fun copyIsIndependent() {
        val heap = KTypeVTypeMinHeap()
        heap.add(rank(1), 1.toVType())
        val copy = heap.copy()
        copy.add(rank(0), 0.toVType())
        copy.replaceFirst(rank(9), 9.toVType())
        assertEquals(1, heap.size)
        assertEquals(rank(1), heap.firstKey())
        assertEquals(1.toVType(), heap.firstValue())
    }

    @Test
    fun text() {
        val heap = KTypeVTypeMinHeap()
        heap.add(rank(1), 2.toVType())
        heap.add(rank(3), 4.toVType())
        assertEquals("[${rank(1)}=${2.toVType()}, ${rank(3)}=${4.toVType()}]", heap.toString())
    }

    private fun heapOf(vararg pairs: Pair<Int, Int>): KTypeVTypeMinHeap {
        val heap = KTypeVTypeMinHeap()
        pairs.forEach { (k, v) -> heap.add(rank(k), v.toVType()) }
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
        val expected = 2 + (31 * rank(3).hashCode() + 4.toVType().hashCode()) + (31 * rank(5).hashCode() + 6.toVType().hashCode())
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

    //#if K_FLOATING
    @Test
    fun floatingKeysUseTheTotalOrder() {
        val a = KTypeVTypeMinHeap()
        a.add(-0.0.toKType(), 1.toVType())
        val b = KTypeVTypeMinHeap()
        b.add(0.0.toKType(), 1.toVType())
        assertNotEquals(a, b)
        val c = KTypeVTypeMinHeap()
        c.add(KType.NaN, 1.toVType())
        val d = KTypeVTypeMinHeap()
        d.add(KType.NaN, 1.toVType())
        assertEquals(c, d)
        assertEquals(c.hashCode(), d.hashCode())
    }
    //#endif

    //#if V_OBJ
    @Test
    fun removedAndClearedValuesAreReleased() {
        val heap = KTypeObjMinHeap<String?>()
        heap.add(rank(1), "a")
        heap.add(rank(2), "b")
        heap.add(rank(3), "c")
        heap.removeFirst()
        assertEquals(setOf<String?>("b", "c"), setOf(heap.valueAt(0), heap.valueAt(1)))
        assertNull(heap.valueAt(2))
        heap.clear()
        assertNull(heap.valueAt(0))
        assertNull(heap.valueAt(1))
    }

    @Test
    fun removingTheOnlyValueReleasesIt() {
        val heap = KTypeObjMinHeap<String?>()
        heap.add(rank(1), "a")
        heap.removeFirst()
        assertNull(heap.valueAt(0))
    }

    @Test
    fun nullIsAValue() {
        val heap = KTypeObjMinHeap<String?>()
        heap.add(rank(2), "b")
        heap.add(rank(1), null)
        assertNull(heap.firstValue())
        val other = KTypeObjMinHeap<String?>()
        other.add(rank(1), null)
        other.add(rank(2), "b")
        assertEquals(heap, other)
        assertEquals(heap.hashCode(), other.hashCode())
        assertEquals("[${rank(1)}=null, ${rank(2)}=b]", heap.toString())
        val different = KTypeObjMinHeap<String?>()
        different.add(rank(1), "a")
        different.add(rank(2), "b")
        assertNotEquals(heap, different)
    }

    @Test
    fun equalityUsesEqualsAndCountsDuplicates() {
        fun heapOf(vararg values: String): KTypeObjMinHeap<String> {
            val heap = KTypeObjMinHeap<String>()
            values.forEach { heap.add(rank(1), String(it.toCharArray())) }
            return heap
        }
        assertEquals(heapOf("a", "b", "a"), heapOf("a", "a", "b"))
        assertNotEquals(heapOf("a", "a", "b"), heapOf("a", "b", "b"))
    }

    @Test
    fun valuesKeepTheirTypes() {
        val heap = KTypeObjMinHeap<List<String>>(0)
        heap.add(rank(1), listOf("x"))
        var seen = ""
        heap.forEachUnordered { _, value -> seen = value[0] }
        assertEquals("x", seen)
        assertEquals("x", heap.firstValue()[0])
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

        //#if K_MAX_HEAP
        val BOUNDARY_KEYS_IN_HEAP_ORDER = BOUNDARY_KEYS.reversed()
        //#else
        val BOUNDARY_KEYS_IN_HEAP_ORDER = BOUNDARY_KEYS
        //#endif
    }
}
