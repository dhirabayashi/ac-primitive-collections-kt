// SPDX-License-Identifier: CC0-1.0
package bundler

/** Something the user must fix. The message says what, since this may be read during a contest. */
class BundleException(message: String) : Exception(message)

data class BundleOptions(
    /** The header line that points at the published source. */
    val referenceLine: String,
    val stripComments: Boolean = false,
    /** Warn above this. AtCoder's limit is unconfirmed. See docs/PLAN.md, section 15. */
    val sizeWarningBytes: Int = 512 * 1024,
)

data class BundleResult(val source: String, val includedFiles: List<String>, val warnings: List<String>)

object Bundler {
    const val LIBRARY_PACKAGE = "ac_primitive_collections"

    /** Library rules that keep the token-based dependency scan and pasting sound. */
    fun checkLibrary(library: List<SourceFile>): List<String> = buildList {
        for (file in library) {
            if (file.imports.any { it.endsWith(".*") }) add("${file.name}: star imports change how pasted user code resolves names")
            for (declaration in file.declarations) {
                if (declaration.isOperator && declaration.receiver != null) {
                    // `a + b` does not mention a name, so the scan could not see the dependency.
                    add("${file.name}: top-level operator ${declaration.receiver}.${declaration.name} must be a member")
                }
            }
        }
        val privateNames = library.flatMap { file ->
            file.declarations.filter { it.visibility == Declaration.Visibility.PRIVATE }.map { (it.receiver to it.name) to file.name }
        }
        for ((key, files) in privateNames.groupBy({ it.first }, { it.second })) {
            if (files.distinct().size > 1) add("private ${key.second} is declared in several files, so pasting them together fails: $files")
        }
    }

    fun bundle(main: SourceFile, library: List<SourceFile>, options: BundleOptions): BundleResult {
        checkLibrary(library).takeIf { it.isNotEmpty() }?.let { throw BundleException(it.joinToString("\n")) }

        val owners = HashMap<String, MutableList<SourceFile>>()
        for (file in library) {
            for (declaration in file.declarations) owners.getOrPut(declaration.name) { ArrayList() } += file
        }
        checkUserImports(main)
        val usesLibrary = main.imports.any { isLibraryImport(it) }
        val included = LinkedHashSet<SourceFile>()
        if (usesLibrary) {
            val pending = ArrayDeque<SourceFile>()
            fun visit(from: SourceFile) {
                // Only the library's own files have members worth skipping: user code may call the extension.
                val members = if (from === main) emptySet() else from.memberFunctions
                for (identifier in from.identifiers) {
                    if (identifier in members) continue
                    for (owner in owners[identifier].orEmpty()) {
                        if (owner !== from && included.add(owner)) pending += owner
                    }
                }
            }
            visit(main)
            while (pending.isNotEmpty()) visit(pending.removeFirst())
        }
        val ordered = included.sortedBy { it.name }
        checkConflicts(main, ordered)
        checkInternals(main, library)

        val warnings = userShadowingWarnings(main, ordered).toMutableList()
        val source = render(main, ordered, options)
        val bytes = source.toByteArray().size
        if (bytes > options.sizeWarningBytes) {
            warnings += "The output is $bytes bytes, above the ${options.sizeWarningBytes}-byte warning threshold."
        }
        return BundleResult(source, ordered.map { it.name }, warnings)
    }

    private fun isLibraryImport(path: String) = path == "$LIBRARY_PACKAGE.*" || path.startsWith("$LIBRARY_PACKAGE.")

    private fun checkUserImports(main: SourceFile) {
        for (path in main.imports.filter(::isLibraryImport)) {
            if (" as " in path) throw BundleException("Import alias is not supported, because pasted code has no import to rename: import $path")
        }
    }

    private fun checkConflicts(main: SourceFile, included: List<SourceFile>) {
        val problems = main.declarations.mapNotNull { user ->
            included.firstNotNullOfOrNull { file ->
                file.declarations.firstOrNull { it.name == user.name && it.receiver == user.receiver }?.let { file.name to it }
            }?.let { (fileName, _) ->
                val label = listOfNotNull(user.receiver, user.name).joinToString(".")
                "Your ${user.keyword} $label has the same name as one in $fileName. It compiles with import but not when pasted, so rename yours."
            }
        }
        if (problems.isNotEmpty()) throw BundleException(problems.joinToString("\n"))
    }

    /** Pasted code can reach `internal` declarations that `import` cannot, so it would break at install time. */
    private fun checkInternals(main: SourceFile, library: List<SourceFile>) {
        val userDeclared = main.declarations.mapTo(HashSet()) { it.name }
        val internalNames = library.flatMap { file ->
            file.declarations.filter { it.visibility != Declaration.Visibility.PUBLIC }.map { it.name }
        }
        val used = internalNames.filter { it in main.identifiers && it !in userDeclared }.distinct().sorted()
        if (used.isNotEmpty()) {
            throw BundleException("These are internal to the library, so the code would not compile with import: ${used.joinToString()}")
        }
    }

    /** Library code pasted next to user code resolves names against the user's declarations too. */
    private fun userShadowingWarnings(main: SourceFile, included: List<SourceFile>): List<String> {
        val libraryIdentifiers = included.flatMapTo(HashSet()) { it.identifiers }
        val libraryDeclared = included.flatMapTo(HashSet()) { file -> file.declarations.map { it.name } }
        return main.declarations
            .filter { it.name in libraryIdentifiers && it.name !in libraryDeclared && (it.isType || it.keyword == "fun") && it.receiver == null }
            .map { "Your ${it.keyword} ${it.name} shares a name with something the pasted library code uses. Check that it does not change the library's behavior, or rename it." }
    }

    private fun render(main: SourceFile, included: List<SourceFile>, options: BundleOptions): String {
        val imports = main.imports.filterNot(::isLibraryImport) + included.flatMap { it.imports }
        val distinctImports = imports.distinct()
        // Two different classes with one simple name cannot both be imported into one file.
        val bySimpleName = distinctImports.filterNot { it.endsWith(".*") }.groupBy { it.substringAfterLast(' ').substringAfterLast('.') }
        val clashes = bySimpleName.filter { it.value.size > 1 }
        if (clashes.isNotEmpty()) {
            throw BundleException("These imports clash when pasted into one file: ${clashes.values.flatten().joinToString()}")
        }
        val annotations = (main.fileAnnotations + included.flatMap { it.fileAnnotations }).distinct()
        fun bodyOf(file: SourceFile) = if (options.stripComments) file.bodyWithoutComments() else file.body
        return buildString {
            annotations.forEach { append(it).append('\n') }
            if (annotations.isNotEmpty()) append('\n')
            // REUSE-IgnoreStart
            append("// SPDX-License-Identifier: CC0-1.0\n")
            // REUSE-IgnoreEnd
            append(options.referenceLine).append('\n')
            append('\n')
            distinctImports.forEach { append("import ").append(it).append('\n') }
            if (distinctImports.isNotEmpty()) append('\n')
            append(bodyOf(main)).append('\n')
            for (file in included) {
                append("\n// ---- ac-primitive-collections-kt: ").append(file.name).append(" ----\n\n")
                append(bodyOf(file)).append('\n')
            }
        }
    }
}
