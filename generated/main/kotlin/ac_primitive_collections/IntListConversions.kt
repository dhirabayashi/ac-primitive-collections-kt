// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeListConversions.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/** Copies into a standard list. The elements box. */
@BoxingApi
fun IntList.toList(): List<Int> = toMutableList()

/** Copies into a standard mutable list. The elements box. */
@BoxingApi
fun IntList.toMutableList(): MutableList<Int> {
    val result = ArrayList<Int>(length)
    forEach { result.add(it) }
    return result
}

/** Copies from a standard collection. Reading the elements unboxes them. */
@BoxingApi
fun Iterable<Int>.toIntList(): IntList {
    val result = IntList()
    result.addAll(this)
    return result
}

/** Appends from a standard collection. Reading the elements unboxes them. */
@BoxingApi
fun IntList.addAll(values: Iterable<Int>) {
    for (value in values) add(value)
}

fun IntArray.toIntList(): IntList = IntList(copyOf(), size)

/** Does not box, unlike the `Iterable` overload that this one takes precedence over. */
fun IntRange.toIntList(): IntList {
    val result = IntList()
    if (isEmpty()) return result
    var value = first
    while (true) {
        result.add(value)
        if (value == last) return result
        value++
    }
}
