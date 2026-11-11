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

        checkUserImports(main)
        val included = if (main.imports.any { isLibraryImport(it) }) findIncluded(main, library) else emptySet()
        val ordered = included.sortedBy { it.name }
        checkConflicts(main, ordered)

        val warnings = (userShadowingWarnings(main, ordered) + internalUseWarnings(main, library)).toMutableList()
        val source = render(main, ordered, options)
        val bytes = source.toByteArray().size
        if (bytes > options.sizeWarningBytes) {
            warnings += "The output is $bytes bytes, above the ${options.sizeWarningBytes}-byte warning threshold."
        }
        return BundleResult(source, ordered.map { it.name }, warnings)
    }

    /** Standard array types that the library extends. Their names appear in what makes them: `IntArray(n)`, `intArrayOf`, `toIntArray`. */
    val STANDARD_ARRAYS = listOf("Int", "Long", "Double", "Char", "Boolean", "Byte", "Short", "Float").map { it + "Array" }

    /**
     * Follows names to a fixed point. Names alone would pull in every element type, because `sortBy` and
     * others exist once per type. So an extension on a library type counts only once that type is included:
     * code that holds a value of the type has to reach the type's file through some name.
     *
     * An extension on a standard array, such as `IntArray.lowerBound`, counts only once the user's code can hold that
     * array: once its type name appears, ignoring case, in the user's code (`IntArray(n)`, `intArrayOf`, `toIntArray`),
     * or in the declared return type of a library function that the user's code names (`argsort`, `prefixSum`).
     * Its name, too, must appear in the user's code: the List shares the names `lowerBound` and `sortBy`, so counting
     * the library's names would bring array extensions to every List that sits next to an `IntArray`. This holds
     * because no library file calls an extension on a standard array that another file declares (`toIntList` too),
     * which the library's SourceRulesTest checks in the compiled classes. The library's own arrays do not count either: the List keeps its elements in an `IntArray`
     * and its sort keys in a `LongArray`. SourceRulesTest also checks that every public function that returns an
     * array declares it.
     * An array made with none of these, as by
     * `IntStream.toArray()`, is a compile error that names the missing function, never a wrong answer.
     */
    private fun findIncluded(main: SourceFile, library: List<SourceFile>): Set<SourceFile> {
        val typeOwners = HashMap<String, SourceFile>()
        for (file in library) {
            for (declaration in file.declarations) if (declaration.isType) typeOwners[declaration.name] = file
        }
        val included = LinkedHashSet<SourceFile>()
        val arraysSeen = HashSet<String>()
        fun noteArrays(names: Collection<String>) {
            for (array in STANDARD_ARRAYS) if (array !in arraysSeen && names.any { it.contains(array, ignoreCase = true) }) arraysSeen += array
        }
        val mentioned = HashSet(main.identifiers)
        fun isNeeded(declaration: Declaration): Boolean {
            val receiver = declaration.receiver?.substringBefore('<')?.removeSuffix("?") ?: return declaration.name in mentioned
            // Only the user's code calls array extensions, so the library's names do not count for them.
            if (receiver in STANDARD_ARRAYS) return declaration.name in main.identifiers && receiver in arraysSeen
            if (declaration.name !in mentioned) return false
            val owner = typeOwners[receiver] ?: return true
            return owner in included
        }
        noteArrays(mentioned)
        var changed = true
        while (changed) {
            changed = false
            for (file in library) {
                if (file !in included && file.declarations.any(::isNeeded)) {
                    included += file
                    // Every name counts, members' too: a member may hand its work to a same-named extension in
                    // another file. A missing file is a compile error in a contest, an extra one only costs bytes.
                    mentioned += file.identifiers
                    for ((name, returnType) in file.publicReturnTypes) if (name in main.identifiers) noteArrays(returnType)
                    changed = true
                }
            }
        }
        return included
    }

    fun isLibraryImport(path: String) = path == "$LIBRARY_PACKAGE.*" || path.startsWith("$LIBRARY_PACKAGE.")

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

    /**
     * Pasted code can reach `internal` declarations that `import` cannot, so it would break at install time.
     * Only a warning: the scan cannot tell these from a user's local variable, and pasted code compiles either way.
     */
    private fun internalUseWarnings(main: SourceFile, library: List<SourceFile>): List<String> {
        if (main.imports.none(::isLibraryImport)) return emptyList()
        val userDeclared = main.declarations.mapTo(HashSet()) { it.name }
        val internalNames = library.flatMap { file ->
            file.declarations.filter { it.visibility != Declaration.Visibility.PUBLIC }.map { it.name }
        }
        val used = internalNames.filter { it in main.identifiers && it !in userDeclared }.distinct().sorted()
        if (used.isEmpty()) return emptyList()
        return listOf("These names are internal to the library: ${used.joinToString()}. This is fine for your own local variables. Calling the library's would compile when pasted, but not with import.")
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
