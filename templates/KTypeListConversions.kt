// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

/** Copies into a standard list. The elements box. */
@BoxingApi
fun KTypeList.toList(): List<KType> = toMutableList()

/** Copies into a standard mutable list. The elements box. */
@BoxingApi
fun KTypeList.toMutableList(): MutableList<KType> {
    val result = ArrayList<KType>(length)
    forEach { result.add(it) }
    return result
}

/** Copies from a standard collection. Reading the elements unboxes them. */
@BoxingApi
fun Iterable<KType>.toKTypeList(): KTypeList {
    val result = KTypeList()
    result.addAll(this)
    return result
}

/** Appends from a standard collection. Reading the elements unboxes them. */
@BoxingApi
fun KTypeList.addAll(values: Iterable<KType>) {
    for (value in values) add(value)
}

fun KTypeArray.toKTypeList(): KTypeList = KTypeList(copyOf(), size)

//#if K_HAS_RANGE
/** Takes a range, `step`, or `downTo`. Does not box, unlike the `Iterable` overload that this one takes precedence over. */
fun KTypeProgression.toKTypeList(): KTypeList {
    val result = KTypeList()
    result.addAll(this)
    return result
}

/** Takes a range, `step`, or `downTo`. Does not box, unlike the `Iterable` overload that this one takes precedence over. */
fun KTypeList.addAll(values: KTypeProgression) {
    for (value in values) add(value)
}
//#endif
