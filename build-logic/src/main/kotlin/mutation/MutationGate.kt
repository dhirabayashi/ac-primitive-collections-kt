// SPDX-License-Identifier: CC0-1.0
package mutation

import codegen.Codegen
import codegen.TYPE_SPECS
import codegen.VALUE_SPECS
import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** One mutation from PIT's `mutations.xml`. */
data class Mutation(
    val sourceFile: String,
    val method: String,
    val mutator: String,
    val description: String,
    val line: Int,
    val status: String,
    val detected: Boolean,
)

/** What a reviewed survivor is matched by. Line numbers are left out so that edits and the Char rows do not break it. */
data class MutationKey(val template: String, val method: String, val mutator: String, val description: String)

/**
 * Reads PIT's XML report. The status list is not used for the verdict: `detected` decides,
 * so a status PIT adds later cannot slip through.
 */
object MutationReport {
    fun parse(xml: File): List<Mutation> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        val document = factory.newDocumentBuilder().parse(xml)
        val nodes = document.getElementsByTagName("mutation")
        return (0 until nodes.length).map { index ->
            val element = nodes.item(index) as Element
            fun text(tag: String) = element.getElementsByTagName(tag).item(0).textContent
            Mutation(
                sourceFile = text("sourceFile"),
                method = text("mutatedMethod"),
                mutator = text("mutator").substringAfterLast('.'),
                description = text("description"),
                line = text("lineNumber").toInt(),
                status = element.getAttribute("status"),
                detected = element.getAttribute("detected") == "true",
            )
        }
    }
}

/** A generated file, the template it came from, and the types it was filled with. */
data class GeneratedFile(val template: String, val kType: String, val vType: String?)

/**
 * Turns a mutation into a [MutationKey]. Generated files name their template on the second line
 * (see [Codegen.header]), so this reads that instead of repeating the generator's naming rules.
 */
class TemplateMap(private val files: Map<String, GeneratedFile>) {
    private val templatesByClass = files.mapKeys { it.key.removeSuffix(".kt") }.mapValues { it.value.template.removeSuffix(".kt") }

    fun key(mutation: Mutation): MutationKey {
        val generated = files[mutation.sourceFile]
        // A hand-written file has methods that differ by type on purpose, so their names stay as they are.
        val method = generated?.let { normalize(mutation.method, it) } ?: mutation.method
        return MutationKey(generated?.template ?: mutation.sourceFile, method, mutation.mutator, normalizeClasses(mutation.description))
    }

    private fun normalize(name: String, file: GeneratedFile): String {
        var result = name.replace(typeWord(file.kType), "KType")
        if (file.vType != null && file.vType != file.kType) result = result.replace(typeWord(file.vType), "VType")
        return result
    }

    /** A description that names a generated class, such as `removed call to .../IntList::reverse`, gets the template's name instead. */
    private fun normalizeClasses(description: String) = CLASS_REFERENCE.replace(description) { match ->
        val name = match.groupValues[1]
        val outer = name.substringBefore('$')
        val inner = name.substring(outer.length)
        val fileClass = outer.removeSuffix(FILE_CLASS_SUFFIX)
        val template = templatesByClass[outer] ?: templatesByClass[fileClass]
        if (template == null) {
            match.value
        } else {
            val suffix = if (templatesByClass[outer] == null) FILE_CLASS_SUFFIX else ""
            "$PACKAGE/$template$suffix$inner::"
        }
    }

    companion object {
        private const val PACKAGE = "ac_primitive_collections"
        private const val FILE_CLASS_SUFFIX = "Kt"
        private const val HEADER_PREFIX = "${Codegen.GENERATED_FROM}templates/"
        private val CLASS_REFERENCE = Regex("""$PACKAGE/([A-Za-z0-9_$]+)::""")

        /** Every type the generator knows, so that a file it makes later still parses. Longer names first. */
        private val TYPE_NAMES = (TYPE_SPECS.keys + VALUE_SPECS.keys).sortedByDescending { it.length }
        private val TYPE_GROUP = TYPE_NAMES.joinToString("|", "(", ")")

        /** A type name not followed by a lowercase letter, so that `Int` does not match inside `Interval`. */
        private fun typeWord(type: String) = Regex("${Regex.escape(type)}(?![a-z])")

        /** Reads the second line of each file in [dir]. A file without the header is not generated and is skipped. */
        fun read(dir: File): TemplateMap {
            val files = dir.listFiles { file -> file.extension == "kt" }.orEmpty().mapNotNull { file ->
                val line = file.useLines { it.drop(1).firstOrNull() } ?: return@mapNotNull null
                if (!line.startsWith(HEADER_PREFIX)) return@mapNotNull null
                val template = line.removePrefix(HEADER_PREFIX).substringBefore(".kt") + ".kt"
                file.name to parse(file.name, template)
            }
            return TemplateMap(files.toMap())
        }

        /** Finds which types filled [template] to make [fileName]. A heap template also makes the Max direction. */
        fun parse(fileName: String, template: String): GeneratedFile {
            val pattern = template.removeSuffix(".kt").split(Regex("(?=KType|VType)|(?<=KType|VType)")).filter { it.isNotEmpty() }
                .joinToString("") { part ->
                    when (part) {
                        "KType", "VType" -> TYPE_GROUP
                        else -> part.split(Codegen.MIN_HEAP).joinToString("(?:${Codegen.MIN_HEAP}|${Codegen.MAX_HEAP})") { Regex.escape(it) }
                    }
                }
            val match = Regex("$pattern\\.kt").matchEntire(fileName)
                ?: throw IllegalArgumentException("$fileName does not match its template $template")
            val types = match.groupValues.drop(1)
            val parts = Regex("KType|VType").findAll(template).map { it.value }.toList()
            val byToken = parts.zip(types).toMap()
            return GeneratedFile(template, byToken.getValue("KType"), byToken["VType"])
        }
    }
}

/** A reviewed survivor: at most [limit] mutations with this key may go undetected in any one generated class. */
data class Allowance(val key: MutationKey, val limit: Int)

/**
 * Reads the reviewed survivors from the table under [HEADING] in `docs/mutation-review.md`.
 * The table is the only list, so that no survivor can be allowed without its reason next to it.
 */
object ReviewTable {
    const val HEADING = "## 確認済みの生存変異"
    private val COLUMNS = listOf("テンプレート", "メソッド", "ミューテータ", "説明", "上限")
    private val CODE = Regex("`([^`]+)`")

    fun parse(markdown: String): List<Allowance> {
        val lines = markdown.lines()
        val start = lines.indexOfFirst { it.trim() == HEADING }
        require(start >= 0) { "The heading \"$HEADING\" is missing" }
        val table = lines.drop(start + 1).dropWhile { !it.trimStart().startsWith("|") }.takeWhile { it.trimStart().startsWith("|") }
        require(table.size >= 2) { "The table under \"$HEADING\" is missing" }
        val header = cells(table[0])
        val index = COLUMNS.associateWith { column ->
            header.indexOf(column).also { require(it >= 0) { "The table under \"$HEADING\" has no column \"$column\"" } }
        }
        val allowances = table.drop(2).flatMap { row ->
            val cell = cells(row)
            fun codes(column: String) = CODE.findAll(cell[index.getValue(column)]).map { it.groupValues[1] }.toList()
            fun single(column: String) = codes(column).singleOrNull()
                ?: throw IllegalArgumentException("Column \"$column\" needs exactly one code span: $row")
            val limit = cell[index.getValue("上限")].trim().toIntOrNull()
                ?: throw IllegalArgumentException("Column \"上限\" needs a number: $row")
            require(limit > 0) { "Column \"上限\" must be positive: $row" }
            val methods = codes("メソッド")
            require(methods.isNotEmpty()) { "Column \"メソッド\" needs a code span: $row" }
            methods.map { Allowance(MutationKey(single("テンプレート"), it, single("ミューテータ"), single("説明")), limit) }
        }
        val duplicates = allowances.groupBy { it.key }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Listed more than once: ${duplicates.joinToString()}" }
        return allowances
    }

    private fun cells(row: String) = row.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
}

data class GateResult(val failures: List<String>, val warnings: List<String>, val detected: Int, val total: Int)

/** Fails on any undetected mutation that the review table does not allow, or on more of one than it allows. */
object MutationGate {
    fun check(mutations: List<Mutation>, templates: TemplateMap, allowances: List<Allowance>): GateResult {
        val limits = allowances.associate { it.key to it.limit }
        val undetected = mutations.filter { !it.detected }.groupBy { it.sourceFile to templates.key(it) }
        val failures = undetected.entries.sortedWith(compareBy({ it.key.first }, { it.value.first().line })).mapNotNull { (where, found) ->
            val (file, key) = where
            val limit = limits[key]
            val detail = found.joinToString { "${it.line} ${it.status}" }
            when {
                limit == null -> "$file: not in the review table: ${describe(key)} (lines $detail)"
                found.size > limit -> "$file: ${found.size} undetected, the review table allows $limit: ${describe(key)} (lines $detail)"
                else -> null
            }
        }
        val seen = undetected.keys.map { it.second }.toSet()
        val warnings = allowances.filter { it.key !in seen }.map { "Detected in every class, so it may be stale: ${describe(it.key)}" }
        return GateResult(failures, warnings, mutations.count { it.detected }, mutations.size)
    }

    private fun describe(key: MutationKey) = "${key.template} ${key.method} ${key.mutator} \"${key.description}\""
}
