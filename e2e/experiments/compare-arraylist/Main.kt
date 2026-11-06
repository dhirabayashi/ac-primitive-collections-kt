// Compares IntList with ArrayList<Int> on AtCoder's code test (docs/PLAN.md, sections 1.2 S5 and 7.6).
// Bundle it with ./bundle --allow-unpublished. A code test is not a contest, so this is allowed.
// Standard input chooses one workload, so each measurement is its own cold JVM, as a submission is:
//   <ours|boxed> <elements>     such as "ours 5000000"
// Compare several runs of each. Read the code test's reported memory as well as the output.
import ac_primitive_collections.*

private fun ours(n: Int): Long {
    val list = IntList()
    for (i in 0 until n) list.add(i xor (i shl 3))
    var sum = 0L
    for (i in 0 until list.size) sum += list[i]
    list.forEach { sum += it }
    list.sort()
    for (i in 0 until n step 16) sum += list.lowerBound(i)
    return sum
}

private fun boxed(n: Int): Long {
    val list = ArrayList<Int>()
    for (i in 0 until n) list.add(i xor (i shl 3))
    var sum = 0L
    for (i in 0 until list.size) sum += list[i]
    list.forEach { sum += it }
    list.sort()
    for (i in 0 until n step 16) {
        var lo = 0
        var hi = list.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (list[mid] < i) lo = mid + 1 else hi = mid
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
    val runtime = Runtime.getRuntime()
    println("kind=$kind n=$n ms=$millis result=$result heapUsedMiB=${(runtime.totalMemory() - runtime.freeMemory()) shr 20}")
}
