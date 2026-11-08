// SPDX-License-Identifier: CC0-1.0
// Generated from test-templates/KTypeListDifferentialTest.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Runs random operations on a CharList and an ArrayList together.
 *
 * Expected values come from the model, never from the implementation.
 */
class CharListDifferentialTest {
    /** One operation. `a` and `b` become indices modulo the current size, so any sublist stays valid. */
    private data class Op(val kind: Kind, val a: Int, val b: Int, val value: Char)

    private enum class Kind(val group: Int) {
        ADD(0), INSERT(0), ADD_ALL(0), ADD_ALL_SELF(0), INSERT_ALL(0), INSERT_ALL_SELF(0), PLUS_ASSIGN(0), RESIZE_GROW(0),
        REMOVE_AT(1), REMOVE_FIRST(1), REMOVE_LAST(1), REMOVE_VALUE(1), REMOVE_RANGE(1), SWAP_REMOVE(1), REMOVE_IF(1),
        TRUNCATE(1), CLEAR(1), MINUS_ASSIGN(1),
        SET(2), SORT(2), SORT_RANGE(2), SORT_DESCENDING(2), REVERSE(2), SWAP(2), ROTATE(2), FILL(2), DEDUPE(2),
        SORT_WITH(2), SORT_BY(2), SORT_BY_LONG(2), SORT_BY_DOUBLE(2), NEXT_PERMUTATION(2), COPY_THEN_REPLACE(2), MAP_THEN_REPLACE(2), TRIM(2),
    }

    private fun value(random: Random, domain: ValueDomain, step: Int): Char = when (domain) {
        ValueDomain.SMALL -> random.nextInt(5).toChar()
        ValueDomain.FULL -> random.nextInt().toChar()
        ValueDomain.BOUNDARY -> BOUNDARY_VALUES[random.nextInt(BOUNDARY_VALUES.size)]
        ValueDomain.MONOTONIC -> step.toChar()
    }

    private fun randomOps(seed: Long, count: Int, distribution: Distribution, domain: ValueDomain): List<Op> {
        val random = Random(seed)
        val groups = Kind.entries.groupBy { it.group }
        return List(count) { step ->
            val group = groups.getValue(random.nextWeighted(distribution))
            Op(group[random.nextInt(group.size)], random.nextInt(), random.nextInt(), value(random, domain, step))
        }
    }

    private fun index(raw: Int, size: Int) = Math.floorMod(raw, size)

    private fun apply(op: Op, list: CharList, model: ArrayList<Char>) {
        val n = model.size
        val v = op.value
        when (op.kind) {
            Kind.ADD -> {
                list.add(v)
                model.add(v)
            }
            Kind.INSERT -> {
                val i = index(op.a, n + 1)
                list.insert(i, v)
                model.add(i, v)
            }
            Kind.ADD_ALL -> {
                val values = CharArray(index(op.a, 5)) { v }
                list.addAll(values)
                values.forEach { model.add(it) }
            }
            Kind.ADD_ALL_SELF -> if (n < 500) {
                list.addAll(list)
                model.addAll(model.toList())
            }
            Kind.INSERT_ALL -> {
                val i = index(op.a, n + 1)
                val values = CharArray(index(op.b, 5)) { v }
                list.insertAll(i, values)
                model.addAll(i, values.toList())
            }
            Kind.INSERT_ALL_SELF -> if (n < 500) {
                val i = index(op.a, n + 1)
                list.insertAll(i, list)
                model.addAll(i, model.toList())
            }
            Kind.PLUS_ASSIGN -> {
                list += v
                model += v
            }
            Kind.RESIZE_GROW -> {
                val extra = index(op.a, 5)
                list.resize(n + extra, v)
                repeat(extra) { model.add(v) }
            }
            Kind.REMOVE_AT -> if (n > 0) {
                val i = index(op.a, n)
                assertEquals(model.removeAt(i), list.removeAt(i))
            }
            Kind.REMOVE_FIRST -> if (n > 0) assertEquals(model.removeAt(0), list.removeFirst())
            Kind.REMOVE_LAST -> if (n > 0) assertEquals(model.removeAt(n - 1), list.removeLast())
            Kind.REMOVE_VALUE -> assertEquals(model.remove(v), list.removeValue(v))
            Kind.REMOVE_RANGE -> {
                val from = index(op.a, n + 1)
                val to = from + index(op.b, n - from + 1)
                list.removeRange(from, to)
                model.subList(from, to).clear()
            }
            Kind.SWAP_REMOVE -> if (n > 0) {
                val i = index(op.a, n)
                val expected = model[i]
                model[i] = model[n - 1]
                model.removeAt(n - 1)
                assertEquals(expected, list.swapRemove(i))
            }
            Kind.REMOVE_IF -> {
                val threshold = op.value
                assertEquals(model.removeAll { (it < threshold) }, list.removeIf { (it < threshold) })
            }
            Kind.TRUNCATE -> {
                val size = index(op.a, n + 1)
                list.truncate(size)
                model.subList(size, n).clear()
            }
            Kind.CLEAR -> if (index(op.a, 20) == 0) {
                list.clear()
                model.clear()
            }
            Kind.MINUS_ASSIGN -> {
                val other = charListOf(v, if (n > 0) model[index(op.a, n)] else v)
                list -= other
                model.removeAll(other.toList().toSet())
            }
            Kind.SET -> if (n > 0) {
                val i = index(op.a, n)
                list[i] = v
                model[i] = v
            }
            Kind.SORT -> {
                list.sort()
                model.sort()
            }
            Kind.SORT_RANGE -> {
                val from = index(op.a, n + 1)
                val to = from + index(op.b, n - from + 1)
                list.sort(from, to)
                model.subList(from, to).sort()
            }
            Kind.SORT_DESCENDING -> {
                list.sortDescending()
                model.sortDescending()
            }
            Kind.REVERSE -> {
                list.reverse()
                model.reverse()
            }
            Kind.SWAP -> if (n > 0) {
                val i = index(op.a, n)
                val j = index(op.b, n)
                list.swap(i, j)
                java.util.Collections.swap(model, i, j)
            }
            Kind.ROTATE -> {
                list.rotate(op.a)
                java.util.Collections.rotate(model, op.a)
            }
            Kind.FILL -> {
                list.fill(v)
                java.util.Collections.fill(model, v)
            }
            Kind.DEDUPE -> {
                list.dedupe()
                val kept = model.filterIndexed { i, x -> i == 0 || !(x == model[i - 1]) }
                model.clear()
                model.addAll(kept)
            }
            Kind.SORT_WITH -> {
                list.sortWith { x, y -> y.compareTo(x) }
                model.sortWith { x, y -> y.compareTo(x) }
            }
            Kind.SORT_BY -> {
                list.sortBy { it.code % 3 }
                model.sortBy { it.code % 3 }
            }
            Kind.SORT_BY_LONG -> {
                list.sortByLong { it.code % 3 * Long.MAX_VALUE }
                model.sortBy { it.code % 3 * Long.MAX_VALUE }
            }
            Kind.SORT_BY_DOUBLE -> {
                // Dividing by zero reaches both infinities and NaN.
                list.sortByDouble { it.code % 3 / 0.0 }
                model.sortBy { it.code % 3 / 0.0 }
            }
            Kind.NEXT_PERMUTATION -> if (n <= 6) {
                val expected = nextPermutationOf(model)
                assertEquals(expected != null, list.nextPermutation())
                val next = expected ?: model.sorted()
                model.clear()
                model.addAll(next)
            }
            Kind.COPY_THEN_REPLACE -> {
                val copy = list.copy()
                assertEquals(list, copy)
                if (copy.isNotEmpty()) copy[0] = copy[0]
                list.clear()
                list.addAll(copy)
            }
            Kind.MAP_THEN_REPLACE -> {
                val mapped = list.map { it }
                list.clear()
                list.addAll(mapped)
            }
            Kind.TRIM -> list.trimToSize()
        }
    }

    /** An independent reference: the lexicographically smallest arrangement greater than [values], by brute force. */
    private fun nextPermutationOf(values: List<Char>): List<Char>? {
        var best: List<Char>? = null
        fun visit(prefix: List<Char>, rest: List<Char>) {
            if (rest.isEmpty()) {
                if (compareLists(prefix, values) > 0 && (best == null || compareLists(prefix, best!!) < 0)) best = prefix
                return
            }
            rest.forEachIndexed { i, x -> visit(prefix + x, rest.filterIndexed { j, _ -> j != i }) }
        }
        visit(emptyList(), values)
        return best
    }

    private fun compareLists(a: List<Char>, b: List<Char>): Int {
        for (i in a.indices) {
            val c = a[i].compareTo(b[i])
            if (c != 0) return c
        }
        return 0
    }

    private fun checkInvariants(list: CharList) {
        assertTrue(list.length in 0..list.elements.size, "length ${list.length} capacity ${list.elements.size}")
    }

    private fun checkQueries(op: Op, list: CharList, model: List<Char>) {
        checkInvariants(list)
        assertEquals(model.size, list.size)
        assertEquals(model.isEmpty(), list.isEmpty())
        assertEquals(model.isNotEmpty(), list.isNotEmpty())
        assertEquals(model.lastIndex, list.lastIndex)
        assertEquals(model.indexOf(op.value), list.indexOf(op.value))
        assertEquals(model.lastIndexOf(op.value), list.lastIndexOf(op.value))
        assertEquals(op.value in model, op.value in list)
        if (model.isNotEmpty()) {
            val i = index(op.a, model.size)
            assertEquals(model[i], list[i])
            assertEquals(model.first(), list.first())
            assertEquals(model.last(), list.last())
        }
    }

    private fun checkFully(op: Op, list: CharList, model: List<Char>) {
        assertEquals(model, list.toList())
        assertContentEquals(model, list.toCharArray().toList())
        assertEquals(model.hashCode(), list.hashCode())
        assertEquals(model.toString(), list.toString())
        val iterated = ArrayList<Char>()
        for (x in list) iterated.add(x)
        assertEquals(model, iterated)
        assertEquals(model.count { (it < op.value) }, list.count { (it < op.value) })
        assertEquals(model.distinct(), list.distinct().toList())
        // The standard `minOrNull` on doubles lets NaN win, so compare by the order the list promises.
        assertEquals(model.minWithOrNull(naturalOrder()) ?: op.value, list.minOrDefault(op.value))
        assertEquals(model.maxWithOrNull(naturalOrder()) ?: op.value, list.maxOrDefault(op.value))
        val sorted = model.sorted()
        val sortedList = list.sorted()
        assertEquals(sorted, sortedList.toList())
        assertEquals(sorted.indexOfFirst { !(it < op.value) }.let { if (it < 0) sorted.size else it }, sortedList.lowerBound(op.value))
        assertEquals(sorted.indexOfFirst { (op.value < it) }.let { if (it < 0) sorted.size else it }, sortedList.upperBound(op.value))
        val found = sortedList.binarySearch(op.value)
        if (op.value in sorted) assertEquals(op.value, sortedList[found]) else assertEquals(-sortedList.lowerBound(op.value) - 1, found)
        val indices = list.argsort()
        assertEquals(model.indices.sortedBy { model[it] }, indices.toList())
        assertEquals(model.indices.sortedBy { model[it].code * (1L shl 33) }, list.argsortByLong { it.code * (1L shl 33) }.toList())
        assertEquals(model.indices.sortedBy { model[it].code % 3 / 0.0 }, list.argsortByDouble { it.code % 3 / 0.0 }.toList())
    }

    private fun check(ops: List<Op>, fullCheckEvery: Int) {
        val list = CharList()
        val model = ArrayList<Char>()
        ops.forEachIndexed { step, op ->
            apply(op, list, model)
            checkQueries(op, list, model)
            if (step % fullCheckEvery == 0 || step == ops.size - 1) checkFully(op, list, model)
        }
    }

    @Test
    fun matchesArrayListUnderRandomOperations() {
        for (seed in TestConfig.seeds) {
            for (distribution in Distribution.entries) {
                for (domain in ValueDomain.entries) {
                    val ops = randomOps(seed, TestConfig.operations / 16, distribution, domain)
                    OperationRunner.run("CharList $distribution $domain", seed, ops) { check(it, 50) }
                }
            }
        }
    }

    @Test
    fun everyOperationSequenceUpToLengthThreeMatches() {
        val values = listOf(BOUNDARY_VALUES.first(), BOUNDARY_VALUES.last())
        val ops = Kind.entries.flatMap { kind -> values.map { Op(kind, 1, 2, it) } }
        for (first in ops) for (second in ops) for (third in ops) {
            check(listOf(first, second, third), 1)
        }
    }

    private companion object {
        val BOUNDARY_VALUES = listOf(Char.MIN_VALUE, (-1).toChar(), '\u0000', 1.toChar(), Char.MAX_VALUE)
    }
}
