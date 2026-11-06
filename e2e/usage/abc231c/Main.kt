// ABC 231 C - Counting 2. Solves it with IntList to prove the bundled library gets AC.
import ac_primitive_collections.*

fun main() {
    val first = java.util.StringTokenizer(readln())
    val n = first.nextToken().toInt()
    val q = first.nextToken().toInt()
    val heights = IntList(n)
    val tokens = java.util.StringTokenizer(readln())
    repeat(n) { heights.add(tokens.nextToken().toInt()) }
    heights.sort()
    val out = StringBuilder()
    repeat(q) {
        val x = readln().toInt()
        out.append(n - heights.lowerBound(x)).append('\n')
    }
    print(out)
}
