import ac_primitive_collections.*

/** Long, Double, and Char lists, the conversions between types, and the key sorts. */
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
}
