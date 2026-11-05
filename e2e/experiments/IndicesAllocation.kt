// SPDX-License-Identifier: CC0-1.0
// Run with: java -XX:+UseSerialGC -XX:ActiveProcessorCount=1 -XX:MaxHeapSize=1000341504 -cp m.jar MainKt {plain|until|direct|inline}
// The experiment behind docs/PLAN.md, section 4.8. Calling through (L) -> Long boxes the result, so subtract the "plain" baseline.
import java.lang.management.ManagementFactory
class L(val a: IntArray) {
    val size: Int get() = a.size
    operator fun get(i: Int) = a[i]
    val indicesUntil: IntRange get() = 0 until size
    val indicesDirect: IntRange get() = IntRange(0, size - 1)
    inline val indicesInline: IntRange get() = 0 until size
    inline fun forEach(action: (Int) -> Unit) { for (x in a) action(x) }
}
val mx = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
inline fun bytes(body: () -> Unit): Long { val id = Thread.currentThread().id; val s = mx.getThreadAllocatedBytes(id); body(); return mx.getThreadAllocatedBytes(id) - s }
fun until(l: L): Long { var s = 0L; for (i in l.indicesUntil) s += l[i]; return s }
fun direct(l: L): Long { var s = 0L; for (i in l.indicesDirect) s += l[i]; return s }
fun inl(l: L): Long { var s = 0L; for (i in l.indicesInline) s += l[i]; return s }
fun plain(l: L): Long { var s = 0L; for (i in 0 until l.size) s += l[i]; return s }
fun run(name: String, l: L, f: (L) -> Long) {
    var sink = 0L
    val first = bytes { sink += f(l) }
    repeat(200_000) { sink += f(l) }
    val after = bytes { repeat(10_000) { sink += f(l) } } / 10_000
    println("$name first=$first afterPerCall=$after sink=${sink and 1}")
}
fun main(args: Array<String>) {
    val l = L(IntArray(1000) { it })
    when (args[0]) { "until" -> run("until", l, ::until); "direct" -> run("direct", l, ::direct); "inline" -> run("inline", l, ::inl); "plain" -> run("plain", l, ::plain) }
}
