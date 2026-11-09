// SPDX-License-Identifier: CC0-1.0
// Generated from test-templates/KTypeArrayExtensionsTest.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CharArrayExtensionsTest {
    private fun v(i: Int): Char = i.toChar()

    private fun arrayOf(vararg values: Int): CharArray = CharArray(values.size) { v(values[it]) }

    /** Small values repeat, and random ones cover the whole range of the type. */
    private fun randomArrays(): List<CharArray> {
        val random = Random(1)
        return (0..40).flatMap { n ->
            listOf(
                CharArray(n) { v(random.nextInt(5)) },
                CharArray(n) { random.nextInt().toChar() },
            )
        } + listOf(BOUNDARY.toCharArray(), BOUNDARY.reversed().toCharArray())
    }

    @Test
    fun boundsMatchALinearScan() {
        for (array in randomArrays()) {
            array.sort()
            val probes = array.toList() + BOUNDARY
            for (probe in probes) {
                assertEquals(array.indexOfFirst { NATURAL.compare(it, probe) >= 0 }.let { if (it < 0) array.size else it }, array.lowerBound(probe))
                assertEquals(array.indexOfFirst { NATURAL.compare(it, probe) > 0 }.let { if (it < 0) array.size else it }, array.upperBound(probe))
            }
        }
    }

    @Test
    fun boundsTakeARangeAndCheckIt() {
        val a = arrayOf(1, 2, 2, 3, 5, 8)
        assertEquals(1, a.lowerBound(v(2)))
        assertEquals(3, a.upperBound(v(2)))
        assertEquals(2, a.lowerBound(v(2), 2, 5))
        assertEquals(3, a.upperBound(v(2), 2, 5))
        assertEquals(4, a.lowerBound(v(4), 2, 5))
        assertEquals(5, a.lowerBound(v(9), 2, 5))
        assertEquals(2, a.upperBound(v(0), 2, 5))
        assertEquals(3, a.lowerBound(v(0), 3, 3))
        assertEquals(0, CharArray(0).lowerBound('\u0000'))
        for ((from, to) in listOf(-1 to 2, 2 to 7, 4 to 3)) {
            assertFailsWith<IndexOutOfBoundsException> { a.lowerBound('\u0000', from, to) }
            assertFailsWith<IndexOutOfBoundsException> { a.upperBound('\u0000', from, to) }
        }
    }

    /** Stable order of [keys] ascending, written with the standard library. */
    private fun <T> stableOrder(keys: List<T>, comparator: Comparator<T>): IntArray =
        keys.indices.sortedWith { i, j -> comparator.compare(keys[i], keys[j]) }.toIntArray()

    @Test
    fun argsortIsStableAndFollowsTheTotalOrder() {
        for (array in randomArrays()) {
            val copy = array.copyOf()
            assertContentEquals(stableOrder(array.toList(), NATURAL), array.argsort())
            assertContentEquals(copy, array)
        }
    }

    @Test
    fun argsortByEachKeyType() {
        for (array in randomArrays()) {
            val list = array.toList()
            val intKeys = list.map { it.code % 3 }
            assertContentEquals(stableOrder(intKeys, naturalOrder()), array.argsortBy { it.code % 3 })
            val longKeys = list.map { -it.code.toLong() * 1_000_000_007L }
            assertContentEquals(stableOrder(longKeys, naturalOrder()), array.argsortByLong { -it.code.toLong() * 1_000_000_007L })
            val doubleKeys = list.map { if (it.code % 4 == 0) -0.0 else it.code / 3.0 }
            assertContentEquals(stableOrder(doubleKeys, naturalOrder()), array.argsortByDouble { if (it.code % 4 == 0) -0.0 else it.code / 3.0 })
        }
    }

    @Test
    fun sortByEachKeyTypeIsStable() {
        for (array in randomArrays()) {
            val list = array.toList()
            fun <T : Comparable<T>> expected(keys: List<T>) = stableOrder(keys, naturalOrder()).map { list[it] }
            val byInt = array.copyOf()
            byInt.sortBy { it.code % 3 }
            assertEquals(expected(list.map { it.code % 3 }), byInt.toList())
            val byLong = array.copyOf()
            byLong.sortByLong { -it.code.toLong() }
            assertEquals(expected(list.map { -it.code.toLong() }), byLong.toList())
            val byDouble = array.copyOf()
            byDouble.sortByDouble { (it.code % 5) / 2.0 }
            assertEquals(expected(list.map { (it.code % 5) / 2.0 }), byDouble.toList())
        }
    }

    @Test
    fun sortWithIsStableAndTakesARange() {
        val byLastDigit = CharComparator { a, b -> (a.code and 7).compareTo(b.code and 7) }
        for (array in randomArrays()) {
            val list = array.toList()
            val sorted = array.copyOf()
            sorted.sortWith(byLastDigit)
            assertEquals(list.sortedWith { a, b -> byLastDigit.compare(a, b) }, sorted.toList())
            val part = array.copyOf()
            val from = part.size / 3
            val to = part.size - part.size / 4
            part.sortWith(byLastDigit, from, to)
            assertEquals(list.subList(0, from) + list.subList(from, to).sortedWith { a, b -> byLastDigit.compare(a, b) } + list.subList(to, list.size), part.toList())
        }
        val a = arrayOf(3, 1, 2)
        for ((from, to) in listOf(-1 to 2, 2 to 4, 2 to 1)) {
            assertFailsWith<IndexOutOfBoundsException> { a.sortWith(byLastDigit, from, to) }
        }
        assertContentEquals(arrayOf(3, 1, 2), a)
    }

    @Test
    fun dedupeKeepsTheFirstOfEachRunAtTheFront() {
        for (array in randomArrays()) {
            val expected = array.toList().filterIndexed { i, x -> i == 0 || NATURAL.compare(array[i - 1], x) != 0 }
            val copy = array.copyOf()
            val count = copy.dedupe()
            assertEquals(expected, copy.toList().subList(0, count))
        }
        val sorted = arrayOf(1, 1, 2, 3, 3, 3)
        assertEquals(3, sorted.dedupe())
        assertContentEquals(arrayOf(1, 2, 3), sorted.copyOf(3))
        assertEquals(0, CharArray(0).dedupe())
        assertEquals(1, arrayOf(4).dedupe())
        assertEquals(1, arrayOf(7, 7).dedupe())
        assertEquals(2, arrayOf(7, 8).dedupe())
    }

    @Test
    fun compressRanksTheDistinctValues() {
        for (array in randomArrays()) {
            val copy = array.copyOf()
            val distinct = array.toList().sortedWith(NATURAL).distinctBy { it.toString() }
            val expected = array.map { x -> distinct.indexOfFirst { NATURAL.compare(it, x) == 0 } }
            assertEquals(expected, array.compress().toList())
            assertContentEquals(copy, array)
        }
        assertContentEquals(intArrayOf(1, 0, 2, 0), arrayOf(5, 2, 9, 2).compress())
        assertEquals(0, CharArray(0).compress().size)
    }

    @Test
    fun nextPermutationVisitsEveryPermutationInOrder() {
        for (input in listOf(arrayOf(1, 2, 3, 4), arrayOf(1, 1, 2, 2), arrayOf(3, 3, 3))) {
            val seen = ArrayList<List<Char>>()
            val a = input.copyOf()
            do {
                seen += a.toList()
            } while (a.nextPermutation())
            val expected = permutations(input.toList()).distinct().sortedWith { x, y -> compareLists(x, y) }
            assertEquals(expected, seen)
            // After the last one, it wraps to the first.
            assertContentEquals(input, a)
        }
        val single = arrayOf(1)
        assertFalse(single.nextPermutation())
        assertFalse(CharArray(0).nextPermutation())
        val two = arrayOf(1, 2)
        assertTrue(two.nextPermutation())
        assertContentEquals(arrayOf(2, 1), two)
    }

    private fun permutations(items: List<Char>): List<List<Char>> =
        if (items.size <= 1) listOf(items) else items.indices.flatMap { i -> permutations(items - items[i]).map { listOf(items[i]) + it } }

    private fun compareLists(x: List<Char>, y: List<Char>): Int {
        for (i in x.indices) {
            val c = NATURAL.compare(x[i], y[i])
            if (c != 0) return c
        }
        return 0
    }



    private companion object {
        val NATURAL: Comparator<Char> = naturalOrder()

        val BOUNDARY = listOf(Char.MIN_VALUE, '\u0000', Char.MAX_VALUE)
    }
}
