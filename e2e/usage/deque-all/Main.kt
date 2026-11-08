// Exercises the Deque API of every element type from user code. The strict boxing check runs on this file's
// MainKt, so anything that boxes after inlining fails the build. Do not add boxing conversions here.
import ac_primitive_collections.*

private val DR = intArrayOf(1, -1, 0, 0)
private val DC = intArrayOf(0, 0, 1, -1)

/** A grid BFS with the deque as a queue, and a 0-1 BFS with both ends. Input: H W, then H rows of '.' and '#'. */
fun bfs() {
    val size = readln().split(" ")
    val h = size[0].toInt()
    val w = size[1].toInt()
    val grid = List(h) { readln() }
    val dist = IntArray(h * w) { -1 }
    val queue = IntDeque()
    dist[0] = 0
    queue.addLast(0)
    while (queue.isNotEmpty()) {
        val cell = queue.removeFirst()
        val r = cell / w
        val c = cell % w
        for (d in 0 until 4) {
            val nr = r + DR[d]
            val nc = c + DC[d]
            if (nr !in 0 until h || nc !in 0 until w || grid[nr][nc] == '#') continue
            val next = nr * w + nc
            if (dist[next] >= 0) continue
            dist[next] = dist[cell] + 1
            queue.addLast(next)
        }
    }
    println(dist[h * w - 1])

    // Moving onto '#' costs 1 and breaks it; moving onto '.' is free.
    val cost = IntArray(h * w) { Int.MAX_VALUE }
    val deque = IntDeque()
    cost[0] = 0
    deque.addFirst(0)
    while (deque.isNotEmpty()) {
        val cell = deque.removeFirst()
        val r = cell / w
        val c = cell % w
        for (d in 0 until 4) {
            val nr = r + DR[d]
            val nc = c + DC[d]
            if (nr !in 0 until h || nc !in 0 until w) continue
            val next = nr * w + nc
            val step = if (grid[nr][nc] == '#') 1 else 0
            if (cost[cell] + step >= cost[next]) continue
            cost[next] = cost[cell] + step
            if (step == 0) deque.addFirst(next) else deque.addLast(next)
        }
    }
    // Every cell is reachable here, so the sum counts the walls broken on the way to each.
    var total = 0L
    for (x in cost) total += x
    println(total)
}

fun intApis() {
    val d = intDequeOf(1, 2, 3)
    d.addFirst(0)
    d.addLast(4)
    d.add(5)
    d += 6
    d += intDequeOf(7)
    d.addAll(intArrayOf(8, 9))
    d.addAll(intArrayOf(10, 11, 12), 0, 1)
    d.addAll(1..2)
    d.addAll(9 downTo 8 step 1)
    println(d)
    d[0] = 100
    d[1] += 10
    d[2]++
    println("${d[0]} ${d[1]} ${d[2]} ${d.first()} ${d.last()} ${d.size} ${d.lastIndex} ${d.capacity}")
    println("${d.removeFirst()} ${d.removeLast()} ${d.removeAt(3)} ${d.removeValue(9)} ${3 in d} ${42 !in d} ${d.indexOf(7)}")
    d -= 1
    d -= intDequeOf(2, 10)
    println(d)
    println(d + 1)
    println(d + intDequeOf(2, 3))
    println(d - 4)
    println(d - intDequeOf(4, 5))
    d.rotate(2)
    println(d)
    d.rotate(-3)
    println(d)
    println("${d.firstOrDefault(-1)} ${d.lastOrDefault(-1)} ${d.firstOrElse { -2 }} ${d.lastOrElse { -2 }}")
    var sum = 0L
    d.forEach { sum += it }
    d.forEachReversed { sum = sum * 3 + it }
    println(sum)
    for (x in d) print("$x ")
    println()
    for (i in d.indices) print("${d[i]} ")
    println()
    for (i in 0 until d.size) print("${d[i]} ")
    println()
    println(d.toIntArray().joinToString())
    println(d.copy() == d)
    println(d.hashCode())
    println(d.joinToString("-", "<", ">"))
    val builder = StringBuilder()
    d.appendTo(builder)
    println(builder)
    println(intArrayOf(3, 4).toIntDeque())
    println((1..3).toIntDeque())
    d.ensureCapacity(100)
    println(d.capacity)
    d.clear()
    println("${d.isEmpty()} ${d.isNotEmpty()} ${d.firstOrDefault(-1)} ${d.lastOrElse { -2 }}")
    val stack = IntDeque(0)
    for (i in 1..3) stack.addLast(i)
    while (stack.isNotEmpty()) print("${stack.removeLast()} ")
    println()
}

fun longApis() {
    val d = longDequeOf(5_000_000_000L, -1L)
    d.addFirst(3L)
    d += 4L
    d.addAll((1L..2L))
    d.rotate(1)
    println(d)
    var sum = 0L
    d.forEach { sum += it }
    d.forEachReversed { sum -= it / 2 }
    for (x in d) sum += x
    println(sum)
    println("${d.firstOrElse { 0L }} ${d.lastOrElse { 0L }} ${d.removeFirst()} ${d.removeLast()} ${-1L in d}")
    println(d.toLongArray().joinToString())
    println(longArrayOf(1L, 2L).toLongDeque() + 3L)
}

fun doubleApis() {
    val d = doubleDequeOf(1.5, Double.NaN, -0.0)
    d.addFirst(0.0)
    d += 2.5
    println(d)
    println("${d.indexOf(Double.NaN)} ${d.indexOf(-0.0)} ${d.indexOf(0.0)} ${0.25 in d}")
    var sum = 0.0
    d.forEach { if (!it.isNaN()) sum += it }
    d.forEachReversed { if (!it.isNaN()) sum -= it / 2 }
    for (x in d) if (!x.isNaN()) sum += x
    println(sum)
    d -= doubleDequeOf(Double.NaN, 0.0)
    println(d)
    println("${d.firstOrElse { 0.0 }} ${d.lastOrElse { 0.0 }} ${d.removeFirst()} ${d.removeLast()}")
    println(doubleArrayOf(1.0).toDoubleDeque())
}

fun charApis() {
    val d = charDequeOf('b', 'c')
    d.addFirst('a')
    d += 'd'
    d.addAll('x'..'z')
    d.rotate(-1)
    println(d.joinToString(""))
    var codes = 0
    d.forEach { codes += it.code }
    d.forEachReversed { codes -= it.code / 2 }
    for (x in d) codes += x.code
    println(codes)
    println("${d.firstOrElse { '?' }} ${d.lastOrElse { '?' }} ${d.removeFirst()} ${d.removeLast()} ${'c' in d}")
    println(d.toCharArray().concatToString())
    println(charArrayOf('q').toCharDeque() + 'r')
    println(('a'..'c').toCharDeque())
}

fun main() {
    bfs()
    intApis()
    longApis()
    doubleApis()
    charApis()
}
