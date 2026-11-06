plugins {
    kotlin("jvm")
    application
}
application { mainClass = "bytecodecheck.MainKt" }
dependencies {
    implementation("org.ow2.asm:asm:9.8")
    implementation("org.ow2.asm:asm-commons:9.8")
    implementation("org.jetbrains.kotlin:kotlin-metadata-jvm:${providers.gradleProperty("judgeKotlinVersion").get()}")
    testImplementation(kotlin("test"))
}
