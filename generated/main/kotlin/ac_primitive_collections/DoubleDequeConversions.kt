// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeDequeConversions.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/** Copies into a standard list, from first to last. The elements box. */
@BoxingApi
fun DoubleDeque.toList(): List<Double> = toMutableList()

/** Copies into a standard mutable list, from first to last. The elements box. */
@BoxingApi
fun DoubleDeque.toMutableList(): MutableList<Double> {
    val result = ArrayList<Double>(length)
    forEach { result.add(it) }
    return result
}

/** Copies from a standard collection. Reading the elements unboxes them. */
@BoxingApi
fun Iterable<Double>.toDoubleDeque(): DoubleDeque {
    val result = DoubleDeque()
    result.addAll(this)
    return result
}

/** Appends from a standard collection. Reading the elements unboxes them. */
@BoxingApi
fun DoubleDeque.addAll(values: Iterable<Double>) {
    for (value in values) addLast(value)
}

fun DoubleArray.toDoubleDeque(): DoubleDeque {
    val result = DoubleDeque(size)
    result.addAll(this)
    return result
}

