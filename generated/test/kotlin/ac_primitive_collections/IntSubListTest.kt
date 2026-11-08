// SPDX-License-Identifier: CC0-1.0
// Generated from test-templates/KTypeSubListTest.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class IntSubListTest {
    private fun v(i: Int): Int = i.toInt()
    private fun listOfInts(vararg values: Int) = IntList(values.size) { v(values[it]) }
    private fun ints(vararg values: Int) = values.map { v(it) }

    private data class Op(val kind: Int, val a: Int, val b: Int, val value: Int)

    private fun check(ops: List<Op>, seed: Long) {
        val random = Random(seed)
        val initial = List(40) { random.nextInt(8).toInt() }
        val list = IntList(initial.size) { initial[it] }
        val model = ArrayList(initial)
        var view = list.subList(5, 35)
        var modelView: MutableList<Int> = model.subList(5, 35)
        for (op in ops) {
            val n = modelView.size
            if (n == 0) {
                view = list.subList(0, list.size)
                modelView = model.subList(0, model.size)
                continue
            }
            val i = Math.floorMod(op.a, n)
            val j = Math.floorMod(op.b, n)
            when (op.kind) {
                0 -> {
                    view[i] = op.value
                    modelView[i] = op.value
                }
                1 -> {
                    view.sort()
                    modelView.sort()
                }
                2 -> {
                    view.sortDescending()
                    modelView.sortDescending()
                }
                3 -> {
                    view.reverse()
                    modelView.reverse()
                }
                4 -> {
                    view.fill(op.value)
                    java.util.Collections.fill(modelView, op.value)
                }
                5 -> {
                    view.swap(i, j)
                    java.util.Collections.swap(modelView, i, j)
                }
                6 -> {
                    view.rotate(op.a)
                    java.util.Collections.rotate(modelView, op.a)
                }
                7 -> {
                    view.sortWith { x, y -> y.compareTo(x) }
                    modelView.sortWith { x, y -> y.compareTo(x) }
                }
                8 -> when (Math.floorMod(op.b, 3)) {
                    0 -> {
                        view.sortBy { it % 3 }
                        modelView.sortBy { it % 3 }
                    }
                    1 -> {
                        view.sortByLong { it % 3 * Long.MAX_VALUE }
                        modelView.sortBy { it % 3 * Long.MAX_VALUE }
                    }
                    else -> {
                        view.sortByDouble { it % 3 / 0.0 }
                        modelView.sortBy { it % 3 / 0.0 }
                    }
                }
                9 -> {
                    val from = minOf(i, j)
                    val to = maxOf(i, j)
                    view = view.subList(from, to + 1)
                    modelView = modelView.subList(from, to + 1)
                }
                10 -> if (Math.floorMod(op.b, 5) == 0) {
                    val from = minOf(i, j)
                    val to = maxOf(i, j)
                    view.subList(from, to + 1).clear()
                    modelView.subList(from, to + 1).clear()
                    // A cleared nested view invalidates its siblings and ancestors, as ArrayList's does.
                    view = list.subList(0, list.size)
                    modelView = model.subList(0, model.size)
                } else {
                    view.clear()
                    modelView.clear()
                }
            }
            assertEquals(modelView.toList(), view.toList())
            assertEquals(modelView.size, view.size)
            assertEquals(modelView.indexOf(op.value), view.indexOf(op.value))
            assertEquals(op.value in modelView, op.value in view)
            assertEquals(model.toList(), list.toList())
            if (modelView.isNotEmpty()) {
                assertEquals(modelView.first(), view.first())
                assertEquals(modelView.last(), view.last())
                assertEquals(modelView.minWith(naturalOrder()), view.min())
                assertEquals(modelView.maxWith(naturalOrder()), view.max())
                assertEquals(modelView.sumOf { it.toLong() }, view.sum())
            }
        }
    }

    @Test
    fun matchesArrayListSubList() {
        for (seed in TestConfig.seeds) {
            val random = Random(seed)
            val ops = List(TestConfig.operations / 4) {
                Op(random.nextInt(11), random.nextInt(), random.nextInt(), random.nextInt(8).toInt())
            }
            OperationRunner.run("IntSubList", seed, ops) { check(it, seed) }
        }
    }

    @Test
    fun readsAndWritesPassThrough() {
        val list = listOfInts(1, 2, 3, 4, 5)
        val view = list.subList(1, 4)
        assertEquals(3, view.size)
        assertEquals(2, view.lastIndex)
        assertEquals(0..2, view.indices)
        assertTrue(view.isNotEmpty())
        assertFalse(view.isEmpty())
        assertEquals(v(2), view[0])
        view[0] = v(9)
        assertEquals(v(9), list[1])
        list[2] = v(8)
        assertEquals(v(8), view[1])
        for (bad in intArrayOf(-1, 3)) {
            assertFailsWith<IndexOutOfBoundsException> { view[bad] }
            assertFailsWith<IndexOutOfBoundsException> { view[bad] = v(0) }
            assertFailsWith<IndexOutOfBoundsException> { view.swap(bad, 0) }
            assertFailsWith<IndexOutOfBoundsException> { view.swap(0, bad) }
        }
    }

    @Test
    fun invalidRanges() {
        val list = listOfInts(1, 2, 3)
        assertFailsWith<IndexOutOfBoundsException> { list.subList(-1, 1) }
        assertFailsWith<IndexOutOfBoundsException> { list.subList(0, 4) }
        assertFailsWith<IndexOutOfBoundsException> { list.subList(2, 1) }
        val view = list.subList(1, 3)
        assertFailsWith<IndexOutOfBoundsException> { view.subList(0, 3) }
    }

    @Test
    fun emptyAndFullViews() {
        val list = listOfInts(1, 2)
        val empty = list.subList(1, 1)
        assertTrue(empty.isEmpty())
        assertFalse(empty.isNotEmpty())
        assertFailsWith<NoSuchElementException> { empty.first() }
        assertFailsWith<NoSuchElementException> { empty.last() }
        assertFailsWith<NoSuchElementException> { empty.min() }
        assertFailsWith<NoSuchElementException> { empty.max() }
        assertEquals(0L, empty.sum())
        assertEquals(-1, empty.lastIndex)
        assertEquals("[]", empty.toString())
        empty.rotate(3)
        empty.clear()
        assertEquals(ints(1, 2), list.toList())
        assertEquals(ints(1, 2), list.subList(0, 2).toList())
    }

    @Test
    fun structuralChangeToTheParentInvalidatesTheView() {
        val list = listOfInts(1, 2, 3)
        val uses = listOf<(IntSubList) -> Unit>(
            { it.size }, { it[0] }, { it[0] = v(1) }, { it.first() }, { it.last() }, { it.indexOf(v(1)) },
            { it.binarySearch(v(1)) }, { it.lowerBound(v(1)) }, { it.upperBound(v(1)) }, { it.min() },
            { it.sum() },
            { it.max() }, { it.sort() }, { it.sortDescending() }, { it.sortWith { _, _ -> 0 } }, { it.sortBy { 0 } },
            { it.sortByLong { 0L } }, { it.sortByDouble { 0.0 } },
            { it.reverse() }, { it.shuffle() }, { it.fill(v(0)) }, { it.swap(0, 0) }, { it.rotate(1) }, { it.clear() },
            { it.forEach { } }, { it.iterator().hasNext() }, { it.subList(0, 0) }, { it.toIntList() },
            { it.toIntArray() }, { it.contentEquals(IntList()) }, { it.contentEquals(it) },
            { it.joinToString() }, { it.toString() }, { it.hashCode() },
        )
        for (use in uses) {
            val view = list.subList(0, 2)
            list.add(v(4))
            assertFailsWith<ConcurrentModificationException> { use(view) }
            list.removeLast()
        }
    }

    @Test
    fun valueChangesInTheParentDoNotInvalidate() {
        val list = listOfInts(1, 2, 3)
        val view = list.subList(0, 2)
        list[0] = v(9)
        list.sort(0, 3)
        assertEquals(ints(2, 3), view.toList())
    }

    @Test
    fun clearRemovesFromTheParentAndKeepsTheViewUsable() {
        val list = listOfInts(1, 2, 3, 4)
        val view = list.subList(1, 3)
        view.clear()
        assertEquals(ints(1, 4), list.toList())
        assertEquals(0, view.size)
        assertEquals(emptyList(), view.toList())
        view.clear()
        assertEquals(ints(1, 4), list.toList())
        list.add(v(5))
        assertFailsWith<ConcurrentModificationException> { view.size }
    }

    @Test
    fun nestedViewsPointAtTheParent() {
        val list = IntList(10) { v(it) }
        val outer = list.subList(2, 8)
        val inner = outer.subList(1, 4)
        assertEquals(ints(3, 4, 5), inner.toList())
        assertTrue(inner.parent === list)
        assertEquals(3, inner.offset)
        inner[0] = v(99)
        assertEquals(v(99), list[3])
        assertEquals(v(99), outer[1])
    }

    @Test
    fun search() {
        val list = listOfInts(0, 1, 3, 3, 5, 100)
        val view = list.subList(1, 5)
        assertEquals(1, view.indexOf(v(3)))
        assertEquals(-1, view.indexOf(v(0)))
        assertEquals(-1, view.indexOf(v(100)))
        assertTrue(v(5) in view)
        assertFalse(v(100) in view)
        assertEquals(0, view.lowerBound(v(0)))
        assertEquals(1, view.lowerBound(v(3)))
        assertEquals(3, view.upperBound(v(3)))
        assertEquals(4, view.upperBound(v(100)))
        assertEquals(0, view.binarySearch(v(1)))
        assertEquals(java.util.Collections.binarySearch(view.toList(), v(2)), view.binarySearch(v(2)))
        assertEquals(java.util.Collections.binarySearch(view.toList(), v(0)), view.binarySearch(v(0)))
        assertEquals(java.util.Collections.binarySearch(view.toList(), v(100)), view.binarySearch(v(100)))
    }

    @Test
    fun rearrangementsStayInsideTheView() {
        val list = listOfInts(9, 3, 1, 2, 9)
        val view = list.subList(1, 4)
        view.sort()
        assertEquals(ints(9, 1, 2, 3, 9), list.toList())
        view.sortDescending()
        assertEquals(ints(9, 3, 2, 1, 9), list.toList())
        view.reverse()
        assertEquals(ints(9, 1, 2, 3, 9), list.toList())
        view.rotate(1)
        assertEquals(ints(9, 3, 1, 2, 9), list.toList())
        view.swap(0, 2)
        assertEquals(ints(9, 2, 1, 3, 9), list.toList())
        view.sortWith { a, b -> a.compareTo(b) }
        assertEquals(ints(9, 1, 2, 3, 9), list.toList())
        view.sortBy { -it }
        assertEquals(ints(9, 3, 2, 1, 9), list.toList())
        view.sortByLong { it * (1L shl 40) }
        assertEquals(ints(9, 1, 2, 3, 9), list.toList())
        view.sortByDouble { -it.toDouble() }
        assertEquals(ints(9, 3, 2, 1, 9), list.toList())
        view.shuffle(Random(1))
        assertEquals(ints(1, 2, 3), view.toList().sorted())
        assertEquals(v(9), list[0])
        assertEquals(v(9), list[4])
        view.shuffle()
        view.fill(v(7))
        assertEquals(ints(9, 7, 7, 7, 9), list.toList())
    }

    @Test
    fun shufflePassesThrough() {
        val list = IntList(102) { v(it) }
        val view = list.subList(1, 101)
        val before = view.toList()
        view.shuffle(Random(1))
        assertNotEquals(before, view.toList())
        assertEquals(before, view.toList().sorted())
        assertEquals(v(0), list[0])
        assertEquals(v(101), list[101])
    }

    @Test
    fun iterationAndAggregates() {
        val view = listOfInts(0, 1, 2, 3, 0).subList(1, 4)
        val seen = ArrayList<Int>()
        for (x in view) seen += x
        assertEquals(ints(1, 2, 3), seen)
        seen.clear()
        view.forEach { seen += it }
        assertEquals(ints(1, 2, 3), seen)
        val indexed = ArrayList<Int>()
        view.forEachIndexed { i, x -> indexed += i + x }
        assertEquals(listOf(1, 3, 5), indexed)
        assertEquals(2, view.count { it > 1 })
        assertTrue(view.any { it == v(3) })
        assertFalse(view.any { it == v(0) })
        assertTrue(view.all { it > 0 })
        assertFalse(view.all { it > 1 })
        assertEquals(v(6).toLong(), view.sum())
        assertEquals(v(1), view.min())
        assertEquals(v(3), view.max())
        assertEquals(v(1), view.first())
        assertEquals(v(3), view.last())
    }

    /**
     * Stopping quietly here would give a wrong answer: `all` would say true after seeing one element.
     * Plain tries: inlining through assertFailsWith as well would hide these branches from the coverage tool.
     */
    @Test
    fun clearingTheViewFromALoopFails() {
        fun failsOnClear(use: (IntSubList) -> Unit): Boolean {
            val list = listOfInts(1, 2, 3, 4)
            return try {
                use(list.subList(1, 3))
                false
            } catch (_: ConcurrentModificationException) {
                assertEquals(ints(1, 4), list.toList())
                true
            }
        }
        assertTrue(failsOnClear { view -> view.forEach { view.clear() } })
        assertTrue(failsOnClear { view -> view.forEachIndexed { _, _ -> view.clear() } })
        assertTrue(failsOnClear { view -> view.count { view.clear(); true } })
        assertTrue(failsOnClear { view -> view.any { view.clear(); false } })
        assertTrue(failsOnClear { view -> view.all { view.clear(); true } })
        assertTrue(failsOnClear { view -> for (x in view) view.clear() })
        assertTrue(failsOnClear { view -> view.sortBy { view.clear(); 0 } })
        assertTrue(failsOnClear { view -> view.sortByLong { view.clear(); 0L } })
        assertTrue(failsOnClear { view -> view.sortByDouble { view.clear(); 0.0 } })
    }

    /** Without hasNext, an iterator still has to report the clear rather than an index out of bounds. */
    @Test
    fun anIteratorReportsAClearFromNext() {
        val view = listOfInts(1, 2, 3).subList(0, 3)
        val iterator = view.iterator()
        view.clear()
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
        assertFailsWith<ConcurrentModificationException> { iterator.hasNext() }
    }

    @Test
    fun anIteratorEndsNormallyWithoutChanges() {
        val iterator = listOfInts(1).subList(0, 1).iterator()
        assertTrue(iterator.hasNext())
        assertEquals(v(1), iterator.next())
        assertFalse(iterator.hasNext())
        assertFailsWith<NoSuchElementException> { iterator.next() }
    }

    @Test
    fun changingTheParentFromALoopOverTheViewFails() {
        val loops = listOf<(IntList, IntSubList) -> Unit>(
            { list, view -> view.forEach { list.add(v(9)) } },
            { list, view -> view.forEachIndexed { _, _ -> list.add(v(9)) } },
            { list, view -> for (x in view) list.add(v(9)) },
            { list, view -> for (x in view) list.removeLast() },
        )
        for (loop in loops) {
            val list = listOfInts(1, 2, 3, 4)
            assertFailsWith<ConcurrentModificationException> { loop(list, list.subList(0, 2)) }
        }
    }

    @Test
    fun conversionsAndEquality() {
        val list = listOfInts(0, 1, 2, 0)
        val view = list.subList(1, 3)
        val copy = view.toIntList()
        copy[0] = v(9)
        assertEquals(v(1), view[0])
        assertContentEquals(IntArray(2) { v(it + 1) }, view.toIntArray())
        assertTrue(view.contentEquals(listOfInts(1, 2)))
        assertFalse(view.contentEquals(listOfInts(1, 3)))
        assertFalse(view.contentEquals(listOfInts(1)))
        assertTrue(view.contentEquals(list.subList(1, 3)))
        assertFalse(view.contentEquals(list.subList(0, 2)))
        assertEquals(view, list.subList(1, 3))
        assertEquals(view.hashCode(), listOfInts(1, 2).hashCode())
        assertNotEquals(view, list.subList(0, 2))
        assertFalse(view.equals(listOfInts(1, 2)))
        assertEquals("[${v(1)}, ${v(2)}]", view.toString())
        assertEquals("<${v(1)}|${v(2)}>", view.joinToString("|", "<", ">"))
    }
}
