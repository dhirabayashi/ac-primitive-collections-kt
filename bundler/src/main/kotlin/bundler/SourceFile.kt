// SPDX-License-Identifier: CC0-1.0
package bundler

/** A top-level declaration. [receiver] is the text before the last dot, for extensions. */
data class Declaration(
    val name: String,
    val receiver: String?,
    val keyword: String,
    val visibility: Visibility,
    val isOperator: Boolean,
) {
    enum class Visibility { PUBLIC, INTERNAL, PRIVATE }

    val isType get() = keyword in TYPE_KEYWORDS

    companion object {
        val TYPE_KEYWORDS = setOf("class", "interface", "object", "typealias")
    }
}

/** A parsed source file: its header lines, body, declarations, and every identifier it mentions. */
class SourceFile(val name: String, val text: String) {
    val fileAnnotations: List<String>
    val imports: List<String>
    val packageName: String?
    val body: String
    val lexed: LexedSource = lex(text)
    val identifiers: Set<String>

    /**
     * Each public function's name, members' too, to the identifiers of its declared return type. Overloads merge.
     * An inferred return type is not seen; the library's SourceRulesTest checks this against the compiled classes.
     */
    val publicReturnTypes: Map<String, Set<String>>
    val declarations: List<Declaration>

    init {
        val annotations = ArrayList<String>()
        val importLines = ArrayList<String>()
        var pkg: String? = null
        var bodyStart = 0
        var offset = 0
        for (rawLine in text.split('\n')) {
            val line = rawLine.trim()
            val nextOffset = offset + rawLine.length + 1
            when {
                line.startsWith("@file:") -> annotations += line
                line.startsWith("package ") -> pkg = line.removePrefix("package ").trim()
                line.startsWith("import ") -> importLines += line.removePrefix("import ").trim()
                line.isEmpty() || line.startsWith("//") -> Unit
                else -> break
            }
            if (line.isNotEmpty() && !line.startsWith("//")) bodyStart = minOf(nextOffset, text.length)
            offset = nextOffset
        }
        fileAnnotations = annotations
        imports = importLines
        packageName = pkg
        body = text.substring(bodyStart).trim('\n')
        // Tokens of the header would make every `import x.y` look like a use of `y`.
        val bodyTokens = lexed.tokens.filter { it.start >= bodyStart }
        identifiers = bodyTokens.filter { it.isIdentifier }.mapTo(HashSet()) { it.text }
        declarations = parseTopLevel(bodyTokens)
        publicReturnTypes = publicFunctions(bodyTokens).groupBy { it.first }.mapValues { (_, overloads) -> overloads.flatMapTo(HashSet()) { it.second } }
    }

    /** The body without comments, and without blank lines left behind. For `--strip-comments`. */
    fun bodyWithoutComments(): String {
        val offset = text.indexOf(body)
        val builder = StringBuilder()
        var position = offset
        val end = offset + body.length
        for (comment in lexed.comments) {
            if (comment.first < offset) continue
            builder.append(text, position, comment.first)
            position = comment.last + 1
        }
        builder.append(text, position, end)
        return builder.toString().lines().map { it.trimEnd() }.filter { it.isNotEmpty() }.joinToString("\n")
    }
}

/** True if [name] is declared with `fun` inside braces, even if it is also declared at the top level. */
private val MODIFIERS = setOf(
    "public", "internal", "private", "protected", "inline", "operator", "infix", "tailrec", "external", "suspend",
    "const", "data", "value", "annotation", "enum", "sealed", "abstract", "open", "final", "expect", "actual", "fun", "lateinit",
)

/** Finds every public `fun` at any depth, with the identifiers of its declared return type. Internal and private ones are skipped. */
private fun publicFunctions(tokens: List<Token>): List<Pair<String, Set<String>>> {
    val found = ArrayList<Pair<String, Set<String>>>()
    for (i in tokens.indices) {
        if (tokens[i].text != "fun" || !tokens[i].isIdentifier || !isDeclarationKeyword(tokens, i)) continue
        if (tokens.getOrNull(i + 1)?.text == "interface" || modifiersOf(tokens, i).any { it == "private" || it == "internal" }) continue
        // The name is the identifier before the parameters' `(`, outside the type parameters.
        var j = i + 1
        var angle = 0
        while (j < tokens.size && !(angle == 0 && tokens[j].text == "(" && tokens[j].kind == Token.Kind.SYMBOL)) {
            if (tokens[j].text == "<") angle++
            if (tokens[j].text == ">") angle--
            j++
        }
        val name = tokens.getOrNull(j - 1)?.takeIf { it.isIdentifier }?.text ?: continue
        j = skipBalanced(tokens, j)
        val returnType = HashSet<String>()
        if (tokens.getOrNull(j)?.text == ":") {
            j++
            var depth = 0
            while (j < tokens.size) {
                val token = tokens[j]
                if (token.kind == Token.Kind.SYMBOL) {
                    when (token.text) {
                        "(", "<", "[" -> depth++
                        ")", ">", "]" -> depth--
                        "{", "}", "=" -> if (depth == 0) break
                    }
                } else if (token.isIdentifier) {
                    if (depth == 0 && token.text in DECLARATION_STARTS) break
                    returnType += token.text
                }
                j++
            }
        }
        found += name to returnType
    }
    return found
}

/** Words that start the next declaration, which ends a bodiless function's return type. */
private val DECLARATION_STARTS = setOf("fun", "val", "var", "class", "interface", "object", "override", "operator", "inline", "private", "internal")

/** Returns the index after the bracket group that opens at [open]. */
private fun skipBalanced(tokens: List<Token>, open: Int): Int {
    var depth = 0
    var j = open
    while (j < tokens.size) {
        val token = tokens[j]
        if (token.kind == Token.Kind.SYMBOL) {
            if (token.text == "(" || token.text == "[" || token.text == "{") depth++
            if (token.text == ")" || token.text == "]" || token.text == "}") depth--
        }
        j++
        if (depth == 0) break
    }
    return j
}

/** The modifiers before the keyword at [keywordIndex], skipping annotations. */
private fun modifiersOf(tokens: List<Token>, keywordIndex: Int): List<String> {
    val modifiers = ArrayList<String>()
    var back = keywordIndex - 1
    while (back >= 0) {
        val text = tokens[back].text
        if (text in MODIFIERS && tokens[back].isIdentifier) {
            modifiers += text
            back--
        } else if (tokens[back].isIdentifier && tokens.getOrNull(back - 1)?.text == "@") {
            back -= 2
        } else {
            break
        }
    }
    return modifiers
}

/** Finds declarations at bracket depth zero, without a full parser. */
private fun parseTopLevel(tokens: List<Token>): List<Declaration> {
    val declarations = ArrayList<Declaration>()
    var depth = 0
    var i = 0
    while (i < tokens.size) {
        val token = tokens[i]
        when (token.text) {
            "(", "[", "{", "\${" -> if (token.kind != Token.Kind.IDENTIFIER) depth++
            ")", "]", "}" -> if (token.kind != Token.Kind.IDENTIFIER) depth--
        }
        if (depth == 0 && token.isIdentifier && isDeclarationKeyword(tokens, i)) {
            val parsed = parseDeclaration(tokens, i)
            if (parsed != null) declarations += parsed
        }
        i++
    }
    return declarations
}

private fun isDeclarationKeyword(tokens: List<Token>, i: Int): Boolean {
    val previous = tokens.getOrNull(i - 1)?.text
    if (previous == "." || previous == "::") return false
    return when (tokens[i].text) {
        "fun" -> tokens.getOrNull(i + 1)?.text != "interface" && tokens.getOrNull(i + 1)?.text != "("
        "val", "var", "class", "interface", "typealias" -> true
        "object" -> tokens.getOrNull(i + 1)?.isIdentifier == true
        else -> false
    }
}

private fun parseDeclaration(tokens: List<Token>, keywordIndex: Int): Declaration? {
    val keyword = tokens[keywordIndex].text
    var visibility = Declaration.Visibility.PUBLIC
    var isOperator = false
    var back = keywordIndex - 1
    while (back >= 0) {
        val text = tokens[back].text
        if (text in MODIFIERS && tokens[back].isIdentifier) {
            if (text == "private") visibility = Declaration.Visibility.PRIVATE
            if (text == "internal") visibility = Declaration.Visibility.INTERNAL
            if (text == "operator") isOperator = true
            back--
        } else if (tokens[back].isIdentifier && tokens.getOrNull(back - 1)?.text == "@") {
            back -= 2
        } else {
            break
        }
    }

    var i = keywordIndex + 1
    // Skip `<T>` type parameters after `fun` or `val`.
    if (tokens.getOrNull(i)?.text == "<") {
        var angle = 0
        while (i < tokens.size) {
            if (tokens[i].text == "<") angle++
            if (tokens[i].text == ">") angle--
            i++
            if (angle == 0) break
        }
    }
    if (keyword in Declaration.TYPE_KEYWORDS) {
        val name = tokens.getOrNull(i)?.takeIf { it.isIdentifier } ?: return null
        return Declaration(name.text, null, keyword, visibility, false)
    }
    // The name is the last identifier before `(`, `:`, or `=`, and anything before it is the receiver.
    val start = i
    var angle = 0
    var nesting = 0
    var end = i
    while (end < tokens.size) {
        val text = tokens[end].text
        if (tokens[end].kind == Token.Kind.SYMBOL) {
            if (text == "<") angle++
            if (text == ">") angle--
            if (angle == 0 && nesting == 0 && (text == ":" || text == "=" || text == "{")) break
            if (angle == 0 && nesting == 0 && text == "(" && !isFunctionTypeReceiver(tokens, end)) break
            if (text == "(") nesting++
            if (text == ")") nesting--
        }
        end++
    }
    if (end == start) return null
    val nameToken = tokens[end - 1]
    if (!nameToken.isIdentifier) return null
    val receiver = if (end - start >= 3) tokens.subList(start, end - 2).joinToString("") { it.text } else null
    return Declaration(nameToken.text, receiver, keyword, visibility, isOperator)
}

/** True for the `(` of `fun ((Int) -> Int).name()`, which only opens a function-type receiver right after the keyword. */
private fun isFunctionTypeReceiver(tokens: List<Token>, index: Int): Boolean =
    tokens.getOrNull(index - 1)?.text.let { it == "fun" || it == ">" && index >= 2 && tokens[index - 2].text == "<" }
