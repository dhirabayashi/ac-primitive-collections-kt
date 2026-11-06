// SPDX-License-Identifier: CC0-1.0
package codegen

import java.io.File

/** Maps templates to generated files. The output is deterministic so CI can compare it. */
object Codegen {
    /** Phase 1 runs the whole pipeline on Int only. See docs/PLAN.md, section 11. */
    val GENERATED_TYPES = listOf("Int")

    /** Key and value types of pair templates, from docs/PLAN.md section 5.1. Phase 2 starts with Dijkstra's pair. */
    val GENERATED_PAIRS = listOf("Long" to "Int")

    private const val TEMPLATE_PREFIX = "KType"
    private const val PAIR_TEMPLATE_PREFIX = "KTypeVType"
    // REUSE-IgnoreStart
    private const val SPDX = "// SPDX-License-Identifier: CC0-1.0\n"
    // REUSE-IgnoreEnd

    fun header(templateName: String) = SPDX +
        "// Generated from $templateName. Do not edit; run ./gradlew generateSources.\n" +
        "// Created with generative AI. AtCoder's rules require the reference URL when you copy this\n" +
        "// into a contest submission: https://github.com/dhirabayashi/ac-primitive-collections-kt\n"

    /** Returns output path (relative to the output root) to content. */
    fun generate(templateDir: File, types: List<String> = GENERATED_TYPES): Map<String, String> {
        val templates = templateDir.listFiles { file ->
            file.name.startsWith(TEMPLATE_PREFIX) && file.extension == "kt"
        }.orEmpty().sortedBy { it.name }
        val result = sortedMapOf<String, String>()
        for (template in templates) {
            val text = template.readText().replace("\r\n", "\n")
            require(text.startsWith(SPDX)) { "${template.name} must start with the SPDX line" }
            val body = text.removePrefix(SPDX)
            val label = "${templateDir.name}/${template.name}"
            if (template.name.startsWith(PAIR_TEMPLATE_PREFIX)) {
                for ((key, value) in GENERATED_PAIRS) {
                    val name = template.name.replace(PAIR_TEMPLATE_PREFIX, key + value)
                    result["ac_primitive_collections/$name"] = header(label) + TemplateEngine.expand(body, TYPE_SPECS.getValue(key), value)
                }
                continue
            }
            for (typeName in types) {
                val spec = TYPE_SPECS.getValue(typeName)
                val name = template.name.replace(TEMPLATE_PREFIX, spec.name)
                result["ac_primitive_collections/$name"] = header(label) + TemplateEngine.expand(body, spec)
            }
        }
        return result
    }
}
