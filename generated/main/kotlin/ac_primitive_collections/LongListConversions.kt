// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeListConversions.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/** Copies into a standard list. The elements box. */
@BoxingApi
fun LongList.toList(): List<Long> = toMutableList()

/** Copies into a standard mutable list. The elements box. */
@BoxingApi
fun LongList.toMutableList(): MutableList<Long> {
    val result = ArrayList<Long>(length)
    forEach { result.add(it) }
    return result
}

/** Copies from a standard collection. Reading the elements unboxes them. */
@BoxingApi
fun Iterable<Long>.toLongList(): LongList {
    val result = LongList()
    result.addAll(this)
    return result
}

/** Appends from a standard collection. Reading the elements unboxes them. */
@BoxingApi
fun LongList.addAll(values: Iterable<Long>) {
    for (value in values) add(value)
}

fun LongArray.toLongList(): LongList = LongList(copyOf(), size)

/** Takes a range, `step`, or `downTo`. Does not box, unlike the `Iterable` overload that this one takes precedence over. */
fun LongProgression.toLongList(): LongList {
    val result = LongList()
    result.addAll(this)
    return result
}

/** Takes a range, `step`, or `downTo`. Does not box, unlike the `Iterable` overload that this one takes precedence over. */
fun LongList.addAll(values: LongProgression) {
    for (value in values) add(value)
}
