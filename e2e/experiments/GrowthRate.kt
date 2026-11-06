// Decides the growth factor in nextCapacity (docs/PLAN.md, section 7.6) on AtCoder's code test.
// Each run is one cold JVM, as a real submission is. Standard input: "<factor> <elements>", such as "1.5 10000000".
// Compare several runs of each factor. Read the code test's reported memory as well as the output.
fun main() {
    val (factorText, elementsText) = readln().split(" ")
    val factor = factorText.toDouble()
    val elements = elementsText.toInt()
    val start = System.nanoTime()
    var array = IntArray(8)
    var size = 0
    var copied = 0L
    for (i in 0 until elements) {
        if (size == array.size) {
            val next = maxOf((array.size * factor).toLong(), array.size + 1L).toInt()
            copied += size
            array = array.copyOf(next)
        }
        array[size++] = i
    }
    val millis = (System.nanoTime() - start) / 1_000_000
    val runtime = Runtime.getRuntime()
    println("factor=$factor elements=$elements ms=$millis copied=$copied capacity=${array.size} sum=${array.sum()} heapUsedMiB=${(runtime.totalMemory() - runtime.freeMemory()) shr 20}")
}
