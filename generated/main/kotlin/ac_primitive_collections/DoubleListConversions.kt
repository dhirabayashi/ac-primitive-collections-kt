// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeListConversions.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/** Copies into a standard list. The elements box. */
@BoxingApi
fun DoubleList.toList(): List<Double> = toMutableList()

/** Copies into a standard mutable list. The elements box. */
@BoxingApi
fun DoubleList.toMutableList(): MutableList<Double> {
    val result = ArrayList<Double>(length)
    forEach { result.add(it) }
    return result
}

/** Copies from a standard collection. Reading the elements unboxes them. */
@BoxingApi
fun Iterable<Double>.toDoubleList(): DoubleList {
    val result = DoubleList()
    result.addAll(this)
    return result
}

/** Appends from a standard collection. Reading the elements unboxes them. */
@BoxingApi
fun DoubleList.addAll(values: Iterable<Double>) {
    for (value in values) add(value)
}

fun DoubleArray.toDoubleList(): DoubleList = DoubleList(copyOf(), size)

