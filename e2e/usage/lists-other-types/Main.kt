import ac_primitive_collections.*

/**
 * Long, Double, and Char lists, the conversions between types, and the key sorts. The inline functions expand
 * here, and whether their boxing goes away depends on the compiler (docs/PLAN.md, section 5.3.2), so this file
 * calls each of them for the boxing check. That includes `fold` with every primitive result type.
 */
fun main() {
    val longs = longListOf(5_000_000_000L, -1L, 3L)
    longs.sort()
    println(longs)
    println(longs.sum())
    println(longs.argsort().joinToString())
    longs.sortByLong { -it }
    println(longs)
    println((1L..3L).toLongList())

    val doubles = doubleListOf(1.5, Double.NaN, -0.0, 0.0, Double.NEGATIVE_INFINITY)
    doubles.sort()
    println(doubles)
    println(doubles.max())
    println(doubles.indexOf(Double.NaN))
    println(doubles.argsort().joinToString())
    doubles.sortByDouble { if (it.isNaN()) -1.0 else it }
    println(doubles)
    println(doubleListOf(0.5, 0.25).sum())

    val chars = charListOf('c', 'a', 'b')
    chars.sort()
    println(chars)
    println(chars.joinToString(""))
    println(chars.argsort().joinToString())
    println(chars.sumOf { it.code.toLong() })
    println(('x'..'z').toCharList())

    val ints = IntList(4) { it * it }
    val asLongs = ints.mapToLong { it * 3_000_000_000L }
    println(asLongs)
    asLongs.sortByLong { -it }
    println(asLongs)
    println(ints.mapToDouble { it / 2.0 })
    println(ints.mapToChar { 'a' + it })
    println(chars.mapToInt { it.code })
    println(longs.subList(0, 2).sum())
    val view = chars.subList(1, 3)
    view.sortDescending()
    println(chars)
    println(ints.fold('a') { acc, v -> acc + v })
    println(ints.fold(false) { acc, v -> acc || v == 4 })

    longApis()
    doubleApis()
    charApis()
}

private fun longApis() {
    val a = buildLongList { add(3L); add(-1L); add(5_000_000_000L) }
    println(a.fold(0) { acc, v -> acc + if (v > 0) 1 else 0 })
    println(a.fold(0L) { acc, v -> acc + v })
    println(a.fold(0.5) { acc, v -> acc + v })
    println(a.fold('a') { acc, v -> if (v > 0) acc + 1 else acc })
    println(a.fold(false) { acc, v -> acc || v < 0 })
    println(a.sumOf { it * 2 })
    println(a.map { it + 1 })
    println(a.filter { it > 0 })
    println("${a.any { it < 0 }} ${a.all { it < 0 }} ${a.none { it == 0L }} ${a.count { it > 0 }}")
    println("${a.indexOfFirst { it < 0 }} ${a.indexOfLast { it > 0 }}")
    var total = 0L
    a.forEach { total += it }
    a.forEachIndexed { i, v -> total += i * v }
    a.forEachReversed { total -= it }
    for (v in a) total += v
    println(total)
    val view = a.subList(0, 2)
    var viewTotal = 0L
    view.forEach { viewTotal += it }
    view.forEachIndexed { i, v -> viewTotal += i * v }
    println("$viewTotal ${view.count { it > 0 }} ${view.any { it < 0 }} ${view.all { it < 0 }}")
    a.removeIf { it < 0 }
    println(a)
    println(longListOf().firstOrElse { 7L })
    println(LongList(3) { it * 2L })
    println((1L..9L step 4).toLongList())
    println((3L downTo 1L).toLongList())
    a.addAll(1L..2L)
    println(a)
}

private fun doubleApis() {
    val a = doubleListOf(1.5, -0.5, 4.0)
    println(a.fold(0) { acc, v -> acc + if (v > 0) 1 else 0 })
    println(a.fold(0L) { acc, v -> acc + v.toLong() })
    println(a.fold(0.5) { acc, v -> acc + v })
    println(a.fold('a') { acc, v -> if (v > 0) acc + 1 else acc })
    println(a.fold(false) { acc, v -> acc || v < 0 })
    println(a.sumOf { it.toLong() })
    println(a.map { it * 2 })
    println(a.filter { it > 0 })
    println("${a.any { it < 0 }} ${a.all { it < 0 }} ${a.none { it == 0.0 }} ${a.count { it > 0 }}")
    println("${a.indexOfFirst { it < 0 }} ${a.indexOfLast { it > 0 }}")
    var total = 0.0
    a.forEach { total += it }
    a.forEachIndexed { i, v -> total += i * v }
    a.forEachReversed { total -= it }
    for (v in a) total += v
    println(total)
    val view = a.subList(0, 2)
    var viewTotal = 0.0
    view.forEach { viewTotal += it }
    view.forEachIndexed { i, v -> viewTotal += i * v }
    println("$viewTotal ${view.count { it > 0 }} ${view.any { it < 0 }} ${view.all { it < 0 }}")
    a.removeIf { it < 0 }
    println(a)
    println(doubleListOf().firstOrElse { 7.0 })
    println(DoubleList(3) { it / 2.0 })
}

private fun charApis() {
    val a = charListOf('b', 'z', 'a')
    println(a.fold(0) { acc, v -> acc + v.code })
    println(a.fold(0L) { acc, v -> acc + v.code })
    println(a.fold(0.5) { acc, v -> acc + v.code })
    println(a.fold('a') { acc, v -> if (v > acc) v else acc })
    println(a.fold(false) { acc, v -> acc || v == 'z' })
    println(a.sumOf { it.code.toLong() })
    println(a.map { if (it == 'z') 'y' else it })
    println(a.filter { it < 'c' })
    println("${a.any { it == 'z' }} ${a.all { it == 'z' }} ${a.none { it == 'q' }} ${a.count { it < 'c' }}")
    println("${a.indexOfFirst { it == 'z' }} ${a.indexOfLast { it < 'c' }}")
    var total = 0
    a.forEach { total += it.code }
    a.forEachIndexed { i, v -> total += i * v.code }
    a.forEachReversed { total -= it.code }
    for (v in a) total += v.code
    println(total)
    val view = a.subList(0, 2)
    var viewTotal = 0
    view.forEach { viewTotal += it.code }
    view.forEachIndexed { i, v -> viewTotal += i * v.code }
    println("$viewTotal ${view.count { it < 'c' }} ${view.any { it == 'z' }} ${view.all { it == 'z' }}")
    a.removeIf { it == 'z' }
    println(a)
    println(charListOf().firstOrElse { 'q' })
    println(CharList(3) { 'a' + it })
    println(('a'..'g' step 3).toCharList())
    println(('c' downTo 'a').toCharList())
    a.addAll('x'..'y')
    println(a)
}
