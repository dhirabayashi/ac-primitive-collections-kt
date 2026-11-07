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

/** Only test templates use these: [toInt] converts an element, and [random] makes any element from a `Random`. */
private fun testMacros(toInt: String, random: String) = mapOf("K_TO_INT" to toInt, "K_RANDOM" to random)

private val comparableOperators = mapOf(
    "K_EQ" to "(\$0 == \$1)",
    "K_LT" to "(\$0 < \$1)",
    "K_HASH" to "\$0.hashCode()",
)

// IEEE operators disagree with Arrays.sort on NaN and -0.0, so Double uses the total order.
private val totalOrderOperators = mapOf(
    "K_EQ" to "(\$0.compareTo(\$1) == 0)",
    "K_LT" to "(\$0.compareTo(\$1) < 0)",
    "K_HASH" to "\$0.hashCode()",
)

/** Every key type that docs/PLAN.md section 5.1 plans for. [Codegen.GENERATED_TYPES] and [Codegen.GENERATED_PAIRS] select the active ones. */
val TYPE_SPECS: Map<String, TypeSpec> = listOf(
    TypeSpec(
        "Int", "0", setOf("K_NUMERIC", "K_INTEGRAL", "K_HAS_RANGE", "K_INT"),
        comparableOperators + numericSum("Long", "0L", "\$0.toLong()") + ("K_ARGSORT" to "\$0.argsortBy { it }") + testMacros("\$0", "\$0.nextInt()"),
    ),
    TypeSpec(
        "Long", "0L", setOf("K_NUMERIC", "K_INTEGRAL", "K_HAS_RANGE"),
        comparableOperators + numericSum("Long", "0L", "\$0") + ("K_ARGSORT" to "\$0.argsortByLong { it }") + testMacros("\$0.toInt()", "\$0.nextLong()"),
    ),
    TypeSpec(
        "Double", "0.0", setOf("K_NUMERIC", "K_FLOATING"),
        totalOrderOperators + numericSum("Double", "0.0", "\$0") + ("K_ARGSORT" to "\$0.argsortByDouble { it }") + testMacros("\$0.toInt()", "Double.fromBits(\$0.nextLong())"),
    ),
    TypeSpec("Char", "'\\u0000'", setOf("K_HAS_RANGE"), comparableOperators + ("K_ARGSORT" to "\$0.argsortBy { it.code }") + testMacros("\$0.code", "\$0.nextInt().toChar()")),
).associateBy { it.name }

/** How the value side of a pair template fills in. [tokens] apply in order. */
data class ValueSpec(
    val name: String,
    val flags: Set<String>,
    val macros: Map<String, String>,
    val tokens: List<Pair<String, String>>,
    /** In test templates, a generic class needs a type argument wherever it is named, except in the test's own name. */
    val testClassTypeArgument: String? = null,
) {
    val isGeneric: Boolean get() = name == OBJ
}

private const val OBJ = "Obj"

private fun primitiveValue(name: String) = ValueSpec(
    name,
    emptySet(),
    mapOf("V_NEW_ARRAY" to "${name}Array(\$0)", "V_CAST" to "\$0"),
    listOf("KTypeVType" to "KType$name", "VTypeArray" to "${name}Array", "VStored" to name, "VType" to name),
)

private val objValue = ValueSpec(
    OBJ,
    setOf("V_OBJ"),
    mapOf("V_NEW_ARRAY" to "arrayOfNulls<Any?>(\$0)", "V_CAST" to "(\$0 as V)", "V_NULL" to "null"),
    listOf("KTypeVType" to "KType$OBJ", "VTypeArray" to "Array<Any?>", "VStored" to "Any?", "VType" to "V"),
)

/** The target type of a conversion template. */
fun conversionTarget(typeName: String): ValueSpec = primitiveValue(typeName)

/** Values that docs/PLAN.md section 5.1 plans for pair heaps. */
val VALUE_SPECS: Map<String, ValueSpec> = listOf(primitiveValue("Int"), primitiveValue("Long"), objValue).associateBy { it.name }

/** The same, for test templates: an Obj heap is tested with `V = Int` so that the test bodies stay shared. */
val TEST_VALUE_SPECS: Map<String, ValueSpec> = VALUE_SPECS + (
    OBJ to objValue.copy(
        tokens = listOf("KTypeVType" to "KType$OBJ", "VType" to "Int"),
        testClassTypeArgument = "<Int>",
    )
)

/** Flags that come from the generator's choices rather than from a key type. */
const val MAX_HEAP_FLAG = "K_MAX_HEAP"

val KNOWN_FLAGS: Set<String> = TYPE_SPECS.values.flatMapTo(sortedSetOf()) { it.flags } +
    VALUE_SPECS.values.flatMap { it.flags } + MAX_HEAP_FLAG
