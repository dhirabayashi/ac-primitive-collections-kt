// Uses each dimension of the pair heap family from user code: key type, value type, and direction.
// The strict boxing check runs on this file's MainKt.
import ac_primitive_collections.*

fun main() {
    val maxHeap = IntIntMaxHeap()
    maxHeap.add(1, 10)
    maxHeap.add(3, 30)
    maxHeap.add(2, 20)
    print("IntIntMaxHeap")
    while (maxHeap.isNotEmpty()) {
        print(" ${maxHeap.firstKey()}=${maxHeap.firstValue()}")
        maxHeap.removeFirst()
    }
    println()

    val longs = IntLongMinHeap(1)
    longs.add(5, 5_000_000_000L)
    longs.add(4, 4_000_000_000L)
    println("IntLongMinHeap ${longs.firstKey()}=${longs.firstValue()}")

    val doubles = DoubleIntMinHeap()
    for (key in doubleArrayOf(Double.NaN, 0.0, -0.0, Double.NEGATIVE_INFINITY)) doubles.add(key, 1)
    print("DoubleIntMinHeap")
    while (doubles.isNotEmpty()) {
        print(" ${doubles.firstKey()}")
        doubles.removeFirst()
    }
    println()

    val largest = DoubleLongMaxHeap()
    largest.add(1.5, 15L)
    largest.add(Double.NaN, 0L)
    largest.add(2.5, 25L)
    largest.removeFirst()
    println("DoubleLongMaxHeap ${largest.firstKey()}=${largest.firstValue()}")

    val tasks = LongObjMinHeap<String>()
    tasks.add(3L, "c")
    tasks.add(1L, "a")
    tasks.add(2L, "b")
    tasks.replaceFirst(4L, "d")
    val names = StringBuilder()
    tasks.forEachUnordered { _, value -> names.append(value.length) }
    println("LongObjMinHeap ${tasks.firstKey()}=${tasks.firstValue()} ${tasks.size} $names")

    val copy = tasks.copy()
    println(copy == tasks)
    copy.clear()
    println(copy)
    println(tasks.size)

    val objects = DoubleObjMaxHeap<IntArray>(0)
    objects.add(1.0, intArrayOf(7, 8))
    objects.add(2.0, intArrayOf(9))
    println("DoubleObjMaxHeap ${objects.firstKey()}=${objects.firstValue()[0]}")
}
