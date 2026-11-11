// Exercises the single-type heaps and the indexed heaps from user code. The strict boxing check runs on this
// file's MainKt, so anything that boxes after inlining fails the build. Do not add boxing conversions here.
import ac_primitive_collections.*

/** A Dijkstra with an indexed heap: each vertex is in the queue at most once. Input: N M, then M lines of u v w. */
fun dijkstra() {
    val input = java.io.StreamTokenizer(System.`in`.bufferedReader())
    fun nextInt(): Int {
        input.nextToken()
        return input.nval.toInt()
    }
    val n = nextInt()
    val m = nextInt()
    val head = IntArray(n) { -1 }
    val next = IntArray(2 * m)
    val to = IntArray(2 * m)
    val weight = LongArray(2 * m)
    for (e in 0 until m) {
        val u = nextInt() - 1
        val v = nextInt() - 1
        val w = nextInt().toLong()
        for ((slot, a, b) in arrayOf(intArrayOf(2 * e, u, v), intArrayOf(2 * e + 1, v, u))) {
            to[slot] = b
            weight[slot] = w
            next[slot] = head[a]
            head[a] = slot
        }
    }
    val distance = LongArray(n) { Long.MAX_VALUE }
    val queue = LongIndexedMinHeap(n)
    distance[0] = 0L
    queue[0] = 0L
    var pops = 0
    while (queue.isNotEmpty()) {
        val d = queue.firstKey()
        val v = queue.removeFirst()
        pops++
        var slot = head[v]
        while (slot >= 0) {
            val u = to[slot]
            val candidate = d + weight[slot]
            if (candidate < distance[u]) {
                distance[u] = candidate
                queue[u] = candidate
            }
            slot = next[slot]
        }
    }
    val out = StringBuilder()
    for (d in distance) out.append(if (d == Long.MAX_VALUE) -1L else d).append(' ')
    println(out.toString().trimEnd())
    println("pops $pops")
}

fun intHeaps() {
    val min = IntMinHeap()
    min += 5
    min.add(1)
    min.addAll(intArrayOf(9, 3, 7))
    min.addAll(intArrayOf(100, 2, 200), 1, 2)
    println("${min.size} ${min.first()} ${min.isEmpty()} ${min.isNotEmpty()} ${min.capacity}")
    println("${min.removeFirst()} ${min.replaceFirst(8)} ${min.first()}")
    var sum = 0
    min.forEachUnordered { sum += it }
    println(sum)
    println(min)
    val builder = StringBuilder()
    min.appendTo(builder)
    println(builder)
    val sorted = min.toSortedArray()
    println("${sorted.size} ${sorted[0]} ${min.toIntArray().size}")
    val copy = min.copy()
    println("${min == copy} ${min.hashCode() == copy.hashCode()}")
    copy.addAll(min)
    println(copy)
    while (copy.isNotEmpty()) print("${copy.removeFirst()} ")
    println()
    println("${copy.firstOrDefault(-1)} ${copy.removeFirstOrDefault(-2)}")
    min.ensureCapacity(100)
    min.clear()
    println("$min ${min.capacity}")

    val max = intMaxHeapOf(4, 8, 1, 6)
    println("${max.first()} ${max.removeFirst()} $max")
    println(intArrayOf(3, 9, 2).toIntMaxHeap())
    println(intArrayOf(3, 9, 2).toIntMinHeap())
    println(intMinHeapOf(Int.MIN_VALUE, Int.MAX_VALUE, 0))
}

fun longAndDoubleHeaps() {
    val longMin = longMinHeapOf(30L, 10L, 20L)
    longMin += Long.MIN_VALUE
    println("${longMin.removeFirst()} ${longMin.first()} ${longMin.size}")
    val longMax = LongMaxHeap(0)
    longMax.addAll(longArrayOf(1L, Long.MAX_VALUE, -5L))
    println("${longMax.removeFirstOrDefault(0L)} $longMax")
    var longSum = 0L
    longMax.forEachUnordered { longSum += it }
    println(longSum)

    // Doubles follow the total order: -0.0 before 0.0, and NaN last.
    val doubleMin = doubleMinHeapOf(0.0, Double.NaN, -0.0, -1.5)
    println(doubleMin)
    println("${doubleMin.removeFirst()} ${doubleMin.removeFirst()} ${doubleMin.first()}")
    val doubleMax = doubleArrayOf(0.5, Double.NaN, 2.0).toDoubleMaxHeap()
    println("${doubleMax.first()} ${doubleMax.replaceFirst(1.0)} $doubleMax")
    var doubleSum = 0.0
    doubleMax.forEachUnordered { doubleSum += it }
    println(doubleSum)
    println("${doubleMax.firstOrDefault(9.0)} ${doubleMax == doubleMax.copy()}")
}

fun indexedHeaps() {
    val heap = IntIndexedMaxHeap(5)
    heap[0] = 10
    heap[3] = 30
    heap[4] = 20
    println("${heap.size} ${heap.indexCount} ${heap.firstIndex()} ${heap.firstKey()} ${3 in heap} ${1 in heap}")
    heap.decreaseKey(3, 5)
    heap.increaseKey(0, 15)
    println("${heap.firstIndex()} ${heap.keyOf(3)} $heap")
    println("${heap.remove(4)} ${heap.remove(4)} ${heap.isEmpty()} ${heap.isNotEmpty()}")
    var weighted = 0
    heap.forEachUnordered { index, key -> weighted += index * key }
    println(weighted)
    val copy = heap.copy()
    println("${heap == copy} ${heap.hashCode() == copy.hashCode()}")
    copy.clear()
    println("$copy ${copy.size} $heap")

    val doubles = DoubleIndexedMinHeap(3)
    doubles[2] = Double.NaN
    doubles[1] = -0.0
    doubles[0] = 0.0
    while (doubles.isNotEmpty()) {
        print("${doubles.firstIndex()}:${doubles.firstKey()} ")
        doubles.removeFirst()
    }
    println()
    val longs = LongIndexedMaxHeap(2)
    longs[1] = Long.MIN_VALUE
    longs[0] = Long.MAX_VALUE
    println("${longs.removeFirst()} ${longs.removeFirst()}")
    val ints = IntIndexedMinHeap(2)
    ints[1] = 7
    println("${ints.firstIndex()} ${ints.keyOf(1)}")
    val longMin = LongIndexedMinHeap(1)
    longMin[0] = 3L
    val doubleMax = DoubleIndexedMaxHeap(1)
    doubleMax[0] = 3.5
    println("${longMin.firstKey()} ${doubleMax.firstKey()}")
}

fun main() {
    dijkstra()
    intHeaps()
    longAndDoubleHeaps()
    indexedHeaps()
}
