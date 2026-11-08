// SPDX-License-Identifier: CC0-1.0
package mutation

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

/** Compares PIT's report with the reviewed survivors in `docs/mutation-review.md`. See docs/PLAN.md, section 7.1. */
abstract class MutationGateTask : DefaultTask() {
    @get:InputFile
    abstract val report: RegularFileProperty

    @get:InputFile
    abstract val review: RegularFileProperty

    @get:InputDirectory
    abstract val generated: DirectoryProperty

    @TaskAction
    fun run() {
        val result = MutationGate.check(
            MutationReport.parse(report.get().asFile),
            TemplateMap.read(generated.get().asFile),
            ReviewTable.parse(review.get().asFile.readText()),
        )
        // The score is for reference only: equivalent survivors repeat in every generated class.
        logger.lifecycle("Mutations detected: ${result.detected} of ${result.total} (%.2f%%)".format(100.0 * result.detected / result.total))
        result.warnings.forEach { logger.warn(it) }
        if (result.failures.isNotEmpty()) {
            throw GradleException(
                "Undetected mutations that docs/mutation-review.md does not allow. Add a test, or review them and list them there.\n" +
                    result.failures.joinToString("\n"),
            )
        }
    }
}

class MutationGatePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val gate = project.tasks.register("pitestBaseline", MutationGateTask::class.java) {
            group = "verification"
            report.set(project.layout.buildDirectory.file("reports/pitest/mutations.xml"))
            review.set(project.layout.projectDirectory.file("docs/mutation-review.md"))
            generated.set(project.layout.projectDirectory.dir("generated/main/kotlin/ac_primitive_collections"))
            dependsOn("pitest")
        }
        project.plugins.withId("info.solidsoft.pitest") {
            project.tasks.named("pitest").configure { finalizedBy(gate) }
        }
    }
}
