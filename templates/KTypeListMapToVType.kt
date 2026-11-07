// SPDX-License-Identifier: CC0-1.0
package ac_primitive_collections

/** Lives in its own file, so that a submission gets the target type only when it converts to it. */
inline fun KTypeList.mapToVType(transform: (KType) -> VType): VTypeList {
    val result = VTypeList(length)
    forEach { result.add(transform(it)) }
    return result
}
