// Exercises the array extensions of every element type from user code. The strict boxing check runs on this
// file's MainKt, so anything that boxes after inlining fails the build. Do not add boxing conversions here.
import ac_primitive_collections.*

fun IntArray.text() = joinToString(" ")

fun intArrays() {
    val a = intArrayOf(5, 2, 9, 2, 7)
    println(a.compress().text())
    println(a.argsort().text())
    println(a.argsortBy { -it }.text())
    println(a.argsortByLong { it % 3L }.text())
    println(a.argsortByDouble { if (it > 4) 0.5 else -0.0 }.text())
    val prefix = a.prefixSum()
    println("${prefix.size} ${prefix[5]} ${prefix[3] - prefix[1]}")
    println(intArrayOf(Int.MAX_VALUE, Int.MAX_VALUE).prefixSum()[2])
    val byKey = a.copyOf()
    byKey.sortBy { it % 3 }
    println(byKey.text())
    byKey.sortByLong { -it.toLong() }
    println(byKey.text())
    byKey.sortByDouble { it / 2.0 }
    println(byKey.text())
    val descending = IntComparator { x, y -> y.compareTo(x) }
    byKey.sortWith(descending)
    println(byKey.text())
    byKey.sortWith(IntComparator { x, y -> x.compareTo(y) }, 1, 4)
    println(byKey.text())
    a.sort()
    println("${a.lowerBound(2)} ${a.upperBound(2)} ${a.lowerBound(6)} ${a.lowerBound(100)} ${a.lowerBound(2, 2, 5)} ${a.upperBound(7, 0, 3)}")
    val distinct = a.dedupe()
    println("$distinct ${a.copyOf(distinct).text()}")
    val p = intArrayOf(1, 2, 3)
    var count = 1
    while (p.nextPermutation()) count++
    println("$count ${p.text()}")
}

fun longArrays() {
    val a = longArrayOf(30L, Long.MIN_VALUE, 10L, 30L)
    println(a.compress().text())
    println(a.argsort().text())
    println("${a.argsortBy { (it % 7).toInt() }.text()} | ${a.argsortByLong { -it / 2 }.text()} | ${a.argsortByDouble { it.toDouble() }.text()}")
    val prefix = longArrayOf(1L, 2L, 3L).prefixSum()
    println("${prefix[3]} ${prefix.size}")
    a.sortBy { (it and 1L).toInt() }
    println(a.joinToString(" "))
    a.sortByLong { it }
    a.sortByDouble { -it.toDouble() }
    a.sortWith(LongComparator { x, y -> x.compareTo(y) })
    a.sortWith(LongComparator { x, y -> y.compareTo(x) }, 0, 2)
    println(a.joinToString(" "))
    a.sort()
    println("${a.lowerBound(30L)} ${a.upperBound(30L)} ${a.lowerBound(0L, 1, 4)} ${a.upperBound(Long.MIN_VALUE, 0, 2)} ${a.dedupe()} ${a.nextPermutation()}")
}

fun doubleArrays() {
    // The total order of sort: -0.0 before 0.0, and NaN last.
    val a = doubleArrayOf(0.0, Double.NaN, -0.0, -1.5, 0.0)
    println(a.compress().text())
    println(a.argsort().text())
    println("${a.argsortBy { it.toInt() }.text()} | ${a.argsortByLong { it.toLong() }.text()} | ${a.argsortByDouble { -it }.text()}")
    val prefix = doubleArrayOf(0.5, 0.25).prefixSum()
    println("${prefix[2]} ${prefix.size}")
    val b = a.copyOf()
    b.sortBy { it.toInt() }
    b.sortByLong { 0L }
    b.sortByDouble { it }
    println(b.joinToString(" "))
    b.sortWith(DoubleComparator { x, y -> y.compareTo(x) })
    b.sortWith(DoubleComparator { x, y -> x.compareTo(y) }, 1, 3)
    println(b.joinToString(" "))
    a.sort()
    println("${a.lowerBound(0.0)} ${a.upperBound(-0.0)} ${a.lowerBound(Double.NaN)} ${a.lowerBound(0.0, 0, 2)} ${a.upperBound(0.0, 0, 2)}")
    val distinct = a.dedupe()
    println("$distinct ${a.copyOf(distinct).joinToString(" ")} ${doubleArrayOf(1.0, 2.0).nextPermutation()}")
}

fun charArrays() {
    val s = "banana".toCharArray()
    println(s.compress().text())
    println(s.argsort().text())
    println("${s.argsortBy { -it.code }.text()} | ${s.argsortByLong { it.code.toLong() }.text()} | ${s.argsortByDouble { it.code / 2.0 }.text()}")
    val t = s.copyOf()
    t.sortBy { it.code % 2 }
    println(String(t))
    t.sortByLong { -it.code.toLong() }
    t.sortByDouble { it.code.toDouble() }
    t.sortWith(CharComparator { x, y -> y.compareTo(x) })
    println(String(t))
    t.sortWith(CharComparator { x, y -> x.compareTo(y) }, 0, 3)
    println(String(t))
    s.sort()
    println("${String(s)} ${s.lowerBound('b')} ${s.upperBound('a')} ${s.lowerBound('n', 0, 3)} ${s.upperBound('z', 2, 4)}")
    val distinct = s.dedupe()
    println(String(s, 0, distinct))
    val word = "abc".toCharArray()
    val words = StringBuilder()
    do {
        words.append(word).append(' ')
    } while (word.nextPermutation())
    println(words.toString().trimEnd())
}

fun main() {
    intArrays()
    longArrays()
    doubleArrays()
    charArrays()
}
