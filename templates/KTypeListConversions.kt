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
/** Does not box, unlike the `Iterable` overload that this one takes precedence over. */
fun KTypeRange.toKTypeList(): KTypeList {
    val result = KTypeList()
    if (isEmpty()) return result
    var value = first
    while (true) {
        result.add(value)
        if (value == last) return result
        value++
    }
}
//#endif
