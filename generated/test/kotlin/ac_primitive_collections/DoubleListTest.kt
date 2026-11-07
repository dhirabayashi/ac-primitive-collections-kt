// SPDX-License-Identifier: CC0-1.0
// Generated from test-templates/KTypeListTest.kt. Do not edit; run ./gradlew generateSources.
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

/** Hand-written expectations. The differential test cannot catch a spec misread that java.util shares. */
class DoubleListTest {
    private fun v(i: Int): Double = i.toDouble()

    /** `Double.MIN_VALUE` is the smallest positive value, so it does not sort first. */
    private val lowest: Double = Double.NEGATIVE_INFINITY

    private fun listOfInts(vararg values: Int) = DoubleList(values.size) { v(values[it]) }
    private fun assertList(list: DoubleList, vararg expected: Int) {
        assertEquals(expected.map { v(it) }, list.toList())
    }

    @Test
    fun constructorArgumentIsACapacity() {
        val list = DoubleList(3)
        assertEquals(0, list.size)
        assertEquals(3, list.capacity)
        assertEquals(DEFAULT_CAPACITY, DoubleList().capacity)
        assertFailsWith<IllegalArgumentException> { DoubleList(-1) }
        assertList(DoubleList(0))
    }

    @Test
    fun factories() {
        assertList(DoubleList(3) { v(it * 2) }, 0, 2, 4)
        // A plain try: inlining through assertFailsWith as well would hide this branch from the coverage tool.
        val negativeSize = try {
            DoubleList(-1) { v(it) }
            false
        } catch (_: IllegalArgumentException) {
            true
        }
        assertTrue(negativeSize)
        assertEquals(3, DoubleList(3) { v(it) }.capacity)
        assertList(doubleListOf(v(1), v(2)), 1, 2)
        assertList(buildDoubleList {
            add(v(1))
            add(v(2))
        }, 1, 2)
        val source = DoubleArray(2) { v(it) }
        val fromArray = source.toDoubleList()
        source[0] = v(9)
        assertList(fromArray, 0, 1)
        assertList(listOf(v(3), v(4)).toDoubleList(), 3, 4)
    }


    @Test
    fun elementAccess() {
        val list = listOfInts(1, 2, 3)
        assertEquals(v(2), list[1])
        list[1] = v(9)
        assertEquals(v(9), list[1])
        list[1] += 1
        assertEquals(v(10), list[1])
        for (bad in intArrayOf(-1, 3)) {
            assertFailsWith<IndexOutOfBoundsException> { list[bad] }
            assertFailsWith<IndexOutOfBoundsException> { list[bad] = v(0) }
        }
    }

    @Test
    fun indexOutsideSizeButInsideCapacityIsRejected() {
        val list = DoubleList(10)
        list.add(v(1))
        assertFailsWith<IndexOutOfBoundsException> { list[1] }
        assertFailsWith<IndexOutOfBoundsException> { list[1] = v(1) }
        assertFailsWith<IndexOutOfBoundsException> { list.swap(0, 1) }
        assertFailsWith<IndexOutOfBoundsException> { list.swap(1, 0) }
        assertFailsWith<IndexOutOfBoundsException> { list.swap(-1, 0) }
        assertFailsWith<IndexOutOfBoundsException> { list.swap(0, -1) }
    }

    /** The array has room past `size`, so without an explicit check these would read stale elements. */
    @Test
    fun rangesPastSizeAreRejectedEvenInsideCapacity() {
        val list = DoubleList(100)
        repeat(3) { list.add(v(it)) }
        val expected = list.toList()
        val modCount = list.modCount
        // Each bad range, including an inverted one that arithmetic alone would not catch.
        for ((from, to) in listOf(0 to 4, 2 to 1, -1 to 1)) {
            val checks = listOf<(DoubleList) -> Unit>(
                { it.addAll(it, from, to) }, { it.addAll(DoubleArray(3), from, to) }, { it.indexOf(v(0), from, to) },
                { it.binarySearch(v(0), from, to) }, { it.lowerBound(v(0), from, to) }, { it.upperBound(v(0), from, to) },
                { it.fill(v(9), from, to) }, { it.indexOfMin(from, to) }, { it.indexOfMax(from, to) }, { it.min(from, to) },
                { it.max(from, to) }, { it.removeRange(from, to) }, { it.reverse(from, to) }, { it.rotate(1, from, to) },
                { it.shuffle(Random(1), from, to) }, { it.slice(from, to) }, { it.sort(from, to) },
                { it.sortDescending(from, to) }, { it.sortWith({ _, _ -> 0 }, from, to) },
                { it.sum(from, to) },
                { it.subList(from, to) },
            )
            for (check in checks) {
                assertFailsWith<IndexOutOfBoundsException> { check(list) }
                // A failed call must leave the list as it was, not just throw.
                assertEquals(expected, list.toList())
                assertEquals(modCount, list.modCount)
            }
        }
        val singleIndexChecks = listOf<(DoubleList) -> Unit>(
            { it.removeAt(3) }, { it.swapRemove(3) }, { it.truncate(4) }, { it.insert(4, v(0)) },
        )
        for (check in singleIndexChecks) {
            assertFailsWith<IndexOutOfBoundsException> { check(list) }
            assertEquals(expected, list.toList())
            assertEquals(modCount, list.modCount)
        }
    }

    @Test
    fun removeAtTheFirstIndexIsAllowed() {
        val list = listOfInts(1, 2)
        assertEquals(v(1), list.removeAt(0))
        assertEquals(v(2), list.swapRemove(0))
    }

    @Test
    fun capacityIsExactWhenItAlreadyFits() {
        val list = DoubleList(4)
        list.ensureCapacity(4)
        assertEquals(4, list.capacity)
        list.addAll(DoubleArray(4))
        assertEquals(4, list.capacity)
        list.add(v(1))
        assertEquals(DEFAULT_CAPACITY, list.capacity)
    }

    @Test
    fun plusAndCopiesKeepEveryElementOfALongList() {
        val long = DoubleList(100) { v(it) }
        val expected = long.toList()
        assertEquals(expected + v(1), (long + v(1)).toList())
        assertEquals(expected + expected, (long + long).toList())
        assertEquals(expected, long.copy().toList())
        assertEquals(expected, long.toDoubleArray().toList())
    }

    @Test
    fun endsOfANonEmptyList() {
        val list = listOfInts(1, 2)
        assertEquals(v(1), list.first())
        assertEquals(v(2), list.last())
        assertEquals(v(1), list.firstOrDefault(v(7)))
        assertEquals(v(2), list.lastOrDefault(v(7)))
        assertEquals(v(1), list.firstOrElse { v(7) })
        assertEquals(v(2), list.lastOrElse { v(7) })
        assertEquals(1, list.lastIndex)
        assertEquals(0..1, list.indices)
    }

    @Test
    fun endsOfAnEmptyList() {
        val list = DoubleList()
        assertFailsWith<NoSuchElementException> { list.first() }
        assertFailsWith<NoSuchElementException> { list.last() }
        assertFailsWith<NoSuchElementException> { list.removeFirst() }
        assertFailsWith<NoSuchElementException> { list.removeLast() }
        assertFailsWith<NoSuchElementException> { list.min() }
        assertFailsWith<NoSuchElementException> { list.max() }
        assertFailsWith<NoSuchElementException> { list.indexOfMin() }
        assertFailsWith<NoSuchElementException> { list.indexOfMax() }
        assertEquals(v(7), list.firstOrDefault(v(7)))
        assertEquals(v(7), list.lastOrDefault(v(7)))
        assertEquals(v(7), list.firstOrElse { v(7) })
        assertEquals(v(7), list.lastOrElse { v(7) })
        assertEquals(v(7), list.minOrDefault(v(7)))
        assertEquals(v(7), list.maxOrDefault(v(7)))
        assertEquals(-1, list.lastIndex)
        assertTrue(list.indices.isEmpty())
        assertEquals(0.0, list.sum())
        assertEquals("[]", list.toString())
        assertFalse(list.nextPermutation())
    }

    @Test
    fun destructuring() {
        val (a, b, c, d, e) = listOfInts(1, 2, 3, 4, 5)
        assertEquals(listOf(v(1), v(2), v(3), v(4), v(5)), listOf(a, b, c, d, e))
        assertFailsWith<IndexOutOfBoundsException> { listOfInts(1).component2() }
    }

    @Test
    fun addGrowsAcrossTheCapacityBoundary() {
        val list = DoubleList(0)
        repeat(1000) { list.add(v(it % 100)) }
        assertEquals(1000, list.size)
        repeat(1000) { assertEquals(v(it % 100), list[it]) }
    }

    @Test
    fun addAllOverloads() {
        val list = DoubleList()
        list.addAll(DoubleArray(2) { v(it) })
        assertList(list, 0, 1)
        list.addAll(DoubleArray(4) { v(it) }, 2, 4)
        assertList(list, 0, 1, 2, 3)
        list.addAll(listOfInts(4))
        assertList(list, 0, 1, 2, 3, 4)
        list.addAll(listOfInts(5, 6, 7), 1, 2)
        assertList(list, 0, 1, 2, 3, 4, 6)
        list.addAll(listOf(v(7)))
        assertList(list, 0, 1, 2, 3, 4, 6, 7)
        assertFailsWith<IndexOutOfBoundsException> { list.addAll(DoubleArray(1), 0, 2) }
        assertFailsWith<IndexOutOfBoundsException> { list.addAll(DoubleList(), 0, 1) }
    }

    @Test
    fun emptyAddAllIsNotAStructuralChange() {
        val list = listOfInts(1)
        val before = list.modCount
        list.addAll(DoubleArray(0))
        list.insertAll(0, DoubleArray(0))
        list.removeRange(0, 0)
        list.truncate(1)
        assertEquals(before, list.modCount)
    }

    @Test
    fun selfReferenceIsSafe() {
        val list = listOfInts(1, 2, 3)
        list.addAll(list)
        assertList(list, 1, 2, 3, 1, 2, 3)
        val growing = DoubleList(2)
        growing.add(v(1))
        growing.add(v(2))
        growing += growing
        assertList(growing, 1, 2, 1, 2)
        val inserted = listOfInts(1, 2, 3)
        inserted.insertAll(1, inserted)
        assertList(inserted, 1, 1, 2, 3, 2, 3)
        val removed = listOfInts(1, 2)
        removed -= removed
        assertList(removed)
        val range = listOfInts(1, 2, 3)
        range.addAll(range, 1, 3)
        assertList(range, 1, 2, 3, 2, 3)
    }

    @Test
    fun insertBoundaries() {
        val list = listOfInts(1)
        list.insert(0, v(0))
        list.insert(2, v(2))
        assertList(list, 0, 1, 2)
        list.insertAll(3, DoubleArray(1) { v(3) })
        list.insertAll(0, listOfInts(9))
        assertList(list, 9, 0, 1, 2, 3)
        for (bad in intArrayOf(-1, 6)) {
            assertFailsWith<IndexOutOfBoundsException> { list.insert(bad, v(0)) }
            assertFailsWith<IndexOutOfBoundsException> { list.insertAll(bad, DoubleArray(0)) }
        }
    }

    @Test
    fun removals() {
        val list = listOfInts(1, 2, 3, 4, 5, 6)
        assertEquals(v(2), list.removeAt(1))
        assertEquals(v(1), list.removeFirst())
        assertEquals(v(6), list.removeLast())
        assertList(list, 3, 4, 5)
        assertEquals(v(3), list.swapRemove(0))
        assertList(list, 5, 4)
        for (bad in intArrayOf(-1, 2)) {
            assertFailsWith<IndexOutOfBoundsException> { list.removeAt(bad) }
            assertFailsWith<IndexOutOfBoundsException> { list.swapRemove(bad) }
        }
        assertFalse(list.removeValue(v(9)))
        assertTrue(list.removeValue(v(5)))
        assertList(list, 4)
        assertFailsWith<IndexOutOfBoundsException> { list.removeRange(0, 2) }
    }

    @Test
    fun removeValueTakesOnlyTheFirstOccurrence() {
        val list = listOfInts(1, 2, 1)
        assertTrue(list.removeValue(v(1)))
        assertList(list, 2, 1)
    }

    @Test
    fun removeIfCompacts() {
        val list = listOfInts(1, 2, 3, 4)
        assertTrue(list.removeIf { it.toInt() % 2 == 0 })
        assertList(list, 1, 3)
        assertFalse(list.removeIf { false })
        assertList(list, 1, 3)
        assertTrue(list.removeIf { true })
        assertList(list)
    }

    @Test
    fun removeIfKeepsUnvisitedElementsWhenThePredicateThrows() {
        val list = listOfInts(1, 2, 3, 4)
        assertFailsWith<IllegalStateException> {
            list.removeIf {
                check(it != v(3))
                it == v(1)
            }
        }
        assertList(list, 2, 3, 4)
    }

    @Test
    fun removeIfKeepsUnvisitedElementsOnNonLocalReturn() {
        val list = listOfInts(1, 2, 3, 4)
        fun run() {
            list.removeIf {
                if (it == v(3)) return
                it == v(1)
            }
        }
        run()
        assertList(list, 2, 3, 4)
    }

    @Test
    fun truncateResizeClear() {
        val list = listOfInts(1, 2, 3)
        list.truncate(2)
        assertList(list, 1, 2)
        assertFailsWith<IndexOutOfBoundsException> { list.truncate(3) }
        assertFailsWith<IndexOutOfBoundsException> { list.truncate(-1) }
        list.resize(4, v(7))
        assertList(list, 1, 2, 7, 7)
        list.resize(1)
        assertList(list, 1)
        list.resize(2)
        assertList(list, 1, 0)
        list.resize(2, v(9))
        assertList(list, 1, 0)
        assertFailsWith<IllegalArgumentException> { list.resize(-1) }
        list.clear()
        assertList(list)
        list.add(v(1))
        assertList(list, 1)
    }

    @Test
    fun capacityControl() {
        val list = DoubleList(0)
        list.ensureCapacity(100)
        assertEquals(100, list.capacity)
        list.ensureCapacity(10)
        assertEquals(100, list.capacity)
        list.add(v(1))
        list.trimToSize()
        assertEquals(1, list.capacity)
        list.trimToSize()
        assertEquals(1, list.capacity)
        assertList(list, 1)
    }

    @Test
    fun search() {
        val list = listOfInts(1, 3, 3, 5)
        assertEquals(1, list.indexOf(v(3)))
        assertEquals(2, list.lastIndexOf(v(3)))
        assertEquals(-1, list.indexOf(v(4)))
        assertEquals(-1, list.lastIndexOf(v(4)))
        assertEquals(2, list.indexOf(v(3), 2, 4))
        assertEquals(-1, list.indexOf(v(3), 3, 4))
        assertTrue(v(3) in list)
        assertTrue(v(4) !in list)
        assertEquals(1, list.lowerBound(v(3)))
        assertEquals(3, list.upperBound(v(3)))
        assertEquals(0, list.lowerBound(v(0)))
        assertEquals(4, list.upperBound(v(6)))
        assertEquals(3, list.lowerBound(v(4)))
        assertEquals(3, list.upperBound(v(4)))
        assertEquals(2, list.lowerBound(v(3), 2, 4))
        assertEquals(3, list.upperBound(v(3), 2, 4))
        assertEquals(0, list.binarySearch(v(1)))
        assertEquals(-4, list.binarySearch(v(4)))
        assertEquals(3, list.binarySearch(v(5), 2, 4))
        assertEquals(1, list.indexOfFirst { it == v(3) })
        assertEquals(2, list.indexOfLast { it == v(3) })
        assertEquals(-1, list.indexOfFirst { it == v(9) })
        assertEquals(-1, list.indexOfLast { it == v(9) })
        assertFailsWith<IndexOutOfBoundsException> { list.indexOf(v(1), 0, 5) }
        assertFailsWith<IndexOutOfBoundsException> { list.binarySearch(v(1), 0, 5) }
        assertFailsWith<IndexOutOfBoundsException> { list.lowerBound(v(1), 0, 5) }
        assertFailsWith<IndexOutOfBoundsException> { list.upperBound(v(1), 0, 5) }
    }

    @Test
    fun searchRespectsSizeNotCapacity() {
        val list = listOfInts(1, 2, 3)
        list.truncate(2)
        assertEquals(-1, list.indexOf(v(3)))
        assertEquals(-1, list.lastIndexOf(v(3)))
        assertEquals(2, list.lowerBound(v(3)))
        assertFalse(v(3) in list)
        assertEquals(v(3), list.sum())
        assertEquals(v(2), list.max())
    }

    @Test
    fun rearrangements() {
        val list = listOfInts(3, 1, 2)
        list.sort()
        assertList(list, 1, 2, 3)
        list.sortDescending()
        assertList(list, 3, 2, 1)
        list.reverse()
        assertList(list, 1, 2, 3)
        list.swap(0, 2)
        assertList(list, 3, 2, 1)
        list.rotate(1)
        assertList(list, 1, 3, 2)
        list.rotate(-1)
        assertList(list, 3, 2, 1)
        list.rotate(3)
        assertList(list, 3, 2, 1)
        list.rotate(Int.MIN_VALUE)
        assertEquals(3, list.size)
        list.fill(v(7))
        assertList(list, 7, 7, 7)
        val empty = DoubleList()
        empty.rotate(5)
        empty.sort()
        empty.reverse()
        empty.shuffle()
        empty.dedupe()
        assertList(empty)
    }

    @Test
    fun rangeOverloads() {
        val list = listOfInts(5, 4, 3, 2, 1)
        list.sort(1, 4)
        assertList(list, 5, 2, 3, 4, 1)
        list.sortDescending(1, 4)
        assertList(list, 5, 4, 3, 2, 1)
        list.reverse(0, 2)
        assertList(list, 4, 5, 3, 2, 1)
        list.rotate(1, 0, 3)
        assertList(list, 3, 4, 5, 2, 1)
        list.fill(v(0), 3, 5)
        assertList(list, 3, 4, 5, 0, 0)
        assertEquals(v(9), list.sum(1, 3))
        assertEquals(v(4), list.min(1, 3))
        assertEquals(v(5), list.max(1, 3))
        for (bad in listOf<(DoubleList) -> Unit>(
            { it.sort(0, 6) }, { it.sortDescending(0, 6) }, { it.reverse(0, 6) },
            { it.rotate(1, 0, 6) }, { it.fill(v(0), 0, 6) }, { it.min(0, 6) }, { it.max(0, 6) },
            { it.sum(0, 6) },
            { it.shuffle(Random(1), 0, 6) },
        )) {
            assertFailsWith<IndexOutOfBoundsException> { bad(list) }
        }
        assertFailsWith<NoSuchElementException> { list.min(1, 1) }
        assertFailsWith<NoSuchElementException> { list.max(1, 1) }
    }

    @Test
    fun rangeOverloadsStayInsideTheRange() {
        val list = listOfInts(1, 2, 3, 4, 5, 6, 7, 8)
        list.shuffle(Random(1), 2, 6)
        assertList(list.slice(0, 2), 1, 2)
        assertList(list.slice(6, 8), 7, 8)
        assertEquals(listOf(3, 4, 5, 6).map { v(it) }, list.slice(2, 6).sorted().toList())
    }

    @Test
    fun shuffleIsAPermutationAndUsesTheRandom() {
        val list = DoubleList(100) { v(it) }
        val copy = list.copy()
        list.shuffle(Random(1))
        assertNotEquals(copy, list)
        assertEquals(copy, list.sorted())
        val again = copy.copy()
        again.shuffle(Random(1))
        assertEquals(list, again)
    }

    @Test
    fun shuffleReachesEveryArrangement() {
        val seen = HashSet<List<Double>>()
        val random = Random(1)
        repeat(1000) {
            val list = listOfInts(1, 2, 3)
            list.shuffle(random)
            seen += list.toList()
        }
        assertEquals(6, seen.size)
    }

    @Test
    fun dedupeRemovesOnlyAdjacentDuplicates() {
        val list = listOfInts(1, 1, 2, 1, 1, 1, 3, 3)
        list.dedupe()
        assertList(list, 1, 2, 1, 3)
        val single = listOfInts(1)
        single.dedupe()
        assertList(single, 1)
    }

    @Test
    fun distinctKeepsFirstOccurrences() {
        assertList(listOfInts(3, 1, 3, 2, 1).distinct(), 3, 1, 2)
        assertList(DoubleList().distinct())
    }

    @Test
    fun nextPermutationVisitsAllInOrder() {
        val list = listOfInts(1, 2, 3)
        val seen = mutableListOf(list.toList())
        while (list.nextPermutation()) seen += list.toList()
        assertEquals(
            listOf(
                listOf(1, 2, 3), listOf(1, 3, 2), listOf(2, 1, 3),
                listOf(2, 3, 1), listOf(3, 1, 2), listOf(3, 2, 1),
            ).map { row -> row.map { v(it) } },
            seen,
        )
        assertList(list, 1, 2, 3)
        val one = listOfInts(1)
        assertFalse(one.nextPermutation())
        assertList(one, 1)
        val duplicates = listOfInts(1, 1, 2)
        var count = 1
        while (duplicates.nextPermutation()) count++
        assertEquals(3, count)
    }

    @Test
    fun allPermutationsSort() {
        val base = listOfInts(0, 1, 2, 3, 4, 5, 6)
        val expected = base.toList()
        val descending = expected.reversed()
        val permutation = base.copy()
        do {
            val natural = permutation.copy()
            natural.sort()
            assertEquals(expected, natural.toList())
            val comparator = permutation.copy()
            comparator.sortWith { a, b -> a.compareTo(b) }
            assertEquals(expected, comparator.toList())
            val reversed = permutation.copy()
            reversed.sortWith { a, b -> b.compareTo(a) }
            assertEquals(descending, reversed.toList())
            val by = permutation.copy()
            by.sortBy { -it.toInt() }
            assertEquals(descending, by.toList())
        } while (permutation.nextPermutation())
    }

    private fun sortPatterns(n: Int): List<IntArray> {
        val random = Random(n)
        return listOf(
            IntArray(n) { it },
            IntArray(n) { n - it },
            IntArray(n) { 7 },
            IntArray(n) { it % 16 },
            IntArray(n) { minOf(it, n - it) },
            IntArray(n) { random.nextInt() },
            IntArray(n) { random.nextInt(4) },
        )
    }

    @Test
    fun comparatorSortMatchesAndIsStableOnPatterns() {
        for (n in intArrayOf(0, 1, 2, 7, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65, 100, 1000, 4097, 20000)) {
            for (pattern in sortPatterns(n)) {
                val list = DoubleList(n) { v(pattern[it]) }
                val expected = list.toList().sorted()
                val natural = list.copy()
                natural.sortWith { a, b -> a.compareTo(b) }
                assertEquals(expected, natural.toList(), "n=$n")
                // Sort by the low bits only, and check against the stable standard sort.
                val byLowBits = list.copy()
                byLowBits.sortWith { a, b -> (a.toInt() and 3).compareTo(b.toInt() and 3) }
                assertEquals(list.toList().sortedBy { it.toInt() and 3 }, byLowBits.toList(), "n=$n")
                val keyed = list.copy()
                keyed.sortBy { it.toInt() and 3 }
                assertEquals(list.toList().sortedBy { it.toInt() and 3 }, keyed.toList(), "n=$n")
                assertEquals(
                    list.toList().indices.sortedBy { list[it] },
                    list.argsort().toList(),
                    "n=$n",
                )
                assertEquals(
                    list.toList().indices.sortedBy { list[it].toInt() and 3 },
                    list.argsortBy { it.toInt() and 3 }.toList(),
                    "n=$n",
                )
            }
        }
    }

    @Test
    fun comparatorSortStaysInBoundsWithABrokenComparator() {
        val random = Random(1)
        for (n in intArrayOf(2, 9, 64, 1000)) {
            val list = DoubleList(n) { v(it) }
            val expected = list.toList()
            list.sortWith { _, _ -> random.nextInt(3) - 1 }
            assertEquals(expected, list.sorted().toList())
            list.sortWith { _, _ -> 1 }
            assertEquals(expected, list.sorted().toList())
            list.sortWith { _, _ -> -1 }
            assertEquals(expected, list.sorted().toList())
        }
    }

    @Test
    fun comparatorRangeSort() {
        val list = DoubleList(40) { v(40 - it) }
        list.sortWith({ a, b -> a.compareTo(b) }, 5, 30)
        assertEquals(
            (1..40).reversed().toList().subList(0, 5) + (11..35).toList() + (1..10).reversed().toList(),
            list.toList().map { it.toInt() },
        )
        assertFailsWith<IndexOutOfBoundsException> { list.sortWith({ _, _ -> 0 }, 0, 41) }
    }

    @Test
    fun comparatorThatChangesTheListIsDetected() {
        val list = listOfInts(2, 1)
        assertFailsWith<ConcurrentModificationException> {
            list.sortWith { a, b ->
                list.add(v(0))
                a.compareTo(b)
            }
        }
    }

    @Test
    fun argsortOrdersExtremeKeysAndKeepsTies() {
        val list = DoubleList(4) { i -> listOf(Double.MAX_VALUE, lowest, v(1), lowest)[i] }
        assertContentEquals(intArrayOf(1, 3, 2, 0), list.argsort())
    }

    @Test
    fun sortByLongOrdersExtremeKeysAndKeepsTies() {
        val keys = longArrayOf(Long.MAX_VALUE, Long.MIN_VALUE, -1L, 1L shl 40, -1L, 0L)
        val list = listOfInts(0, 1, 2, 3, 4, 5)
        val expected = listOf(1, 2, 4, 5, 3, 0)
        assertEquals(expected, list.argsortByLong { keys[it.toInt()] }.toList())
        list.sortByLong { keys[it.toInt()] }
        assertEquals(expected.map { v(it) }, list.toList())
        assertTrue(DoubleList().argsortByLong { 0L }.isEmpty())
    }

    @Test
    fun sortByDoubleUsesTheOrderOfCompareTo() {
        val keys = doubleArrayOf(Double.NaN, 0.0, -0.0, Double.NEGATIVE_INFINITY, 1.5, Double.POSITIVE_INFINITY, -1.5, 0.0)
        val list = listOfInts(0, 1, 2, 3, 4, 5, 6, 7)
        val expected = listOf(3, 6, 2, 1, 7, 4, 5, 0)
        assertEquals(expected, list.argsortByDouble { keys[it.toInt()] }.toList())
        list.sortByDouble { keys[it.toInt()] }
        assertEquals(expected.map { v(it) }, list.toList())
    }

    /** The size alone would end this loop as if it had finished. */
    @Test
    fun clearingTheListFromAForInLoopFails() {
        val list = listOfInts(1, 2, 3)
        assertFailsWith<ConcurrentModificationException> { for (x in list) list.clear() }
        val cleared = listOfInts(1, 2)
        val withoutHasNext = cleared.iterator()
        cleared.clear()
        assertFailsWith<ConcurrentModificationException> { withoutHasNext.next() }
        val iterator = listOfInts(1, 2).iterator()
        assertTrue(iterator.hasNext())
        assertEquals(v(1), iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals(v(2), iterator.next())
        assertFalse(iterator.hasNext())
    }

    /** Plain tries: inlining through assertFailsWith as well would hide these branches from the coverage tool. */
    @Test
    fun keySelectorsCannotChangeTheList() {
        fun failsOnChange(sort: (DoubleList) -> Unit): Boolean {
            val list = listOfInts(1, 2)
            return try {
                sort(list)
                false
            } catch (_: ConcurrentModificationException) {
                true
            }
        }
        assertTrue(failsOnChange { list -> list.sortByLong { list.clear(); 0L } })
        assertTrue(failsOnChange { list -> list.sortByDouble { list.clear(); 0.0 } })
        assertTrue(failsOnChange { list -> list.argsortByLong { list.clear(); 0L } })
        assertTrue(failsOnChange { list -> list.argsortByDouble { list.clear(); 0.0 } })
    }

    @Test
    fun everythingUsesTheTotalOrder() {
        val zero = 0.0
        val negativeZero = -0.0
        val nan = Double.NaN
        val list = doubleListOf(nan, zero, Double.POSITIVE_INFINITY, negativeZero, Double.NEGATIVE_INFINITY)
        val sorted = listOf(Double.NEGATIVE_INFINITY, negativeZero, zero, Double.POSITIVE_INFINITY, nan)
        assertEquals(sorted, list.sorted().toList())
        assertEquals(sorted, list.copy().also { it.sortWith { a, b -> a.compareTo(b) } }.toList())
        assertEquals(listOf(4, 3, 1, 2, 0), list.argsort().toList())
        assertEquals(nan, list.max())
        assertEquals(Double.NEGATIVE_INFINITY, list.min())
        assertEquals(0, list.indexOf(nan))
        assertEquals(3, list.indexOf(negativeZero))
        assertEquals(1, list.indexOf(zero))
        assertTrue(nan in list)
        assertEquals(2, doubleListOf(zero, negativeZero).distinct().size)
        assertEquals(1, doubleListOf(nan, nan).distinct().size)
        val adjacent = doubleListOf(zero, negativeZero, nan, nan)
        adjacent.dedupe()
        assertEquals(listOf(zero, negativeZero, nan), adjacent.toList())
        assertNotEquals(doubleListOf(zero), doubleListOf(negativeZero))
        assertEquals(doubleListOf(nan), doubleListOf(nan))
        assertEquals(0, list.sorted().lowerBound(Double.NEGATIVE_INFINITY))
        assertEquals(2, list.sorted().lowerBound(zero))
        assertEquals(4, list.sorted().lowerBound(nan))
        assertEquals(5, list.sorted().upperBound(nan))
        assertTrue(doubleListOf(nan, zero).sum().isNaN())
    }

    @Test
    fun sumOfTheElementType() {
        assertEquals(v(6), listOfInts(1, 2, 3).sum())
    }

    @Test
    fun iteration() {
        val list = listOfInts(1, 2, 3)
        val forward = ArrayList<Double>()
        for (x in list) forward += x
        assertEquals(listOf(v(1), v(2), v(3)), forward)
        val each = ArrayList<Double>()
        list.forEach { each += it }
        assertEquals(forward, each)
        val indexed = ArrayList<Pair<Int, Double>>()
        list.forEachIndexed { i, x -> indexed += i to x }
        assertEquals(listOf(0 to v(1), 1 to v(2), 2 to v(3)), indexed)
        val reversed = ArrayList<Double>()
        list.forEachReversed { reversed += it }
        assertEquals(forward.reversed(), reversed)
        val iterator = list.iterator()
        repeat(3) { iterator.nextDouble() }
        assertFalse(iterator.hasNext())
        assertFailsWith<NoSuchElementException> { iterator.nextDouble() }
        val indices = ArrayList<Int>()
        for (i in list.indices) indices += i
        assertEquals(listOf(0, 1, 2), indices)
    }

    @Test
    fun structuralChangesDuringIterationFail() {
        val list = listOfInts(1, 2, 3)
        assertFailsWith<ConcurrentModificationException> { for (x in list) list.add(x) }
        assertFailsWith<ConcurrentModificationException> { list.forEach { list.add(it) } }
        assertFailsWith<ConcurrentModificationException> { list.forEachIndexed { _, x -> list.add(x) } }
        assertFailsWith<ConcurrentModificationException> { list.forEachReversed { list.add(it) } }
        assertFailsWith<ConcurrentModificationException> { list.indexOfLast { list.add(it); false } }
        assertFailsWith<ConcurrentModificationException> { list.removeIf { list.add(it); false } }
        assertFailsWith<ConcurrentModificationException> { list.fold(0) { acc, x -> list.add(x); acc } }
        val before = list.toList()
        list.forEach { list[0] = list[0] }
        assertEquals(before, list.toList())
    }

    @Test
    fun higherOrder() {
        val list = listOfInts(1, 2, 3, 4)
        assertList(list.map { v(it.toInt() * 2) }, 2, 4, 6, 8)
        assertList(list.filter { it.toInt() % 2 == 0 }, 2, 4)
        assertTrue(list.any { it == v(4) })
        assertFalse(list.any { it == v(5) })
        assertTrue(list.all { it.toInt() > 0 })
        assertFalse(list.all { it.toInt() > 1 })
        assertTrue(list.none { it == v(5) })
        assertFalse(list.none { it == v(4) })
        assertEquals(2, list.count { it.toInt() > 2 })
        assertEquals(10, list.fold(0) { acc, x -> acc + x.toInt() })
        assertEquals(11L, list.fold(1L) { acc, x -> acc + x.toInt().toLong() })
        assertEquals(10.5, list.fold(0.5) { acc, x -> acc + x.toInt().toDouble() })
        assertEquals(20L, list.sumOf { it.toInt().toLong() * 2 })
    }

    @Test
    fun aggregates() {
        val list = listOfInts(3, 1, 4, 1, 5, 5)
        assertEquals(v(19), list.sum())
        assertEquals(v(1), list.min())
        assertEquals(v(5), list.max())
        assertEquals(v(1), list.minOrDefault(v(9)))
        assertEquals(v(5), list.maxOrDefault(v(9)))
        assertEquals(1, list.indexOfMin())
        assertEquals(4, list.indexOfMax())
    }

    @Test
    fun aggregatesOfBoundaryValues() {
        val list = DoubleList(3) { i -> listOf(lowest, v(1), Double.MAX_VALUE)[i] }
        assertEquals(lowest, list.min())
        assertEquals(Double.MAX_VALUE, list.max())
    }


    @Test
    fun operatorsCopy() {
        val a = listOfInts(1, 2, 1)
        assertList(a + v(3), 1, 2, 1, 3)
        assertList(a + listOfInts(4, 5), 1, 2, 1, 4, 5)
        assertList(a - v(1), 2, 1)
        assertList(a - v(9), 1, 2, 1)
        assertList(a - listOfInts(1), 2)
        assertList(a, 1, 2, 1)
        assertList(a + a, 1, 2, 1, 1, 2, 1)
        assertList(DoubleList(0) + v(1), 1)
    }

    @Test
    fun operatorsInPlace() {
        val a = listOfInts(1)
        a += v(2)
        a += listOfInts(3, 1)
        assertList(a, 1, 2, 3, 1)
        a -= v(1)
        assertList(a, 2, 3, 1)
        a -= listOfInts(1, 3, 9)
        assertList(a, 2)
    }

    @Test
    fun plusResultIsIndependent() {
        val a = listOfInts(1)
        val b = a + v(2)
        b[0] = v(9)
        assertList(a, 1)
    }

    @Test
    fun copies() {
        val a = listOfInts(1, 2, 3, 4)
        val copy = a.copy()
        copy[0] = v(9)
        copy.add(v(5))
        assertList(a, 1, 2, 3, 4)
        assertList(a.slice(1, 3), 2, 3)
        assertList(a.slice(2, 2))
        assertFailsWith<IndexOutOfBoundsException> { a.slice(0, 5) }
        assertList(a.take(2), 1, 2)
        assertList(a.take(9), 1, 2, 3, 4)
        assertList(a.take(0))
        assertList(a.drop(1), 2, 3, 4)
        assertList(a.drop(9))
        assertList(a.drop(0), 1, 2, 3, 4)
        assertFailsWith<IllegalArgumentException> { a.take(-1) }
        assertFailsWith<IllegalArgumentException> { a.drop(-1) }
        assertList(listOfInts(2, 3, 1).sorted(), 1, 2, 3)
        assertList(listOfInts(2, 3, 1).sortedDescending(), 3, 2, 1)
        assertList(listOfInts(2, 3, 1).reversed(), 1, 3, 2)
        val source = listOfInts(2, 1)
        source.sorted()
        source.sortedDescending()
        source.reversed()
        assertList(source, 2, 1)
        assertContentEquals(DoubleArray(2) { v(2 - it) }, source.toDoubleArray())
    }

    @Test
    fun copiesWorkAfterGrowingThroughThem() {
        val empty = DoubleList(0)
        val copy = empty.copy()
        copy.add(v(1))
        assertList(copy, 1)
        val slice = listOfInts(1, 2).slice(0, 0)
        slice.add(v(1))
        assertList(slice, 1)
    }

    @Test
    fun conversionsAreCopies() {
        val a = listOfInts(1, 2)
        val standard = a.toMutableList()
        standard[0] = v(9)
        assertList(a, 1, 2)
        a[1] = v(8)
        assertEquals(listOf(v(9), v(2)), standard)
        assertEquals(listOf(v(1), v(8)), a.toList())
    }

    @Test
    fun text() {
        val a = listOfInts(1, 2)
        val one = v(1).toString()
        val two = v(2).toString()
        assertEquals("[$one, $two]", a.toString())
        assertEquals("$one, $two", a.joinToString())
        assertEquals("<$one|$two>", a.joinToString("|", "<", ">"))
        val builder = StringBuilder("x")
        a.appendTo(builder)
        assertEquals("x$one $two", builder.toString())
        builder.clear()
        a.appendTo(builder, ",")
        assertEquals("$one,$two", builder.toString())
    }

    @Test
    fun equalityIsOnlyWithTheSameType() {
        val a = listOfInts(1, 2)
        assertEquals(a, a)
        assertEquals(a, listOfInts(1, 2))
        assertEquals(a.hashCode(), listOfInts(1, 2).hashCode())
        assertEquals(a.toList().hashCode(), a.hashCode())
        assertNotEquals(a, listOfInts(1, 3))
        assertNotEquals(a, listOfInts(1))
        assertFalse(a.equals(a.toList()))
        assertFalse(a.equals(null))
    }

    @Test
    fun equalityIgnoresCapacityAndStaleElements() {
        val a = listOfInts(1, 2, 3)
        a.truncate(2)
        val b = DoubleList(100)
        b.add(v(1))
        b.add(v(2))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
