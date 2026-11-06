// Compares IntList with ArrayList<Int> on AtCoder's code test (docs/PLAN.md, sections 1.2 S5 and 7.6).
// Bundle it with ./bundle -p. A code test is not a contest, so this is allowed.
// Standard input chooses one workload, so each measurement is its own cold JVM, as a submission is:
//   <ours|boxed> <elements>     such as "ours 1000000"
// Both sides do the same work on the same random values, and must print the same result.
import ac_primitive_collections.*

private class Values(private var state: Long = 88172645463325252L) {
    fun next(): Int {
        state = state xor (state shl 13)
        state = state xor (state ushr 7)
        state = state xor (state shl 17)
        return (state ushr 32).toInt()
    }
}

private fun ours(n: Int): Long {
    val values = Values()
    val list = IntList()
    for (i in 0 until n) list.add(values.next())
    var sum = 0L
    // Read in a random order, as graph and DP code often does.
    for (i in 0 until n) sum += list[(values.next() and Int.MAX_VALUE) % n]
    list.forEach { sum += it }
    list.sort()
    for (i in 0 until n) {
        val target = values.next()
        var lo = 0
        var hi = list.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (list[mid] < target) lo = mid + 1 else hi = mid
        }
        sum += lo
    }
    return sum
}

private fun boxed(n: Int): Long {
    val values = Values()
    val list = ArrayList<Int>()
    for (i in 0 until n) list.add(values.next())
    var sum = 0L
    for (i in 0 until n) sum += list[(values.next() and Int.MAX_VALUE) % n]
    list.forEach { sum += it }
    list.sort()
    for (i in 0 until n) {
        val target = values.next()
        var lo = 0
        var hi = list.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (list[mid] < target) lo = mid + 1 else hi = mid
        }
        sum += lo
    }
    return sum
}

fun main() {
    val tokens = java.util.StringTokenizer(readln())
    val kind = tokens.nextToken()
    val n = tokens.nextToken().toInt()
    val start = System.nanoTime()
    val result = if (kind == "ours") ours(n) else boxed(n)
    val millis = (System.nanoTime() - start) / 1_000_000
    println("kind=$kind n=$n ms=$millis result=$result")
}
