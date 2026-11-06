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
    }
}

tasks.test {
    useJUnitPlatform()
    // CodegenTest reads the real templates, so a template change must rerun it.
    inputs.dir("../templates")
    inputs.dir("../test-templates")
}
