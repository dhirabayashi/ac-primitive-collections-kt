// SPDX-License-Identifier: CC0-1.0
// Generated from templates/KTypeListMapToVType.kt. Do not edit; run ./gradlew generateSources.
// Created with generative AI. AtCoder's rules require the reference URL when you copy this
// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt
package ac_primitive_collections

/** Lives in its own file, so that a submission gets the target type only when it converts to it. */
inline fun IntList.mapToLong(transform: (Int) -> Long): LongList {
    val result = LongList(length)
    forEach { result.add(transform(it)) }
    return result
}
