// The Kotlin and JDK versions match the current AtCoder judge. See docs/PLAN.md, section 2.1.
plugins {
    kotlin("jvm") version "2.2.10"
}

group = "ac_primitive_collections"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(23)
}

tasks.test {
    useJUnitPlatform()
}
