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

/**
 * Enforces docs/PLAN.md, section 9: contest code must come from a pushed tag, and the
 * submission must carry a link to it.
 */
object Publication {
    private val SOURCE_PATHS = arrayOf("generated/main", "src/main")

    /** Returns the header line, or throws if the library is not published and [allowUnpublished] is false. */
    fun referenceLine(git: Git, allowUnpublished: Boolean): String {
        val repository = httpsUrl(git.run("remote", "get-url", "origin"))
        val problem = findProblem(git)
        if (problem == null) {
            val tag = releaseTagAtHead(git)!!
            return "// Library source (created with generative AI): $repository/tree/$tag"
        }
        if (!allowUnpublished) {
            throw BundleException(
                "$problem\nThe AtCoder rules require code made with generative AI to be public before a contest. " +
                    "Publish a tag first, or pass -p for practice outside contests.",
            )
        }
        val commit = git.run("rev-parse", "--short", "HEAD") ?: "unknown"
        return "// Library source (created with generative AI): $repository (UNPUBLISHED local state at $commit, not for contests)"
    }

    private fun findProblem(git: Git): String? {
        val status = git.run("status", "--porcelain", "--", *SOURCE_PATHS) ?: return "Cannot read the git status."
        if (status.isNotEmpty()) return "The library sources have uncommitted changes:\n$status"
        val tag = releaseTagAtHead(git) ?: return "HEAD has no release tag (v*)."
        val head = git.run("rev-parse", "HEAD") ?: return "Cannot read HEAD."
        val remote = git.run("ls-remote", "--tags", "origin", "refs/tags/$tag", "refs/tags/$tag^{}")
            ?: return "Cannot reach origin to confirm that $tag is pushed."
        val commits = remote.lines().filter { it.isNotBlank() }.map { it.substringBefore('\t') }
        if (commits.isEmpty()) return "Tag $tag is not on origin. Push it first."
        // An annotated tag lists the tag object first and the commit in the ^{} line.
        if (commits.last() != head) return "Tag $tag on origin points somewhere other than HEAD."
        return null
    }

    private fun releaseTagAtHead(git: Git): String? =
        git.run("tag", "--points-at", "HEAD")?.lines()?.filter { it.startsWith("v") }?.maxOrNull()

    /** Accepts `git@github.com:owner/repo.git` and `https://github.com/owner/repo.git`. */
    fun httpsUrl(remote: String?): String {
        val url = remote ?: throw BundleException("The library repository has no origin remote, so there is no URL to attach.")
        val ssh = Regex("""^git@([^:]+):(.+?)(\.git)?$""").find(url)
        if (ssh != null) return "https://${ssh.groupValues[1]}/${ssh.groupValues[2]}"
        return url.removeSuffix(".git")
    }
}
