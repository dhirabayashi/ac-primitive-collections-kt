// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeDequeConversions.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/** Copies into a standard list, from first to last. The elements box. */
@BoxingApi
fun LongDeque.toList(): List<Long> = toMutableList()

/** Copies into a standard mutable list, from first to last. The elements box. */
@BoxingApi
fun LongDeque.toMutableList(): MutableList<Long> {
    val result = ArrayList<Long>(length)
    forEach { result.add(it) }
    return result
}

/** Copies from a standard collection. Reading the elements unboxes them. */
@BoxingApi
fun Iterable<Long>.toLongDeque(): LongDeque {
    val result = LongDeque()
    result.addAll(this)
    return result
}

/** Appends from a standard collection. Reading the elements unboxes them. */
@BoxingApi
fun LongDeque.addAll(values: Iterable<Long>) {
    for (value in values) addLast(value)
}

fun LongArray.toLongDeque(): LongDeque {
    val result = LongDeque(size)
    result.addAll(this)
    return result
}

/** Takes a range, `step`, or `downTo`. Does not box, unlike the `Iterable` overload that this one takes precedence over. */
fun LongProgression.toLongDeque(): LongDeque {
    val result = LongDeque()
    result.addAll(this)
    return result
}

/** Takes a range, `step`, or `downTo`. Does not box, unlike the `Iterable` overload that this one takes precedence over. */
fun LongDeque.addAll(values: LongProgression) {
    for (value in values) addLast(value)
}
