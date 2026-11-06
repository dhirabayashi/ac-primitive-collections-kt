// SPDX-License-Identifier: CC0-1.0
package codegen

/** Expands a template, which is itself valid Kotlin written against `typealias KType = Int`. */
object TemplateEngine {
    private val DIRECTIVE = Regex("""^\s*//#(if|else|endif)\b\s*(\S*)\s*$""")
    private val MACRO_NAME = Regex("""\b[KV]_[A-Z_]+\b""")
    private val MACRO_ARGUMENT = Regex("""\$[01]""")
    private val PAIR_CLASS_NAME = Regex("""\bKTypeVType\w+""")

    /** `/*<V>*/` keeps the template valid Kotlin for Int values and becomes `<V>` for a generic value. */
    private val COMMENTED_TYPE_ARGUMENTS = Regex("""/\*(<.*?>)\*/""")

    /**
     * [value] fills `VType` in templates that hold a key and a value.
     * [maxHeap] turns `MinHeap` into `MaxHeap` and makes `K_BEFORE(a, b)` mean "b is smaller".
     */
    fun expand(template: String, spec: TypeSpec, value: ValueSpec? = null, maxHeap: Boolean = false): String {
        val flags = spec.flags + value?.flags.orEmpty() + (if (maxHeap) setOf(MAX_HEAP_FLAG) else emptySet())
        val macros = spec.macros + value?.macros.orEmpty() + ("K_ZERO" to spec.zero) + ("K_BEFORE" to beforeBody(spec, maxHeap))
        val selected = applyConditionals(template, flags)
        var text = expandMacros(selected, spec.name, macros)
        if (value != null) {
            text = COMMENTED_TYPE_ARGUMENTS.replace(text) { if (value.isGeneric) it.groupValues[1] else "" }
            value.testClassTypeArgument?.let { argument ->
                text = PAIR_CLASS_NAME.replace(text) { if (it.value.endsWith("Test")) it.value else it.value + argument }
            }
            for ((token, replacement) in value.tokens) text = text.replace(token, replacement)
        }
        if (maxHeap) text = text.replace("MinHeap", "MaxHeap")
        return text
            .replace("KTYPE", spec.name.uppercase())
            .replace("KType", spec.name)
            .replace("ktype", spec.name.replaceFirstChar(Char::lowercase))
    }

    private fun beforeBody(spec: TypeSpec, maxHeap: Boolean): String {
        val lessThan = spec.macros.getValue("K_LT")
        return if (maxHeap) MACRO_ARGUMENT.replace(lessThan) { if (it.value == "\$0") "\$1" else "\$0" } else lessThan
    }

    private fun applyConditionals(template: String, flags: Set<String>): String {
        // Each entry says whether its branch is active, and whether #else was seen.
        class Frame(var active: Boolean, var sawElse: Boolean = false)

        val stack = ArrayDeque<Frame>()
        val out = StringBuilder()
        template.lineSequence().forEachIndexed { index, line ->
            val lineNumber = index + 1
            val directive = DIRECTIVE.find(line)
            if (directive == null) {
                require(!line.trimStart().startsWith("//#")) { "Unknown directive at line $lineNumber: $line" }
                if (stack.all { it.active }) out.append(line).append('\n')
                return@forEachIndexed
            }
            val (keyword, flag) = directive.destructured
            when (keyword) {
                "if" -> {
                    require(flag in KNOWN_FLAGS) { "Unknown flag at line $lineNumber: $flag" }
                    stack.addLast(Frame(flag in flags))
                }
                "else" -> {
                    val top = stack.lastOrNull() ?: error("#else without #if at line $lineNumber")
                    require(!top.sawElse) { "Second #else at line $lineNumber" }
                    top.sawElse = true
                    top.active = !top.active
                }
                else -> {
                    require(stack.isNotEmpty()) { "#endif without #if at line $lineNumber" }
                    stack.removeLast()
                }
            }
        }
        require(stack.isEmpty()) { "Unterminated #if" }
        return out.toString().removeSuffix("\n")
    }

    private fun expandMacros(text: String, typeName: String, macros: Map<String, String>): String {
        val out = StringBuilder()
        var position = 0
        while (true) {
            val match = MACRO_NAME.find(text, position)
            if (match == null) {
                out.append(text, position, text.length)
                return out.toString()
            }
            out.append(text, position, match.range.first)
            val name = match.value
            position = match.range.last + 1
            val body = macros[name] ?: error("Type $typeName does not define macro $name")
            if (position < text.length && text[position] == '(') {
                val (arguments, end) = parseArguments(text, position)
                var replaced = body
                arguments.forEachIndexed { i, argument ->
                    replaced = replaced.replace("$$i", expandMacros(argument, typeName, macros))
                }
                require('$' !in replaced) { "Macro $name got too few arguments: $arguments" }
                out.append(replaced)
                position = end
            } else {
                require('$' !in body) { "Macro $name needs arguments" }
                out.append(body)
            }
        }
    }

    /** Parses `(a, b)` at [open] and returns the trimmed arguments and the index after `)`. */
    private fun parseArguments(text: String, open: Int): Pair<List<String>, Int> {
        val arguments = mutableListOf<String>()
        var depth = 0
        var start = open + 1
        var i = open
        while (i < text.length) {
            when (text[i]) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> {
                    depth--
                    if (depth == 0) {
                        arguments += text.substring(start, i).trim()
                        return arguments to i + 1
                    }
                }
                ',' -> if (depth == 1) {
                    arguments += text.substring(start, i).trim()
                    start = i + 1
                }
            }
            i++
        }
        error("Unbalanced macro arguments at offset $open")
    }
}
