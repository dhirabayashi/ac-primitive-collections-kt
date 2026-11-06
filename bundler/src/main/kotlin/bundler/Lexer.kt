// SPDX-License-Identifier: CC0-1.0
package bundler

/** A token of Kotlin source. Comments and string contents are dropped, except for template expressions. */
data class Token(val text: String, val kind: Kind, val start: Int, val end: Int) {
    enum class Kind { IDENTIFIER, SYMBOL, LITERAL }

    val isIdentifier get() = kind == Kind.IDENTIFIER
}

/** The result of lexing: the tokens and the comment spans, so a caller can strip comments. */
class LexedSource(val tokens: List<Token>, val comments: List<IntRange>)

/**
 * A small Kotlin lexer. The bundler only needs identifiers and bracket structure, and it must
 * see identifiers inside string templates such as `"${a.subList(0, 1)}"`.
 */
fun lex(source: String): LexedSource {
    val tokens = ArrayList<Token>()
    val comments = ArrayList<IntRange>()
    var i = 0
    val n = source.length

    fun isIdentifierStart(c: Char) = c == '_' || c.isLetter()
    fun isIdentifierPart(c: Char) = c == '_' || c.isLetterOrDigit()

    // Each element is the brace depth at which a `${` template opened, plus whether its string is raw.
    class Template(val raw: Boolean, var depth: Int)
    val templates = ArrayList<Template>()

    /** Reads string content from [i] until the closing quote or a `${`. */
    fun readStringBody(raw: Boolean) {
        while (i < n) {
            val c = source[i]
            when {
                !raw && c == '\\' -> i += 2
                raw && source.startsWith("\"\"\"", i) -> {
                    i += 3
                    while (i < n && source[i] == '"') i++
                    return
                }
                !raw && c == '"' -> {
                    i++
                    return
                }
                c == '$' && i + 1 < n && source[i + 1] == '{' -> {
                    tokens += Token("\${", Token.Kind.LITERAL, i, i + 2)
                    i += 2
                    templates += Template(raw, 0)
                    return
                }
                c == '$' && i + 1 < n && isIdentifierStart(source[i + 1]) -> {
                    val start = ++i
                    while (i < n && isIdentifierPart(source[i])) i++
                    tokens += Token(source.substring(start, i), Token.Kind.IDENTIFIER, start, i)
                }
                else -> i++
            }
        }
    }

    while (i < n) {
        val c = source[i]
        when {
            c.isWhitespace() -> i++
            source.startsWith("//", i) -> {
                val start = i
                while (i < n && source[i] != '\n') i++
                comments += start until i
            }
            source.startsWith("/*", i) -> {
                // Kotlin block comments nest.
                val start = i
                var depth = 0
                while (i < n) {
                    if (source.startsWith("/*", i)) {
                        depth++
                        i += 2
                    } else if (source.startsWith("*/", i)) {
                        depth--
                        i += 2
                        if (depth == 0) break
                    } else {
                        i++
                    }
                }
                comments += start until i
            }
            source.startsWith("\"\"\"", i) -> {
                tokens += Token("\"\"\"", Token.Kind.LITERAL, i, i + 3)
                i += 3
                readStringBody(raw = true)
            }
            c == '"' -> {
                tokens += Token("\"", Token.Kind.LITERAL, i, i + 1)
                i++
                readStringBody(raw = false)
            }
            c == '\'' -> {
                val start = i
                i++
                while (i < n && source[i] != '\'') i += if (source[i] == '\\') 2 else 1
                i++
                tokens += Token(source.substring(start, minOf(i, n)), Token.Kind.LITERAL, start, minOf(i, n))
            }
            c == '`' -> {
                val start = ++i
                while (i < n && source[i] != '`') i++
                tokens += Token(source.substring(start, i), Token.Kind.IDENTIFIER, start, i)
                i++
            }
            isIdentifierStart(c) -> {
                val start = i
                while (i < n && isIdentifierPart(source[i])) i++
                tokens += Token(source.substring(start, i), Token.Kind.IDENTIFIER, start, i)
            }
            c.isDigit() -> {
                val start = i
                while (i < n && (isIdentifierPart(source[i]) || (source[i] == '.' && i + 1 < n && source[i + 1].isDigit()))) i++
                tokens += Token(source.substring(start, i), Token.Kind.LITERAL, start, i)
            }
            c == '{' && templates.isNotEmpty() -> {
                templates.last().depth++
                tokens += Token("{", Token.Kind.SYMBOL, i, i + 1)
                i++
            }
            c == '}' && templates.isNotEmpty() -> {
                val template = templates.last()
                if (template.depth == 0) {
                    templates.removeLast()
                    i++
                    readStringBody(template.raw)
                } else {
                    template.depth--
                    tokens += Token("}", Token.Kind.SYMBOL, i, i + 1)
                    i++
                }
            }
            source.startsWith("::", i) -> {
                tokens += Token("::", Token.Kind.SYMBOL, i, i + 2)
                i += 2
            }
            else -> {
                tokens += Token(c.toString(), Token.Kind.SYMBOL, i, i + 1)
                i++
            }
        }
    }
    return LexedSource(tokens, comments)
}
