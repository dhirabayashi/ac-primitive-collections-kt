// Exercises the pair heap API from user code. The strict boxing check runs on this file's MainKt.
import ac_primitive_collections.*

fun main() {
    val heap = LongIntMinHeap()
    heap.add(5L, 50)
    heap.add(1L, 10)
    heap.add(3L, 30)
    println("${heap.size} ${heap.isEmpty()} ${heap.isNotEmpty()} ${heap.firstKey()} ${heap.firstValue()}")
    heap.replaceFirst(4L, 40)
    println("${heap.firstKey()} ${heap.firstValue()}")
    var sum = 0L
    heap.forEachUnordered { key, value -> sum += key + value }
    println(sum)
    val copy = heap.copy()
    println(heap == copy)
    println(heap.hashCode() == copy.hashCode())
    while (copy.isNotEmpty()) {
        print("${copy.firstKey()}=${copy.firstValue()} ")
        copy.removeFirst()
    }
    println()
    println(copy.size)
    heap.ensureCapacity(100)
    println(heap.capacity)
    heap.clear()
    println(heap)
    println(LongIntMinHeap(4).capacity)
}
