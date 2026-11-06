// SPDX-License-Identifier: CC0-1.0
package benchmarks

import ac_primitive_collections.IntList
import com.carrotsearch.hppc.IntArrayList as HppcIntList
import it.unimi.dsi.fastutil.ints.IntArrayList as FastutilIntList
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import java.util.concurrent.TimeUnit

/** IntList against the standard list, fastutil, and HPPC, and against itself without modCount. */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class IntListBenchmark {
    @Param("1000", "1000000")
    var n: Int = 0

    private lateinit var ours: IntList
    private lateinit var boxed: ArrayList<Int>
    private lateinit var fastutil: FastutilIntList
    private lateinit var hppc: HppcIntList

    @Setup
    fun setup() {
        ours = IntList(n) { it }
        boxed = ArrayList((0 until n).toList())
        fastutil = FastutilIntList(IntArray(n) { it })
        hppc = HppcIntList.from(*IntArray(n) { it })
    }

    @Benchmark
    fun addOurs(): IntList {
        val list = IntList()
        for (i in 0 until n) list.add(i)
        return list
    }

    /** The cost of the modCount increment in add. See docs/PLAN.md, section 5.3.1. */
    @Benchmark
    fun addWithoutModCount(): NoModCountList {
        val list = NoModCountList()
        for (i in 0 until n) list.add(i)
        return list
    }

    /** Identical to [addWithoutModCount] except for the increment, which isolates its cost. */
    @Benchmark
    fun addWithModCount(): ModCountList {
        val list = ModCountList()
        for (i in 0 until n) list.add(i)
        return list
    }

    @Benchmark
    fun addArrayList(): ArrayList<Int> {
        val list = ArrayList<Int>()
        for (i in 0 until n) list.add(i)
        return list
    }

    @Benchmark
    fun addFastutil(): FastutilIntList {
        val list = FastutilIntList()
        for (i in 0 until n) list.add(i)
        return list
    }

    @Benchmark
    fun addHppc(): HppcIntList {
        val list = HppcIntList()
        for (i in 0 until n) list.add(i)
        return list
    }

    @Benchmark
    fun sumIndexOurs(): Long {
        var sum = 0L
        for (i in 0 until ours.size) sum += ours[i]
        return sum
    }

    @Benchmark
    fun sumForEachOurs(): Long {
        var sum = 0L
        ours.forEach { sum += it }
        return sum
    }

    @Benchmark
    fun sumIteratorOurs(): Long {
        var sum = 0L
        for (x in ours) sum += x
        return sum
    }

    @Benchmark
    fun sumArrayList(): Long {
        var sum = 0L
        for (x in boxed) sum += x
        return sum
    }

    @Benchmark
    fun sumFastutil(): Long {
        var sum = 0L
        for (i in 0 until fastutil.size) sum += fastutil.getInt(i)
        return sum
    }

    @Benchmark
    fun sumHppc(): Long {
        var sum = 0L
        for (i in 0 until hppc.size()) sum += hppc.get(i)
        return sum
    }
}

/** The same growth as IntList.add, minus the modCount increment. */
class NoModCountList {
    private var elements = IntArray(DEFAULT)
    private var length = 0

    fun add(value: Int) {
        val n = length
        if (n == elements.size) elements = elements.copyOf(maxOf(n + (n shr 1), DEFAULT))
        elements[n] = value
        length = n + 1
    }

    private companion object {
        const val DEFAULT = 8
    }
}

/** [NoModCountList] plus the increment. */
class ModCountList {
    private var elements = IntArray(DEFAULT)
    private var length = 0
    private var modCount = 0

    fun add(value: Int) {
        modCount++
        val n = length
        if (n == elements.size) elements = elements.copyOf(maxOf(n + (n shr 1), DEFAULT))
        elements[n] = value
        length = n + 1
    }

    private companion object {
        const val DEFAULT = 8
    }
}
