// SPDX-License-Identifier: CC0-1.0
package codegen

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class CodegenTask : DefaultTask() {
    @get:InputDirectory
    abstract val templates: DirectoryProperty

    @get:InputDirectory
    abstract val testTemplates: DirectoryProperty

    @get:Internal
    abstract val output: DirectoryProperty

    protected fun expected(): Map<String, String> {
        val main = Codegen.generate(templates.get().asFile).mapKeys { "main/kotlin/${it.key}" }
        val test = Codegen.generate(testTemplates.get().asFile).mapKeys { "test/kotlin/${it.key}" }
        return main + test
    }
}

/** Rewrites `generated/`, removing files that no template produces. */
abstract class GenerateSourcesTask : CodegenTask() {
    @TaskAction
    fun run() {
        val root = output.get().asFile
        val files = expected()
        root.walkBottomUp().filter { it.isFile }.forEach { file ->
            if (file.relativeTo(root).invariantSeparatorsPath !in files) file.delete()
        }
        for ((path, content) in files) {
            val file = root.resolve(path)
            file.parentFile.mkdirs()
            file.writeText(content)
        }
    }
}

/** Fails when the committed `generated/` differs from what the templates produce. */
abstract class CheckGeneratedTask : CodegenTask() {
    @TaskAction
    fun run() {
        val root = output.get().asFile
        val files = expected()
        val actual = root.walkTopDown().filter { it.isFile }.map { it.relativeTo(root).invariantSeparatorsPath }.toSet()
        val problems = buildList {
            (files.keys - actual).forEach { add("missing: $it") }
            (actual - files.keys).forEach { add("not produced by a template: $it") }
            (files.keys intersect actual).forEach { path ->
                if (root.resolve(path).readText() != files[path]) add("out of date: $path")
            }
        }
        if (problems.isNotEmpty()) {
            throw GradleException(
                "generated/ does not match the templates. Run ./gradlew generateSources.\n" +
                    problems.sorted().joinToString("\n"),
            )
        }
    }
}

class CodegenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        fun CodegenTask.configure() {
            group = "codegen"
            templates.set(project.layout.projectDirectory.dir("templates"))
            testTemplates.set(project.layout.projectDirectory.dir("test-templates"))
            output.set(project.layout.projectDirectory.dir("generated"))
        }
        project.tasks.register("generateSources", GenerateSourcesTask::class.java) { configure() }
        project.tasks.register("checkGenerated", CheckGeneratedTask::class.java) { configure() }
    }
}
