// Exercises the IntList API from user code. The strict boxing check runs on this file's MainKt,
// so anything that boxes after inlining fails the build. Do not add boxing conversions here.
import ac_primitive_collections.*

fun operators() {
    val a = intListOf(1, 2, 3)
    a[0] = 5
    a[1] += 10
    a[2]++
    println(a)
    println(2 in a)
    println(12 in a)
    println(a + 7)
    println(a + intListOf(8, 9))
    println(a - 5)
    println(a - intListOf(12))
    a += 7
    a += intListOf(8)
    a -= 8
    a -= intListOf(7)
    println(a)
    val (x, y, z) = a
    println(x + y + z)
    for (v in a) print("$v ")
    println()
    for (i in a.indices) print("${a[i]} ")
    println()
    for (i in 0 until a.size) print("${a[i]} ")
    println()
    println("${a.size} ${a.lastIndex} ${a.isEmpty()} ${a.isNotEmpty()}")
}

fun factories() {
    println(IntList(3) { it * it })
    println(buildIntList {
        add(1)
        add(2)
    })
    println(intArrayOf(4, 5).toIntList())
    println((1..4).toIntList())
    println(IntList(16).capacity)
}

fun higherOrder() {
    val a = intListOf(3, 1, 4, 1, 5, 9, 2, 6)
    println(a.map { it * 2 })
    println(a.filter { it % 2 == 1 })
    println(a.any { it > 8 })
    println(a.all { it > 0 })
    println(a.none { it > 9 })
    println(a.count { it == 1 })
    println(a.fold(0) { acc, v -> acc + v })
    println(a.fold(1L) { acc, v -> acc * v })
    println(a.fold(0.5) { acc, v -> acc + v })
    println(a.sumOf { it.toLong() })
    var sum = 0L
    a.forEach { sum += it }
    a.forEachIndexed { i, v -> sum += i * v }
    a.forEachReversed { sum -= it }
    println(sum)
    println(a.indexOfFirst { it == 4 })
    println(a.indexOfLast { it == 1 })
    println(a.firstOrElse { -1 })
    println(a.lastOrElse { -1 })
    println(a.removeIf { it > 5 })
    println(a)
}

fun earlyReturn(a: IntList): Int {
    a.forEach { if (it > 2) return it }
    return -1
}

fun sorting() {
    val a = intListOf(5, 2, 8, 1, 9, 3)
    println(a.sorted())
    println(a.sortedDescending())
    println(a.reversed())
    println(a.argsort().joinToString())
    println(a.argsortBy { -it }.joinToString())
    val b = a.copy()
    b.sortBy { it % 3 }
    println(b)
    val c = a.copy()
    c.sortWith { p, q -> q.compareTo(p) }
    println(c)
    c.sort()
    println("${c.binarySearch(8)} ${c.lowerBound(4)} ${c.upperBound(5)}")
    println(c.nextPermutation())
    println(c)
    val d = intListOf(1, 1, 2, 2, 1)
    println(d.distinct())
    d.dedupe()
    println(d)
    d.rotate(1)
    d.swap(0, 1)
    println(d)
    val e = intListOf(1, 2, 3, 4, 5, 6, 7, 8)
    e.shuffle(kotlin.random.Random(1))
    println(e.sum())
}

fun subLists() {
    val a = IntList(8) { 8 - it }
    val view = a.subList(2, 6)
    view.sort()
    println(a)
    view[0] = 100
    println(view)
    println(view.sum())
    println(view.count { it > 3 })
    println(view.any { it == 100 })
    println(view.all { it > 0 })
    var total = 0
    view.forEach { total += it }
    for (v in view) total += v
    println(total)
    val inner = view.subList(1, 3)
    inner.reverse()
    println(a)
    view.sortBy { -it }
    view.sortWith { p, q -> p.compareTo(q) }
    println(view.toIntList())
    view.clear()
    println(a)
}

fun removals() {
    val a = IntList(10) { it }
    println(a.removeAt(3))
    println(a.removeFirst())
    println(a.removeLast())
    println(a.removeValue(5))
    println(a.swapRemove(0))
    a.removeRange(0, 1)
    a.insert(0, 42)
    a.insertAll(1, intArrayOf(43, 44))
    a.addAll(intListOf(45), 0, 1)
    a.truncate(4)
    a.resize(6, -1)
    a.fill(9, 0, 1)
    println(a)
    println("${a.min()} ${a.max()} ${a.indexOfMin()} ${a.indexOfMax()} ${a.minOrDefault(0)} ${a.maxOrDefault(0)}")
    println("${a.first()} ${a.last()} ${a.firstOrDefault(0)} ${a.lastOrDefault(0)}")
    println("${a.take(2)} ${a.drop(4)} ${a.slice(1, 2)} ${a.hashCode()} ${a == a.copy()}")
    val out = StringBuilder()
    a.appendTo(out)
    println(out)
    println(a.joinToString("-"))
    println(a.toIntArray().sum())
    a.trimToSize()
    a.ensureCapacity(100)
    a.clear()
    println(a.firstOrDefault(-5))
}

fun main() {
    operators()
    factories()
    higherOrder()
    println(earlyReturn(intListOf(1, 2, 3)))
    println(earlyReturn(intListOf(1)))
    sorting()
    subLists()
    removals()
}
