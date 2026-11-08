import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The generator runs inside Gradle, so it uses Gradle's embedded Kotlin rather than the judge's.
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions.jvmTarget = JvmTarget.JVM_17
}

gradlePlugin {
    plugins {
        create("codegen") {
            id = "ac.codegen"
            implementationClass = "codegen.CodegenPlugin"
        }
        create("mutationGate") {
            id = "ac.mutation-gate"
            implementationClass = "mutation.MutationGatePlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform()
    // CodegenTest reads the real templates, so a template change must rerun it.
    inputs.dir("../templates")
    inputs.dir("../test-templates")
    // MutationGateTest reads the real review table and the committed generated files.
    inputs.file("../docs/mutation-review.md")
    inputs.dir("../generated/main/kotlin")
}
