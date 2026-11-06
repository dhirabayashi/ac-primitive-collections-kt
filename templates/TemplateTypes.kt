// SPDX-License-Identifier: CC0-1.0
// Makes the templates valid Kotlin for the IDE and the templateCheck compilation. Not generated.
@file:Suppress("NOTHING_TO_INLINE", "FunctionName")

package ac_primitive_collections

typealias KType = Int
typealias KTypeArray = IntArray
typealias KTypeIterator = IntIterator
typealias KTypeRange = IntRange
typealias K_SUM_TYPE = Long

const val K_ZERO: KType = 0
const val K_SUM_ZERO: K_SUM_TYPE = 0L

inline fun K_EQ(a: KType, b: KType): Boolean = a == b
inline fun K_LT(a: KType, b: KType): Boolean = a < b
inline fun K_HASH(x: KType): Int = x.hashCode()
inline fun K_TO_SUM(x: KType): K_SUM_TYPE = x.toLong()
