// SPDX-License-Identifier: CC0-1.0
// Compile with kotlinc 2.2.10 and read the bytecode with javap.
// The experiment behind docs/PLAN.md, sections 4.6 and 4.8.
class L(var a: IntArray = IntArray(0)) {
    operator fun get(i: Int): Int = a[i]
    operator fun set(i: Int, v: Int) { a[i] = v }
    operator fun contains(v: Int): Boolean { for (x in a) if (x == v) return true; return false }
    operator fun plus(v: Int): L = L(a + v)
    operator fun plusAssign(v: Int) { a = a + v }
    operator fun iterator(): IntIterator = a.iterator()
}
class M(val k: IntArray, val v: IntArray) {
    operator fun get(key: Int): Int { for (i in k.indices) if (k[i] == key) return v[i]; return 0 }
    operator fun set(key: Int, value: Int) { for (i in k.indices) if (k[i] == key) v[i] = value }
    operator fun iterator(): Cursor = Cursor(this)
    class Cursor(private val m: M) { 
        private var i = -1
        operator fun hasNext() = i + 1 < m.k.size
        operator fun next(): Cursor { i++; return this }
        operator fun component1(): Int = m.k[i]
        operator fun component2(): Int = m.v[i]
    }
}
@JvmInline value class P(val packed: Long) {
    operator fun component1(): Int = (packed ushr 32).toInt()
    operator fun component2(): Int = packed.toInt()
}
fun useVal(): Int { val l = L(); l += 1; l[0] = 2; l[0] += 3; l[0]++; return if (2 in l) l[0] else 0 }
fun usePlus(l: L): L = l + 1
fun useMap(m: M, k: Int) { m[k] += 1; m[k]++ }
fun useFor(m: M): Long { var s = 0L; for ((k, v) in m) s += k + v; return s }
fun usePair(p: P): Int { val (a, b) = p; return a + b }
class TypedList(private val a: IntArray) : AbstractList<Int>() {
    override val size: Int get() = a.size
    override fun get(index: Int): Int = a[index]
}
fun typedIndex(l: TypedList): Long { var s = 0L; for (i in 0 until l.size) s += l[i]; return s }
fun plainFor(l: L): Long { var s = 0L; for (x in l) s += x; return s }
