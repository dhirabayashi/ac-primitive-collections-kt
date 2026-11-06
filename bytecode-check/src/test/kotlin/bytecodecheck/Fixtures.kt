// SPDX-License-Identifier: CC0-1.0
package bytecodecheck.fixtures

// The checker must flag each of these. They are compiled by the same Kotlin as the library.

@Retention(AnnotationRetention.BINARY)
annotation class BoxingApi

class Clean(private val a: IntArray) {
    fun get(i: Int): Int = a[i]
    inline fun each(action: (Int) -> Unit) {
        for (x in a0()) action(x)
    }
    fun a0() = a
}

class ReturnsNullable {
    fun find(x: Int): Int? = if (x > 0) x else null
}

class TakesBoxed {
    fun put(x: Int?) = x?.hashCode()
}

class HasBoxedField {
    @JvmField var cached: Int? = null
}

class UsesList {
    private val list = ArrayList<Int>()
    private fun add(x: Int) { list.add(x) }
    private fun read(): Int = list[0]
    fun run(x: Int): Int { add(x); return read() }
}

class StoresLambda(private val f: (Int) -> Int) {
    fun run(x: Int): Int = f(x)
}

class NonInlineTakesLambda {
    fun run(x: Int, f: (Int) -> Int): Int = f(x)
}

class MakesPair {
    private val p = Pair(1, 2)
    fun first(): Any = p.first
}

class Annotated {
    @BoxingApi
    fun toList(a: IntArray): List<Int> = a.toList()
}

fun userCodeInlinesEach(c: Clean): Int {
    var sum = 0
    c.each { sum += it }
    return sum
}

// Mimics pasted code, where the annotation sits in the default package: see BoxingCheckerTest.
