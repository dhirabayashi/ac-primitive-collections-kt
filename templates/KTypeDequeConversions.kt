// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

/** Copies into a standard list, from first to last. The elements box. */
@BoxingApi
fun KTypeDeque.toList(): List<KType> = toMutableList()

/** Copies into a standard mutable list, from first to last. The elements box. */
@BoxingApi
fun KTypeDeque.toMutableList(): MutableList<KType> {
    val result = ArrayList<KType>(length)
    forEach { result.add(it) }
    return result
}

/** Copies from a standard collection. Reading the elements unboxes them. */
@BoxingApi
fun Iterable<KType>.toKTypeDeque(): KTypeDeque {
    val result = KTypeDeque()
    result.addAll(this)
    return result
}

/** Appends from a standard collection. Reading the elements unboxes them. */
@BoxingApi
fun KTypeDeque.addAll(values: Iterable<KType>) {
    for (value in values) addLast(value)
}

fun KTypeArray.toKTypeDeque(): KTypeDeque {
    val result = KTypeDeque(size)
    result.addAll(this)
    return result
}

//#if K_HAS_RANGE
/** Takes a range, `step`, or `downTo`. Does not box, unlike the `Iterable` overload that this one takes precedence over. */
fun KTypeProgression.toKTypeDeque(): KTypeDeque {
    val result = KTypeDeque()
    result.addAll(this)
    return result
}

/** Takes a range, `step`, or `downTo`. Does not box, unlike the `Iterable` overload that this one takes precedence over. */
fun KTypeDeque.addAll(values: KTypeProgression) {
    for (value in values) addLast(value)
}
//#endif
