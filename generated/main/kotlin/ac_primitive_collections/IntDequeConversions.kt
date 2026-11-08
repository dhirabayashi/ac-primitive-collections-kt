// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeDequeConversions.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/** Copies into a standard list, from first to last. The elements box. */
@BoxingApi
fun IntDeque.toList(): List<Int> = toMutableList()

/** Copies into a standard mutable list, from first to last. The elements box. */
@BoxingApi
fun IntDeque.toMutableList(): MutableList<Int> {
    val result = ArrayList<Int>(length)
    forEach { result.add(it) }
    return result
}

/** Copies from a standard collection. Reading the elements unboxes them. */
@BoxingApi
fun Iterable<Int>.toIntDeque(): IntDeque {
    val result = IntDeque()
    result.addAll(this)
    return result
}

/** Appends from a standard collection. Reading the elements unboxes them. */
@BoxingApi
fun IntDeque.addAll(values: Iterable<Int>) {
    for (value in values) addLast(value)
}

fun IntArray.toIntDeque(): IntDeque {
    val result = IntDeque(size)
    result.addAll(this)
    return result
}

/** Takes a range, `step`, or `downTo`. Does not box, unlike the `Iterable` overload that this one takes precedence over. */
fun IntProgression.toIntDeque(): IntDeque {
    val result = IntDeque()
    result.addAll(this)
    return result
}

/** Takes a range, `step`, or `downTo`. Does not box, unlike the `Iterable` overload that this one takes precedence over. */
fun IntDeque.addAll(values: IntProgression) {
    for (value in values) addLast(value)
}
