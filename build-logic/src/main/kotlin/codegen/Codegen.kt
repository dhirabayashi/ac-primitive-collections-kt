// SPDX-License-Identifier: CC0-1.0
package codegen

import java.io.File

/** Maps templates to generated files. The output is deterministic so CI can compare it. */
object Codegen {
    /** Element types of single-type templates, from docs/PLAN.md section 5.1. */
    val GENERATED_TYPES = listOf("Int", "Long", "Double", "Char")

    /**
     * Key and value types of pair templates, from docs/PLAN.md section 5.1.
     * These are the pair heaps' combinations. Other collections need their own: see Phase 3 in that document.
     */
    val GENERATED_PAIRS = listOf("Int", "Long", "Double").flatMap { key -> listOf("Int", "Long", "Obj").map { key to it } }

    private const val TEMPLATE_PREFIX = "KType"
    private const val PAIR_TEMPLATE_PREFIX = "KTypeVType"

    /** A name with this holds a conversion, expanded once for each pair of different element types. */
    private const val CONVERSION_TARGET = "VType"
    const val MIN_HEAP = "MinHeap"
    const val MAX_HEAP = "MaxHeap"
    // REUSE-IgnoreStart
    private const val SPDX = "// SPDX-License-Identifier: CC0-1.0\n"
    // REUSE-IgnoreEnd

    /** Starts the second line of every generated file. The mutation gate reads the template's name from it. */
    const val GENERATED_FROM = "// Generated from "

    fun header(templateName: String) = SPDX +
        "$GENERATED_FROM$templateName. Do not edit; run ./gradlew generateSources.\n" +
        "// Created with generative AI. AtCoder's rules require the reference URL when you copy this\n" +
        "// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt\n"

    /** Returns output path (relative to the output root) to content. */
    fun generate(templateDir: File): Map<String, String> {
        val templates = templateDir.listFiles { file ->
            file.name.startsWith(TEMPLATE_PREFIX) && file.extension == "kt"
        }.orEmpty().sortedBy { it.name }
        val result = sortedMapOf<String, String>()
        for (template in templates) {
            val text = template.readText().replace("\r\n", "\n")
            require(text.startsWith(SPDX)) { "${template.name} must start with the SPDX line" }
            val body = text.removePrefix(SPDX)
            val label = "${templateDir.name}/${template.name}"
            // A heap template holds both directions, so that Max cannot drift from Min.
            val directions = if (MIN_HEAP in template.name) listOf(false, true) else listOf(false)
            for (maxHeap in directions) {
                val fileName = if (maxHeap) template.name.replace(MIN_HEAP, MAX_HEAP) else template.name
                if (template.name.startsWith(PAIR_TEMPLATE_PREFIX)) {
                    val values = if (template.name.endsWith("Test.kt")) TEST_VALUE_SPECS else VALUE_SPECS
                    for ((key, value) in GENERATED_PAIRS) {
                        val name = fileName.replace(PAIR_TEMPLATE_PREFIX, key + value)
                        result["ac_primitive_collections/$name"] = header(label) +
                            TemplateEngine.expand(body, TYPE_SPECS.getValue(key), values.getValue(value), maxHeap)
                    }
                    continue
                }
                if (CONVERSION_TARGET in template.name) {
                    for (source in GENERATED_TYPES) for (target in GENERATED_TYPES - source) {
                        val name = fileName.replace(CONVERSION_TARGET, target).replace(TEMPLATE_PREFIX, source)
                        result["ac_primitive_collections/$name"] = header(label) +
                            TemplateEngine.expand(body, TYPE_SPECS.getValue(source), conversionTarget(target))
                    }
                    continue
                }
                for (typeName in GENERATED_TYPES) {
                    val spec = TYPE_SPECS.getValue(typeName)
                    val name = fileName.replace(TEMPLATE_PREFIX, spec.name)
                    result["ac_primitive_collections/$name"] = header(label) + TemplateEngine.expand(body, spec, maxHeap = maxHeap)
                }
            }
        }
        return result
    }
}
