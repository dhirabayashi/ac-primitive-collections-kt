pluginManagement {
    includeBuild("build-logic")
    val judgeKotlinVersion: String by settings
    plugins {
        kotlin("jvm") version judgeKotlinVersion
        id("me.champeau.jmh") version "0.7.3"
        id("org.jetbrains.kotlinx.kover") version "0.9.1"
        id("info.solidsoft.pitest") version "1.15.0"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "ac-primitive-collections-kt"
include("bundler", "bytecode-check")
