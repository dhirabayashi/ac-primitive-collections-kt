// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class KTypeMinHeapTest {
    private data class Op(val kind: Kind, val value: KType, val count: Int)

    private enum class Kind(val group: Int) {
        ADD(0), ADD_ALL(0), ADD_ALL_RANGE(0), ADD_ALL_HEAP(0),
        REMOVE_FIRST(1), REMOVE_FIRST_OR_DEFAULT(1),
        REPLACE_FIRST(2), COPY_THEN_SWAP(2), CLEAR(2), REBUILD(2),
    }

    private fun randomValue(random: Random, domain: ValueDomain, step: Int): KType = when (domain) {
        ValueDomain.SMALL -> random.nextInt(5).toKType()
        ValueDomain.FULL -> K_RANDOM(random)
        ValueDomain.BOUNDARY -> BOUNDARY_KEYS[random.nextInt(BOUNDARY_KEYS.size)]
        ValueDomain.MONOTONIC -> step.toKType()
    }

    private fun randomOps(seed: Long, count: Int, distribution: Distribution, domain: ValueDomain): List<Op> {
        val random = Random(seed)
        val groups = Kind.entries.groupBy { it.group }
        return List(count) { step ->
            val group = groups.getValue(random.nextWeighted(distribution))
            // Bulk adds are mostly short, so that they take both the sift and the rebuild path.
            val bulk = if (random.nextInt(8) == 0) random.nextInt(40) else random.nextInt(4)
            Op(group[random.nextInt(group.size)], randomValue(random, domain, step), bulk)
        }
    }

    /** The values that a bulk add uses: derived from the op alone, so that any sublist of ops is still valid. */
    private fun bulkValues(op: Op): KTypeArray = KTypeArray(op.count) { if (it % 2 == 0) op.value else rank(it) }

    /** The model is a plain list. The value that sorts first is the expected first value. */
    private fun check(ops: List<Op>) {
        var heap = KTypeMinHeap()
        val model = ArrayList<KType>()
        fun expectedFirst() = model.minWith(ORDER)
        fun removeFromModel(removed: KType) {
            assertEquals(expectedFirst(), removed)
            model.removeAt(model.indexOfFirst { ORDER.compare(it, removed) == 0 })
        }
        for (op in ops) {
            when (op.kind) {
                Kind.ADD -> {
                    heap += op.value
                    model.add(op.value)
                }
                Kind.ADD_ALL -> {
                    val values = bulkValues(op)
                    heap.addAll(values)
                    model.addAll(values.toList())
                }
                Kind.ADD_ALL_RANGE -> {
                    val values = bulkValues(op)
                    heap.addAll(values, values.size / 3, values.size)
                    model.addAll(values.toList().subList(values.size / 3, values.size))
                }
                Kind.ADD_ALL_HEAP -> {
                    val other = bulkValues(op).toKTypeMinHeap()
                    heap.addAll(other)
                    other.forEachUnordered { model.add(it) }
                }
                Kind.REMOVE_FIRST -> if (model.isNotEmpty()) removeFromModel(heap.removeFirst())
                Kind.REMOVE_FIRST_OR_DEFAULT -> if (model.isEmpty()) {
                    assertEquals(op.value, heap.removeFirstOrDefault(op.value))
                } else {
                    removeFromModel(heap.removeFirstOrDefault(op.value))
                }
                Kind.REPLACE_FIRST -> if (model.isNotEmpty()) {
                    removeFromModel(heap.replaceFirst(op.value))
                    model.add(op.value)
                }
                Kind.COPY_THEN_SWAP -> {
                    val copy = heap.copy()
                    assertEquals(heap, copy)
                    heap = copy
                }
                Kind.CLEAR -> if (op.count < 2) {
                    heap.clear()
                    model.clear()
                }
                Kind.REBUILD -> heap = heap.toKTypeArray().toKTypeMinHeap()
            }
            assertEquals(model.size, heap.size)
            assertEquals(model.isEmpty(), heap.isEmpty())
            if (model.isNotEmpty()) assertEquals(expectedFirst(), heap.first())
        }
        assertEquals(model.sortedWith(ORDER), heap.toSortedArray().toList())
        while (model.isNotEmpty()) removeFromModel(heap.removeFirst())
        assertTrue(heap.isEmpty())
    }

    @Test
    fun matchesAModelUnderRandomOperations() {
        for (seed in TestConfig.seeds) {
            for (distribution in Distribution.entries) {
                for (domain in ValueDomain.entries) {
                    // The model's minimum is O(n), so keep the runs short and rely on many of them.
                    val ops = randomOps(seed, minOf(TestConfig.operations / 8, 3000), distribution, domain)
                    OperationRunner.run("KTypeMinHeap $distribution $domain", seed, ops, ::check)
                }
            }
        }
    }

    @Test
    fun everyInsertionOrderOfSmallHeapsSorts() {
        val base = IntList(7) { it }
        do {
            val heap = KTypeMinHeap()
            base.forEach { heap.add(rank(it)) }
            val built = KTypeArray(7) { rank(base[it]) }.toKTypeMinHeap()
            for (expected in 0 until 7) {
                assertEquals(rank(expected), heap.removeFirst())
                assertEquals(rank(expected), built.removeFirst())
            }
            assertTrue(heap.isEmpty())
        } while (base.nextPermutation())
    }

    @Test
    fun sortsLargeHeapsOfEverySize() {
        // Sizes around the arity's powers exercise a last parent with one to four children.
        for (n in (0..30).toList() + listOf(84, 85, 86, 87, 341, 342, 343, 344, 345, 1000, 5000)) {
            val random = Random(n)
            val values = KTypeArray(n) { K_RANDOM(random) }
            val expected = values.toList().sortedWith(ORDER)
            val added = KTypeMinHeap(0)
            values.forEach { added.add(it) }
            for (heap in listOf(added, values.toKTypeMinHeap(), ktypeMinHeapOf(*values))) {
                assertEquals(expected, heap.toSortedArray().toList())
                assertEquals(expected, List(n) { heap.removeFirst() })
            }
        }
    }

    @Test
    fun boundaryKeysOrderCorrectly() {
        for (input in listOf(BOUNDARY_KEYS, BOUNDARY_KEYS.reversed(), BOUNDARY_KEYS.shuffled(Random(1)))) {
            val heap = KTypeMinHeap()
            input.forEach { heap.add(it) }
            assertEquals(BOUNDARY_KEYS_IN_HEAP_ORDER, heap.toSortedArray().toList())
            assertEquals(BOUNDARY_KEYS_IN_HEAP_ORDER, input.toKTypeArray().toKTypeMinHeap().toSortedArray().toList())
            for (expected in BOUNDARY_KEYS_IN_HEAP_ORDER) assertEquals(expected, heap.removeFirst())
        }
    }

    @Test
    fun emptyHeapFailsOrGivesTheDefault() {
        val heap = KTypeMinHeap()
        assertFailsWith<NoSuchElementException> { heap.first() }
        assertFailsWith<NoSuchElementException> { heap.removeFirst() }
        assertFailsWith<NoSuchElementException> { heap.replaceFirst(K_ZERO) }
        assertEquals(rank(7), heap.firstOrDefault(rank(7)))
        assertEquals(rank(7), heap.removeFirstOrDefault(rank(7)))
        assertEquals("[]", heap.toString())
        heap.add(rank(1))
        assertEquals(rank(1), heap.firstOrDefault(rank(7)))
        assertEquals(rank(1), heap.removeFirstOrDefault(rank(7)))
        assertTrue(heap.isEmpty())
        assertFalse(heap.isNotEmpty())
    }

    @Test
    fun replaceFirstReturnsTheOldFirstAndKeepsTheOrder() {
        val heap = ktypeMinHeapOf(rank(1), rank(2), rank(3))
        assertEquals(rank(1), heap.replaceFirst(rank(5)))
        assertEquals(rank(2), heap.first())
        assertEquals(3, heap.size)
        assertEquals(rank(2), heap.replaceFirst(rank(0)))
        assertEquals(rank(0), heap.first())
        assertEquals(listOf(rank(0), rank(3), rank(5)), heap.toSortedArray().toList())
    }

    @Test
    fun addAllTakesARangeAndChecksIt() {
        val values = KTypeArray(6) { rank(5 - it) }
        val heap = KTypeMinHeap()
        heap.addAll(values, 1, 4)
        assertEquals(listOf(rank(2), rank(3), rank(4)), heap.toSortedArray().toList())
        for ((from, to) in listOf(-1 to 2, 2 to 7, 3 to 2)) {
            // The range check names the range. Without it, arraycopy would throw a less clear subclass.
            val failure = assertFailsWith<IndexOutOfBoundsException> { heap.addAll(values, from, to) }
            assertEquals(IndexOutOfBoundsException::class, failure::class)
        }
        assertEquals(3, heap.size)
    }

    @Test
    fun addAllSiftsAFewAndRebuildsForMany() {
        // Fewer than the heap holds: each is sifted up. As many or more: the heap is rebuilt.
        for (count in listOf(1, 2, 7, 8, 9, 30)) {
            val heap = KTypeMinHeap(8)
            for (i in 0 until 8) heap.add(rank(10 + i))
            val values = KTypeArray(count) { rank(count - it) }
            heap.addAll(values)
            assertEquals(((1..count) + (10 until 18)).map { rank(it) }.sortedWith(ORDER), heap.toSortedArray().toList())
            val fromHeap = KTypeMinHeap(0)
            for (i in 0 until 8) fromHeap.add(rank(10 + i))
            fromHeap.addAll(values.toKTypeMinHeap())
            assertEquals(heap, fromHeap)
        }
    }

    @Test
    fun addAllOfTheHeapItselfDoublesIt() {
        val heap = ktypeMinHeapOf(rank(2), rank(1))
        heap.addAll(heap)
        assertEquals(listOf(rank(1), rank(1), rank(2), rank(2)), heap.toSortedArray().toList())
    }

    @Test
    fun addingNothingIsNotAChange() {
        val heap = heapOfEight()
        var visits = 0
        heap.forEachUnordered {
            heap.addAll(KTypeArray(0))
            heap.addAll(KTypeArray(3), 1, 1)
            heap.addAll(KTypeMinHeap())
            visits++
        }
        assertEquals(8, visits)
    }

    @Test
    fun capacityControl() {
        assertFailsWith<IllegalArgumentException> { KTypeMinHeap(-1) }
        val heap = KTypeMinHeap(2)
        assertEquals(2, heap.capacity)
        heap.ensureCapacity(2)
        assertEquals(2, heap.capacity)
        heap.ensureCapacity(50)
        assertEquals(50, heap.capacity)
        heap.add(rank(1))
        heap.clear()
        assertEquals(0, heap.size)
        assertEquals(50, heap.capacity)
        assertEquals(DEFAULT_CAPACITY, KTypeMinHeap().capacity)
        // A bulk add grows to fit at once.
        val small = KTypeMinHeap(0)
        small.addAll(KTypeArray(100))
        assertEquals(100, small.size)
        assertTrue(small.capacity >= 100)
        val fits = KTypeMinHeap(10)
        fits.addAll(KTypeArray(10))
        assertEquals(10, fits.capacity)
        fits.addAll(fits.copy())
        assertEquals(20, fits.size)
        // Another heap that just fits does not grow it.
        val room = KTypeMinHeap(20)
        room.addAll(KTypeArray(10))
        room.addAll(room.copy())
        assertEquals(20, room.capacity)
        assertEquals(0, ktypeMinHeapOf().size)
    }

    @Test
    fun copyIsIndependent() {
        val heap = ktypeMinHeapOf(rank(1))
        val copy = heap.copy()
        copy.add(rank(0))
        copy.replaceFirst(rank(9))
        assertEquals(1, heap.size)
        assertEquals(rank(1), heap.first())
    }

    @Test
    fun buildingCopiesTheArray() {
        val values = KTypeArray(3) { rank(3 - it) }
        val heap = values.toKTypeMinHeap()
        val vararg = ktypeMinHeapOf(*values)
        values.fill(rank(9))
        assertEquals(listOf(rank(1), rank(2), rank(3)), heap.toSortedArray().toList())
        assertEquals(heap, vararg)
    }

    private fun heapOfEight(): KTypeMinHeap {
        val heap = KTypeMinHeap()
        for (i in 1..8) heap.add(rank(i))
        return heap
    }

    @Test
    fun forEachUnorderedVisitsEveryValueOnce() {
        val heap = heapOfEight()
        heap.removeFirst()
        val seen = ArrayList<KType>()
        heap.forEachUnordered { seen += it }
        assertEquals((2..8).map { rank(it) }, seen.sortedWith(ORDER))
    }

    @Test
    fun forEachUnorderedDetectsEveryChangeToTheHeap() {
        // Without the check, these skip or repeat values and finish normally.
        val changes = listOf<(KTypeMinHeap) -> Unit>(
            { it.add(rank(9)) },
            { it.addAll(KTypeArray(1)) },
            { it.addAll(KTypeArray(2), 0, 1) },
            { it.addAll(ktypeMinHeapOf(rank(9))) },
            { it.removeFirst() },
            { it.removeFirstOrDefault(K_ZERO) },
            { it.replaceFirst(rank(9)) },
            { it.clear() },
        )
        for (change in changes) {
            val heap = heapOfEight()
            var visits = 0
            assertFailsWith<ConcurrentModificationException> {
                heap.forEachUnordered {
                    visits++
                    if (visits == 2) change(heap)
                }
            }
            assertEquals(2, visits)
        }
    }

    @Test
    fun forEachUnorderedDetectsAChangeOnTheLastValue() {
        val heap = ktypeMinHeapOf(rank(1))
        assertFailsWith<ConcurrentModificationException> { heap.forEachUnordered { heap.removeFirst() } }
    }

    @Test
    fun forEachUnorderedAllowsChangesThatKeepTheValues() {
        val heap = heapOfEight()
        var visits = 0
        heap.forEachUnordered {
            heap.ensureCapacity(100)
            heap.copy().clear()
            heap.toSortedArray()
            visits++
        }
        assertEquals(8, visits)
    }

    @Test
    fun arraysAndText() {
        val heap = ktypeMinHeapOf(rank(3), rank(1), rank(2), rank(1))
        assertEquals(listOf(rank(1), rank(1), rank(2), rank(3)), heap.toKTypeArray().toList().sortedWith(ORDER))
        assertContentEquals(KTypeArray(4) { rank(listOf(1, 1, 2, 3)[it]) }, heap.toSortedArray())
        assertEquals("[${rank(1)}, ${rank(1)}, ${rank(2)}, ${rank(3)}]", heap.toString())
        val builder = StringBuilder(">")
        heap.appendTo(builder)
        assertEquals(">${rank(1)} ${rank(1)} ${rank(2)} ${rank(3)}", builder.toString())
        val separated = StringBuilder()
        ktypeMinHeapOf(rank(1)).appendTo(separated, ", ")
        assertEquals("${rank(1)}", separated.toString())
        // Reading does not change the heap.
        assertEquals(4, heap.size)
        assertEquals(rank(1), heap.first())
    }

    @Test
    fun toKTypeArrayIgnoresTheReservedCapacity() {
        val heap = KTypeMinHeap(100)
        heap.add(rank(1))
        assertEquals(1, heap.toKTypeArray().size)
        assertEquals(1, heap.toSortedArray().size)
    }

    @Test
    fun equalityIgnoresLayoutAndCapacity() {
        val a = ktypeMinHeapOf(rank(1), rank(2), rank(2), rank(3))
        val b = KTypeMinHeap(100)
        for (i in listOf(3, 2, 2, 1)) b.add(rank(i))
        assertEquals(a, a)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertEquals(a, a.copy())
        assertEquals(4, a.size)
    }

    @Test
    fun equalityDetectsDifferences() {
        val a = ktypeMinHeapOf(rank(1), rank(2))
        assertNotEquals(a, ktypeMinHeapOf(rank(1)))
        assertNotEquals(a, ktypeMinHeapOf(rank(1), rank(3)))
        assertNotEquals(ktypeMinHeapOf(rank(1), rank(1)), a)
        assertFalse(a.equals(null))
        assertFalse(a.equals("x"))
        assertNotEquals(a.hashCode(), ktypeMinHeapOf(rank(1), rank(3)).hashCode())
    }

    @Test
    fun hashCodeFollowsTheFormulaAndIgnoresStaleSlots() {
        val heap = ktypeMinHeapOf(rank(1), rank(3), rank(5))
        heap.removeFirst()
        assertEquals(2 + rank(3).hashCode() + rank(5).hashCode(), heap.hashCode())
        assertEquals(ktypeMinHeapOf(rank(3), rank(5)).hashCode(), heap.hashCode())
    }

    //#if K_FLOATING
    @Test
    fun floatingValuesUseTheTotalOrder() {
        assertNotEquals(ktypeMinHeapOf(-0.0.toKType()), ktypeMinHeapOf(0.0.toKType()))
        assertEquals(ktypeMinHeapOf(KType.NaN), ktypeMinHeapOf(KType.NaN))
        assertEquals(ktypeMinHeapOf(KType.NaN).hashCode(), ktypeMinHeapOf(KType.NaN).hashCode())
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
