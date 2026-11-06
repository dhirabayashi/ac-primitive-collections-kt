// Compares Dijkstra with LongIntMinHeap against PriorityQueue<Pair<Long, Int>> on AtCoder's code test.
// Also compares a binary heap, to choose the arity (docs/PLAN.md, section 5.5).
// Bundle it with ./bundle -p. A code test is not a contest, so this is allowed.
// Standard input chooses one workload, so each measurement is its own cold JVM, as a submission is:
//   <ours|binary|boxed> <vertices> <edges>     such as "ours 200000 1000000"
// All kinds run the same search on the same graph, and must print the same result.
import ac_primitive_collections.*

/** The same design as LongIntMinHeap with two children per node. */
private class BinaryHeap {
    private var keys = LongArray(8)
    private var values = IntArray(8)
    private var length = 0

    fun isNotEmpty() = length != 0
    fun firstKey() = keys[0]
    fun firstValue() = values[0]

    fun add(key: Long, value: Int) {
        if (length == keys.size) {
            keys = keys.copyOf(length + (length shr 1))
            values = values.copyOf(keys.size)
        }
        val k = keys
        val v = values
        var hole = length++
        while (hole > 0) {
            val parent = (hole - 1) ushr 1
            if (key >= k[parent]) break
            k[hole] = k[parent]
            v[hole] = v[parent]
            hole = parent
        }
        k[hole] = key
        v[hole] = value
    }

    fun removeFirst() {
        val last = --length
        if (last == 0) return
        val k = keys
        val v = values
        val key = k[last]
        val value = v[last]
        val n = length
        var hole = 0
        while (true) {
            var child = 2 * hole + 1
            if (child >= n) break
            if (child + 1 < n && k[child + 1] < k[child]) child++
            if (k[child] >= key) break
            k[hole] = k[child]
            v[hole] = v[child]
            hole = child
        }
        k[hole] = key
        v[hole] = value
    }
}

private class Graph(val head: IntArray, val to: IntArray, val cost: IntArray)

private fun randomGraph(n: Int, m: Int): Graph {
    var state = 88172645463325252L
    fun next(bound: Int): Int {
        state = state xor (state shl 13)
        state = state xor (state ushr 7)
        state = state xor (state shl 17)
        return ((state ushr 33) % bound).toInt()
    }
    val from = IntArray(m) { next(n) }
    val rawTo = IntArray(m) { next(n) }
    val rawCost = IntArray(m) { next(1_000_000_000) + 1 }
    val head = IntArray(n + 1)
    for (f in from) head[f + 1]++
    for (i in 0 until n) head[i + 1] += head[i]
    val position = head.copyOf()
    val to = IntArray(m)
    val cost = IntArray(m)
    for (e in 0 until m) {
        val p = position[from[e]]++
        to[p] = rawTo[e]
        cost[p] = rawCost[e]
    }
    return Graph(head, to, cost)
}

private fun answer(distance: LongArray): Long {
    var sum = 0L
    for (d in distance) if (d != Long.MAX_VALUE) sum += d
    return sum
}

private fun ours(g: Graph, n: Int): Long {
    val distance = LongArray(n) { Long.MAX_VALUE }
    distance[0] = 0
    val queue = LongIntMinHeap()
    queue.add(0L, 0)
    while (queue.isNotEmpty()) {
        val d = queue.firstKey()
        val v = queue.firstValue()
        queue.removeFirst()
        if (d > distance[v]) continue
        for (e in g.head[v] until g.head[v + 1]) {
            val next = d + g.cost[e]
            if (next < distance[g.to[e]]) {
                distance[g.to[e]] = next
                queue.add(next, g.to[e])
            }
        }
    }
    return answer(distance)
}

private fun binary(g: Graph, n: Int): Long {
    val distance = LongArray(n) { Long.MAX_VALUE }
    distance[0] = 0
    val queue = BinaryHeap()
    queue.add(0L, 0)
    while (queue.isNotEmpty()) {
        val d = queue.firstKey()
        val v = queue.firstValue()
        queue.removeFirst()
        if (d > distance[v]) continue
        for (e in g.head[v] until g.head[v + 1]) {
            val next = d + g.cost[e]
            if (next < distance[g.to[e]]) {
                distance[g.to[e]] = next
                queue.add(next, g.to[e])
            }
        }
    }
    return answer(distance)
}

private fun boxed(g: Graph, n: Int): Long {
    val distance = LongArray(n) { Long.MAX_VALUE }
    distance[0] = 0
    val queue = java.util.PriorityQueue<Pair<Long, Int>>(compareBy { it.first })
    queue.add(0L to 0)
    while (queue.isNotEmpty()) {
        val (d, v) = queue.poll()
        if (d > distance[v]) continue
        for (e in g.head[v] until g.head[v + 1]) {
            val next = d + g.cost[e]
            if (next < distance[g.to[e]]) {
                distance[g.to[e]] = next
                queue.add(next to g.to[e])
            }
        }
    }
    return answer(distance)
}

fun main() {
    val tokens = java.util.StringTokenizer(readln())
    val kind = tokens.nextToken()
    val n = tokens.nextToken().toInt()
    val m = tokens.nextToken().toInt()
    val graph = randomGraph(n, m)
    val start = System.nanoTime()
    val result = when (kind) {
        "ours" -> ours(graph, n)
        "binary" -> binary(graph, n)
        else -> boxed(graph, n)
    }
    val millis = (System.nanoTime() - start) / 1_000_000
    println("kind=$kind n=$n m=$m ms=$millis result=$result")
}
