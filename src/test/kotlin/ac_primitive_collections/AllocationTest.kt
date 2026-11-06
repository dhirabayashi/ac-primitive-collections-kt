// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

import java.lang.management.ManagementFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Checks the zero-allocation promises from docs/PLAN.md, section 7.3.
 *
 * A failing cold run does not count: the JIT removes iterators only after warm-up, and this test
 * JVM compiles in the background, so each operation gets many attempts and must reach zero once.
 */
class AllocationTest {
    private val threads = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
    private val threadId = Thread.currentThread().threadId()

    /** Written to keep the JIT from removing the work under test. */
    private var sink = 0L

    private fun allocated(block: () -> Unit): Long {
        val before = threads.getThreadAllocatedBytes(threadId)
        block()
        return threads.getThreadAllocatedBytes(threadId) - before
    }

    private fun assertReachesZeroAllocation(name: String, attempts: Int = 400, block: () -> Unit) {
        val overhead = (0 until 1000).minOf { allocated { } }
        var smallest = Long.MAX_VALUE
        for (attempt in 0 until attempts) {
            repeat(2000) { block() }
            smallest = minOf(smallest, allocated(block) - overhead)
            if (smallest <= 0) break
        }
        assertEquals(0, maxOf(smallest, 0), "$name allocated $smallest bytes after warm-up")
    }

    private var escaped: Any? = null

    @Test
    fun measurementDetectsAllocation() {
        val failure = assertFailsWith<AssertionError> {
            assertReachesZeroAllocation("escaping allocation", attempts = 3) { escaped = IntList(8) }
        }
        assertTrue("escaping allocation" in failure.message!!)
    }

    private val list = IntList(1000).also { l -> repeat(1000) { l.add(it) } }

    @Test
    fun get() = assertReachesZeroAllocation("get") { sink += list[sink.toInt() and 511] }

    @Test
    fun set() = assertReachesZeroAllocation("set") { list[(sink++).toInt() and 511] = 1 }

    @Test
    fun addWithinCapacity() {
        val target = IntList(100)
        assertReachesZeroAllocation("add") {
            target.clear()
            repeat(100) { target.add(it) }
        }
    }

    @Test
    fun containsAndIndexOf() = assertReachesZeroAllocation("contains") { if (5 in list) sink++ }

    @Test
    fun forEach() = assertReachesZeroAllocation("forEach") { list.forEach { sink += it } }

    @Test
    fun aggregates() = assertReachesZeroAllocation("aggregates") { sink += list.sum() + list.min() + list.lowerBound(3) }

    @Test
    fun pairHeapWithinCapacity() {
        val heap = LongIntMinHeap(200)
        assertReachesZeroAllocation("pair heap") {
            for (i in 0 until 100) heap.add((i * 7919 % 101).toLong(), i)
            while (heap.isNotEmpty()) {
                sink += heap.firstKey() + heap.firstValue()
                heap.removeFirst()
            }
        }
    }

    @Test
    fun doubleKeyedMaxHeapWithinCapacity() {
        val heap = DoubleLongMaxHeap(200)
        assertReachesZeroAllocation("double max heap") {
            for (i in 0 until 100) heap.add((i * 7919 % 101).toDouble(), i.toLong())
            while (heap.isNotEmpty()) {
                sink += heap.firstValue()
                heap.removeFirst()
            }
        }
    }

    @Test
    fun objValueHeapWithinCapacity() {
        val heap = IntObjMinHeap<String>(200)
        assertReachesZeroAllocation("obj value heap") {
            for (i in 0 until 100) heap.add(i * 7919 % 101, "x")
            while (heap.isNotEmpty()) {
                sink += heap.firstValue().length
                heap.removeFirst()
            }
        }
    }

    @Test
    fun heapForEachUnordered() {
        val heap = LongIntMinHeap(200)
        for (i in 0 until 100) heap.add(i.toLong(), i)
        assertReachesZeroAllocation("forEachUnordered") { heap.forEachUnordered { key, value -> sink += key + value } }
    }

    @Test
    fun heapEqualsDoesNotCopyTheReservedCapacity() {
        val a = IntIntMinHeap()
        val b = IntIntMinHeap()
        a.ensureCapacity(1_000_000)
        b.ensureCapacity(1_000_000)
        a.add(1, 1)
        b.add(2, 2)
        val used = allocated { assertTrue(a != b) }
        assertTrue(used < 100_000, "equals allocated $used bytes for two pairs")
    }

    @Test
    fun forUntil() = assertReachesZeroAllocation("for until") {
        for (i in 0 until list.size) sink += list[i]
    }

    @Test
    fun forOverList() = assertReachesZeroAllocation("for in list") {
        for (x in list) sink += x
    }

    @Test
    fun forOverIndices() = assertReachesZeroAllocation("for in indices") {
        for (i in list.indices) sink += list[i]
    }
}
