// SPDX-License-Identifier: CC0-1.0
// Generated from test-templates/KTypeDequeDifferentialTest.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Runs random operations on a IntDeque and an ArrayList together.
 *
 * Expected values come from the model, never from the implementation.
 */
class IntDequeDifferentialTest {
    /** One operation. `a` becomes an index modulo the current size. */
    private data class Op(val kind: Kind, val a: Int, val value: Int)

    private enum class Kind(val group: Int) {
        ADD_FIRST(0), ADD_LAST(0), ADD(0), PLUS_ASSIGN(0), ADD_ALL(0), ADD_ALL_RANGE(0), ADD_ALL_DEQUE(0), ADD_ALL_SELF(0),
        REMOVE_FIRST(1), REMOVE_LAST(1), REMOVE_AT(1), REMOVE_VALUE(1), MINUS_ASSIGN(1), CLEAR(1),
        SET(2), ROTATE(2), ENSURE_CAPACITY(2), COPY_THEN_SWAP(2), PLUS_THEN_SWAP(2),
    }

    private fun value(random: Random, domain: ValueDomain, step: Int): Int = when (domain) {
        ValueDomain.SMALL -> random.nextInt(5).toInt()
        ValueDomain.FULL -> random.nextInt()
        ValueDomain.BOUNDARY -> BOUNDARY_VALUES[random.nextInt(BOUNDARY_VALUES.size)]
        ValueDomain.MONOTONIC -> step.toInt()
    }

    private fun randomOps(seed: Long, count: Int, distribution: Distribution, domain: ValueDomain): List<Op> {
        val random = Random(seed)
        val groups = Kind.entries.groupBy { it.group }
        return List(count) { step ->
            val group = groups.getValue(random.nextWeighted(distribution))
            Op(group[random.nextInt(group.size)], random.nextInt(), value(random, domain, step))
        }
    }

    private fun index(raw: Int, size: Int) = Math.floorMod(raw, size)

    /** Returns the deque to continue with, which some operations replace. */
    private fun apply(op: Op, deque: IntDeque, model: ArrayList<Int>): IntDeque {
        val n = model.size
        val v = op.value
        when (op.kind) {
            Kind.ADD_FIRST -> {
                deque.addFirst(v)
                model.add(0, v)
            }
            Kind.ADD_LAST -> {
                deque.addLast(v)
                model.add(v)
            }
            Kind.ADD -> {
                deque.add(v)
                model.add(v)
            }
            Kind.PLUS_ASSIGN -> {
                deque += v
                model += v
            }
            Kind.ADD_ALL -> {
                val values = IntArray(index(op.a, 5)) { v }
                deque.addAll(values)
                values.forEach { model.add(it) }
            }
            Kind.ADD_ALL_RANGE -> {
                val values = IntArray(5) { if (it % 2 == 0) v else 0 }
                val from = index(op.a, 6)
                val to = from + index(op.a / 6, 6 - from)
                deque.addAll(values, from, to)
                for (i in from until to) model.add(values[i])
            }
            Kind.ADD_ALL_DEQUE -> {
                val other = intDequeOf(v, 0)
                other.addFirst(v)
                deque += other
                model.addAll(listOf(v, v, 0))
            }
            Kind.ADD_ALL_SELF -> if (n < 500) {
                deque.addAll(deque)
                model.addAll(model.toList())
            }
            Kind.REMOVE_FIRST -> if (n > 0) assertEquals(model.removeAt(0), deque.removeFirst())
            Kind.REMOVE_LAST -> if (n > 0) assertEquals(model.removeAt(n - 1), deque.removeLast())
            Kind.REMOVE_AT -> if (n > 0) {
                val i = index(op.a, n)
                assertEquals(model.removeAt(i), deque.removeAt(i))
            }
            Kind.REMOVE_VALUE -> assertEquals(model.remove(v), deque.removeValue(v))
            Kind.MINUS_ASSIGN -> {
                val other = intDequeOf(v, if (n > 0) model[index(op.a, n)] else v)
                deque -= other
                model.removeAll(other.toList().toSet())
            }
            Kind.CLEAR -> if (index(op.a, 20) == 0) {
                deque.clear()
                model.clear()
            }
            Kind.SET -> if (n > 0) {
                val i = index(op.a, n)
                deque[i] = v
                model[i] = v
            }
            Kind.ROTATE -> {
                deque.rotate(op.a)
                java.util.Collections.rotate(model, op.a)
            }
            Kind.ENSURE_CAPACITY -> deque.ensureCapacity(n + index(op.a, 40))
            Kind.COPY_THEN_SWAP -> {
                val copy = deque.copy()
                assertEquals(deque, copy)
                return copy
            }
            Kind.PLUS_THEN_SWAP -> {
                val result = deque + v
                model.add(v)
                return result
            }
        }
        return deque
    }

    private fun checkInvariants(deque: IntDeque) {
        val capacity = deque.elements.size
        assertEquals(0, capacity and (capacity - 1), "capacity $capacity is not a power of two")
        assertTrue(deque.length in 0..capacity, "length ${deque.length} capacity $capacity")
        assertTrue(deque.head in 0..maxOf(capacity - 1, 0), "head ${deque.head} capacity $capacity")
    }

    private fun checkQueries(op: Op, deque: IntDeque, model: List<Int>) {
        checkInvariants(deque)
        assertEquals(model.size, deque.size)
        assertEquals(model.isEmpty(), deque.isEmpty())
        assertEquals(model.isNotEmpty(), deque.isNotEmpty())
        assertEquals(model.lastIndex, deque.lastIndex)
        assertEquals(model.indexOf(op.value), deque.indexOf(op.value))
        assertEquals(op.value in model, op.value in deque)
        assertEquals(model.firstOrNull() ?: op.value, deque.firstOrDefault(op.value))
        assertEquals(model.lastOrNull() ?: op.value, deque.lastOrDefault(op.value))
        if (model.isNotEmpty()) {
            val i = index(op.a, model.size)
            assertEquals(model[i], deque[i])
            assertEquals(model.first(), deque.first())
            assertEquals(model.last(), deque.last())
        }
    }

    private fun checkFully(deque: IntDeque, model: List<Int>) {
        assertEquals(model, deque.toList())
        assertContentEquals(model, deque.toIntArray().toList())
        assertEquals(model.hashCode(), deque.hashCode())
        assertEquals(model.toString(), deque.toString())
        val iterated = ArrayList<Int>()
        for (x in deque) iterated.add(x)
        assertEquals(model, iterated)
        val reversed = ArrayList<Int>()
        deque.forEachReversed { reversed.add(it) }
        assertEquals(model.reversed(), reversed)
        assertEquals(deque, model.toIntDeque())
    }

    private fun check(ops: List<Op>, fullCheckEvery: Int) {
        var deque = IntDeque(0)
        val model = ArrayList<Int>()
        ops.forEachIndexed { step, op ->
            deque = apply(op, deque, model)
            checkQueries(op, deque, model)
            if (step % fullCheckEvery == 0 || step == ops.size - 1) checkFully(deque, model)
        }
    }

    @Test
    fun matchesArrayListUnderRandomOperations() {
        for (seed in TestConfig.seeds) {
            for (distribution in Distribution.entries) {
                for (domain in ValueDomain.entries) {
                    val ops = randomOps(seed, TestConfig.operations / 16, distribution, domain)
                    OperationRunner.run("IntDeque $distribution $domain", seed, ops) { check(it, 50) }
                }
            }
        }
    }

    @Test
    fun everyOperationSequenceUpToLengthThreeMatches() {
        val values = listOf(BOUNDARY_VALUES.first(), BOUNDARY_VALUES.last())
        // A negative distance rotates the other way, and on a short deque takes the other loop.
        val ops = Kind.entries.flatMap { kind -> values.map { Op(kind, 1, it) } } + Op(Kind.ROTATE, -2, values[0])
        for (first in ops) for (second in ops) for (third in ops) {
            check(listOf(first, second, third), 1)
        }
    }

    private companion object {
        val BOUNDARY_VALUES = listOf(Int.MIN_VALUE, (-1).toInt(), 0, 1.toInt(), Int.MAX_VALUE)
    }
}
