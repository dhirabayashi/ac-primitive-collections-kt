// SPDX-License-Identifier: CC0-1.0
package bundler

import java.io.File

/** Runs git in the library's repository. Tests replace this. */
fun interface Git {
    /** Returns stdout, or null if git failed. */
    fun run(vararg args: String): String?
}

class ProcessGit(private val directory: File) : Git {
    override fun run(vararg args: String): String? {
        val process = ProcessBuilder("git", *args).directory(directory).redirectError(ProcessBuilder.Redirect.DISCARD).start()
        val output = process.inputStream.bufferedReader().readText()
        return if (process.waitFor() == 0) output.trim() else null
    }
}

/** A release tag on origin and the commit it points at there. */
data class Release(val tag: String, val commit: String)

/**
 * Enforces docs/PLAN.md, section 9: contest code must come from a pushed tag, and the
 * submission must carry a link to it. Contest code is read from the commit that origin
 * reports, so nothing in the working tree can reach it.
 */
object Publication {
    private val SOURCE_DIRECTORIES = arrayOf("generated/main/kotlin", "src/main/kotlin")
    private val RELEASE_TAG = Regex("""v\d{1,9}(\.\d{1,9})*""")

    /** Numeric, so v0.10.0 is newer than v0.9.0. Missing parts count as 0. */
    private val VERSION_ORDER = Comparator<Release> { a, b ->
        val x = versionNumbers(a.tag)
        val y = versionNumbers(b.tag)
        (0 until maxOf(x.size, y.size))
            .map { x.getOrElse(it) { 0 }.compareTo(y.getOrElse(it) { 0 }) }
            .firstOrNull { it != 0 } ?: 0
    }

    private fun versionNumbers(tag: String) = tag.removePrefix("v").split('.').map(String::toInt)

    fun latestRelease(git: Git): Release {
        val remote = git.run("ls-remote", "--tags", "origin")
            ?: throw BundleException("Cannot reach origin to find the latest pushed tag. Check the network and retry.")
        return parseReleases(remote).maxWithOrNull(VERSION_ORDER)
            ?: throw BundleException("origin has no release tag (v*). Push one first, or pass -p for practice outside contests.")
    }

    private fun parseReleases(lsRemote: String): List<Release> {
        val commits = LinkedHashMap<String, String>()
        for (line in lsRemote.lines().filter { it.isNotBlank() }) {
            // An annotated tag lists the tag object first and the commit in the ^{} line, so the last line wins.
            val tag = line.substringAfter('\t').removePrefix("refs/tags/").removeSuffix("^{}")
            if (RELEASE_TAG.matches(tag)) commits[tag] = line.substringBefore('\t')
        }
        return commits.map { (tag, commit) -> Release(tag, commit) }
    }

    fun loadRelease(git: Git, release: Release): List<SourceFile> {
        val missing = "The commit of ${release.tag} is not in the local repository. Run: git fetch --tags origin"
        val paths = git.run("ls-tree", "-r", "--name-only", release.commit, "--", *SOURCE_DIRECTORIES)
            ?: throw BundleException(missing)
        val files = paths.lines().filter { it.endsWith(".kt") }.map { path ->
            SourceFile(path.substringAfterLast('/'), git.run("show", "${release.commit}:$path") ?: throw BundleException(missing))
        }
        if (files.isEmpty()) throw BundleException("${release.tag} has no library sources.")
        return files.sortedBy { it.name }
    }

    fun referenceLine(git: Git, release: Release): String =
        "// Library source (created with generative AI): ${httpsUrl(git.run("remote", "get-url", "origin"))}/tree/${release.tag}"

    fun unpublishedReferenceLine(git: Git): String {
        val repository = httpsUrl(git.run("remote", "get-url", "origin"))
        val commit = git.run("rev-parse", "--short", "HEAD") ?: "unknown"
        return "// Library source (created with generative AI): $repository (UNPUBLISHED local state at $commit, not for contests)"
    }

    /**
     * Local runs use the working tree through includeBuild, but the submission uses the release, so
     * code that works locally can behave differently or fail to compile. These are warnings, because
     * the scan cannot tell a library name from a user's local variable and must not block a valid submission.
     */
    fun localDifferenceWarnings(main: SourceFile, included: List<String>, published: List<SourceFile>, local: List<SourceFile>, tag: String): List<String> {
        if (main.imports.none(Bundler::isLibraryImport)) return emptyList()
        val publishedTexts = published.associate { it.name to it.text.trim() }
        val localTexts = local.associate { it.name to it.text.trim() }
        val changed = included.filter { it in localTexts && localTexts[it] != publishedTexts[it] }
        val publishedNames = published.flatMapTo(HashSet()) { file -> file.declarations.map { it.name } }
        val userNames = main.declarations.mapTo(HashSet()) { it.name }
        val newer = local.flatMap { file -> file.declarations.map { it.name } }
            .filter { it in main.identifiers && it !in publishedNames && it !in userNames }
            .distinct().sorted()
        return buildList {
            if (changed.isNotEmpty()) add("The submission uses $tag, which differs from your local library in: ${changed.joinToString()}")
            if (newer.isNotEmpty()) add("Not in $tag, so this may not compile on AtCoder: ${newer.joinToString()}. Push a new tag, or avoid them.")
        }
    }

    /** Accepts `git@github.com:owner/repo.git` and `https://github.com/owner/repo.git`. */
    fun httpsUrl(remote: String?): String {
        val url = remote ?: throw BundleException("The library repository has no origin remote, so there is no URL to attach.")
        val ssh = Regex("""^git@([^:]+):(.+?)(\.git)?$""").find(url)
        if (ssh != null) return "https://${ssh.groupValues[1]}/${ssh.groupValues[2]}"
        return url.removeSuffix(".git")
    }
}
