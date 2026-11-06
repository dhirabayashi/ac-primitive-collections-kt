// SPDX-License-Identifier: CC0-1.0
package codegen

/** How one element type fills in the template tokens, macros, and flags. */
data class TypeSpec(
    val name: String,
    val zero: String,
    val flags: Set<String>,
    /** Macro bodies. `$0`, `$1` stand for the macro's arguments. */
    val macros: Map<String, String>,
)

private fun numericSum(sumType: String, sumZero: String, toSum: String) = mapOf(
    "K_SUM_TYPE" to sumType,
    "K_SUM_ZERO" to sumZero,
    "K_TO_SUM" to toSum,
)

private val comparableOperators = mapOf(
    "K_EQ" to "(\$0 == \$1)",
    "K_LT" to "(\$0 < \$1)",
    "K_HASH" to "\$0.hashCode()",
)

// IEEE operators disagree with Arrays.sort on NaN and -0.0, so floating types use the total order.
private val totalOrderOperators = mapOf(
    "K_EQ" to "(\$0.compareTo(\$1) == 0)",
    "K_LT" to "(\$0.compareTo(\$1) < 0)",
    "K_HASH" to "\$0.hashCode()",
)

/** Every type that docs/PLAN.md section 5.1 plans for. [Codegen.GENERATED_TYPES] selects the active ones. */
val TYPE_SPECS: Map<String, TypeSpec> = listOf(
    TypeSpec(
        "Int", "0", setOf("K_NUMERIC", "K_INTEGRAL", "K_HAS_RANGE", "K_INT"),
        comparableOperators + numericSum("Long", "0L", "\$0.toLong()"),
    ),
    TypeSpec(
        "Long", "0L", setOf("K_NUMERIC", "K_INTEGRAL", "K_HAS_RANGE"),
        comparableOperators + numericSum("Long", "0L", "\$0"),
    ),
    TypeSpec(
        "Double", "0.0", setOf("K_NUMERIC", "K_FLOATING"),
        totalOrderOperators + numericSum("Double", "0.0", "\$0"),
    ),
    TypeSpec(
        "Float", "0f", setOf("K_NUMERIC", "K_FLOATING"),
        totalOrderOperators + numericSum("Double", "0.0", "\$0.toDouble()"),
    ),
    TypeSpec(
        "Short", "0", setOf("K_NUMERIC", "K_INTEGRAL"),
        comparableOperators + numericSum("Long", "0L", "\$0.toLong()"),
    ),
    TypeSpec(
        "Byte", "0", setOf("K_NUMERIC", "K_INTEGRAL"),
        comparableOperators + numericSum("Long", "0L", "\$0.toLong()"),
    ),
    TypeSpec("Char", "'\\u0000'", setOf("K_HAS_RANGE"), comparableOperators),
    TypeSpec("Boolean", "false", setOf("K_BOOLEAN"), comparableOperators),
).associateBy { it.name }

val KNOWN_FLAGS: Set<String> = TYPE_SPECS.values.flatMapTo(sortedSetOf()) { it.flags }
