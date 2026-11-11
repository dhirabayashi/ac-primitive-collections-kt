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
    fun singleHeapsWithinCapacity() {
        val ints = IntMinHeap(200)
        assertReachesZeroAllocation("int heap") {
            for (i in 0 until 100) ints += i * 7919 % 101
            sink += ints.replaceFirst(50).toLong()
            while (ints.isNotEmpty()) sink += ints.removeFirst()
        }
        val doubles = DoubleMaxHeap(200)
        assertReachesZeroAllocation("double max heap of values") {
            for (i in 0 until 100) doubles.add((i * 7919 % 101).toDouble())
            while (doubles.isNotEmpty()) sink += doubles.removeFirst().toLong()
        }
        val longs = LongMinHeap(200)
        val bulk = LongArray(100) { (it * 7919 % 101).toLong() }
        assertReachesZeroAllocation("long heap addAll") {
            longs.addAll(bulk)
            longs.forEachUnordered { sink += it }
            longs.clear()
        }
    }

    @Test
    fun indexedHeapAsInDijkstra() {
        val heap = LongIndexedMinHeap(100)
        assertReachesZeroAllocation("indexed heap") {
            for (i in 0 until 100) heap[i] = (i * 7919 % 101).toLong()
            // Not `0 until 100 step 3`: `step` builds an IntRange and an IntProgression, which the JIT does not always remove.
            var i = 0
            while (i < 100) {
                heap.decreaseKey(i, -1L)
                i += 3
            }
            heap.remove(50)
            heap.forEachUnordered { index, key -> sink += index + key }
            while (heap.isNotEmpty()) {
                sink += heap.firstKey()
                sink += heap.removeFirst()
            }
        }
    }

    private val sortedInts = IntArray(1000) { it * 2 }

    @Test
    fun arraySearchesAndPermutations() {
        assertReachesZeroAllocation("array bounds") { sink += sortedInts.lowerBound(sink.toInt() and 1023) + sortedInts.upperBound(77) }
        val permutation = IntArray(8) { it }
        assertReachesZeroAllocation("array nextPermutation") { if (permutation.nextPermutation()) sink++ }
        val runs = IntArray(100) { it / 3 }
        assertReachesZeroAllocation("array dedupe") { sink += runs.dedupe() + sortedInts.lowerBound(5, 10, 20) }
    }

    @Test
    fun dequeAsAQueueWithinCapacity() {
        val deque = IntDeque(128)
        assertReachesZeroAllocation("deque queue") {
            // Ten passes of 100 leave the head somewhere new each time, so the indices wrap.
            repeat(10) {
                for (i in 0 until 100) deque.addLast(i)
                while (deque.isNotEmpty()) sink += deque.removeFirst()
            }
        }
    }

    @Test
    fun dequeAsAStackAndBothEndsWithinCapacity() {
        val deque = LongDeque(128)
        assertReachesZeroAllocation("deque both ends") {
            for (i in 0 until 50) {
                deque.addFirst(i.toLong())
                deque.addLast(i.toLong())
            }
            while (deque.isNotEmpty()) sink += deque.removeLast() + deque.removeFirst()
        }
    }

    private val deque = IntDeque(1024).also { d -> repeat(1000) { d.addFirst(it) } }

    @Test
    fun dequeReads() = assertReachesZeroAllocation("deque reads") {
        sink += deque[sink.toInt() and 511] + deque.first() + deque.last()
        if (5 in deque) sink++
    }

    @Test
    fun dequeForEach() = assertReachesZeroAllocation("deque forEach") { deque.forEach { sink += it } }

    @Test
    fun forOverDeque() = assertReachesZeroAllocation("for in deque") {
        for (x in deque) sink += x
    }

    @Test
    fun forUntilOverDeque() = assertReachesZeroAllocation("for until over deque") {
        for (i in 0 until deque.size) sink += deque[i]
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

    private val longs = LongList(1000).also { l -> repeat(1000) { l.add(it.toLong()) } }
    private val doubles = DoubleList(1000).also { l -> repeat(1000) { l.add(it.toDouble()) } }
    private val chars = CharList(1000).also { l -> repeat(1000) { l.add('a' + it % 26) } }

    /** The hot paths from docs/PLAN.md section 7.3, for the types other than Int. */
    @Test
    fun getAndSetOfOtherTypes() {
        assertReachesZeroAllocation("long get and set") { longs[(sink++).toInt() and 511] = longs[1] }
        assertReachesZeroAllocation("double get and set") { doubles[(sink++).toInt() and 511] = doubles[1] }
        assertReachesZeroAllocation("char get and set") { chars[(sink++).toInt() and 511] = chars[1] }
    }

    @Test
    fun addWithinCapacityOfOtherTypes() {
        val longTarget = LongList(100)
        val doubleTarget = DoubleList(100)
        val charTarget = CharList(100)
        assertReachesZeroAllocation("long add") {
            longTarget.clear()
            repeat(100) { longTarget.add(it.toLong()) }
        }
        assertReachesZeroAllocation("double add") {
            doubleTarget.clear()
            repeat(100) { doubleTarget.add(it.toDouble()) }
        }
        assertReachesZeroAllocation("char add") {
            charTarget.clear()
            repeat(100) { charTarget.add('a') }
        }
    }

    @Test
    fun containsOfOtherTypes() {
        assertReachesZeroAllocation("long contains") { if (5L in longs) sink++ }
        assertReachesZeroAllocation("double contains") { if (5.0 in doubles) sink++ }
        assertReachesZeroAllocation("char contains") { if ('c' in chars) sink++ }
    }

    @Test
    fun forUntilOfOtherTypes() {
        assertReachesZeroAllocation("long for until") { for (i in 0 until longs.size) sink += longs[i] }
        assertReachesZeroAllocation("double for until") { for (i in 0 until doubles.size) sink += doubles[i].toLong() }
        assertReachesZeroAllocation("char for until") { for (i in 0 until chars.size) sink += chars[i].code }
    }

    @Test
    fun forOverIndicesOfOtherTypes() {
        assertReachesZeroAllocation("long for in indices") { for (i in longs.indices) sink += longs[i] }
        assertReachesZeroAllocation("double for in indices") { for (i in doubles.indices) sink += doubles[i].toLong() }
        assertReachesZeroAllocation("char for in indices") { for (i in chars.indices) sink += chars[i].code }
    }

    @Test
    fun forOverOtherTypes() {
        assertReachesZeroAllocation("for in long list") { for (x in longs) sink += x }
        assertReachesZeroAllocation("for in double list") { for (x in doubles) sink += x.toLong() }
        assertReachesZeroAllocation("for in char list") { for (x in chars) sink += x.code }
    }

    @Test
    fun forEachOfOtherTypes() {
        assertReachesZeroAllocation("long forEach") { longs.forEach { sink += it } }
        assertReachesZeroAllocation("double forEach") { doubles.forEach { sink += it.toLong() } }
        assertReachesZeroAllocation("char forEach") { chars.forEach { sink += it.code } }
    }

    @Test
    fun aggregatesOfOtherTypes() {
        assertReachesZeroAllocation("long aggregates") { sink += longs.sum() + longs.min() + longs.lowerBound(3L) }
        assertReachesZeroAllocation("double aggregates") { sink += (doubles.sum() + doubles.min()).toLong() + doubles.lowerBound(3.0) }
        assertReachesZeroAllocation("char aggregates") { sink += chars.min().code + chars.lowerBound('c') }
    }
}
