// ABC 340 D - Super Takahashi Bros. A Dijkstra that proves the bundled heap gets AC.
import ac_primitive_collections.*

fun main() {
    val input = java.io.StreamTokenizer(System.`in`.bufferedReader())
    fun nextInt(): Int {
        input.nextToken()
        return input.nval.toInt()
    }
    val n = nextInt()
    val a = IntArray(n - 1)
    val b = IntArray(n - 1)
    val x = IntArray(n - 1)
    for (i in 0 until n - 1) {
        a[i] = nextInt()
        b[i] = nextInt()
        x[i] = nextInt() - 1
    }
    val distance = LongArray(n) { Long.MAX_VALUE }
    distance[0] = 0
    val queue = LongIntMinHeap()
    queue.add(0L, 0)
    while (queue.isNotEmpty()) {
        val d = queue.firstKey()
        val v = queue.firstValue()
        queue.removeFirst()
        if (d > distance[v] || v == n - 1) continue
        val viaNext = d + a[v]
        if (viaNext < distance[v + 1]) {
            distance[v + 1] = viaNext
            queue.add(viaNext, v + 1)
        }
        val viaJump = d + b[v]
        if (viaJump < distance[x[v]]) {
            distance[x[v]] = viaJump
            queue.add(viaJump, x[v])
        }
    }
    println(distance[n - 1])
}
