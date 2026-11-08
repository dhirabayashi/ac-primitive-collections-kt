// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeDequeConversions.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/** Copies into a standard list, from first to last. The elements box. */
@BoxingApi
fun CharDeque.toList(): List<Char> = toMutableList()

/** Copies into a standard mutable list, from first to last. The elements box. */
@BoxingApi
fun CharDeque.toMutableList(): MutableList<Char> {
    val result = ArrayList<Char>(length)
    forEach { result.add(it) }
    return result
}

/** Copies from a standard collection. Reading the elements unboxes them. */
@BoxingApi
fun Iterable<Char>.toCharDeque(): CharDeque {
    val result = CharDeque()
    result.addAll(this)
    return result
}

/** Appends from a standard collection. Reading the elements unboxes them. */
@BoxingApi
fun CharDeque.addAll(values: Iterable<Char>) {
    for (value in values) addLast(value)
}

fun CharArray.toCharDeque(): CharDeque {
    val result = CharDeque(size)
    result.addAll(this)
    return result
}

/** Takes a range, `step`, or `downTo`. Does not box, unlike the `Iterable` overload that this one takes precedence over. */
fun CharProgression.toCharDeque(): CharDeque {
    val result = CharDeque()
    result.addAll(this)
    return result
}

/** Takes a range, `step`, or `downTo`. Does not box, unlike the `Iterable` overload that this one takes precedence over. */
fun CharDeque.addAll(values: CharProgression) {
    for (value in values) addLast(value)
}
