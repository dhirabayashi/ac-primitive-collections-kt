// The Kotlin and JDK versions come from gradle.properties and match the current AtCoder judge.
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm")
    id("ac.codegen")
    id("me.champeau.jmh")
    id("org.jetbrains.kotlinx.kover")
    id("info.solidsoft.pitest")
}

val judgeJdkFeature = providers.gradleProperty("judgeJdkFeature").get().toInt()
val judgeKotlinVersion = providers.gradleProperty("judgeKotlinVersion").get()

group = "ac_primitive_collections"
// A build at a release tag (v and dotted digits) carries its version; any other build is a snapshot.
// See the release procedure in docs/PLAN.md, Phase 1.
version = providers.exec {
    commandLine("git", "tag", "--points-at", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.get().lines().firstOrNull { Regex("""v\d+(\.\d+)*""").matches(it) }?.removePrefix("v") ?: "0.0.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(judgeJdkFeature)
        }
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        systemProperty("judge.kotlin", judgeKotlinVersion)
        systemProperty("judge.jdk", judgeJdkFeature)
        // The differential tests read these. A long run must not time out on the fixed-seed gates.
        System.getProperty("seed")?.let { systemProperty("seed", it) }
        System.getProperty("ops")?.let { systemProperty("ops", it) }
    }
}

sourceSets {
    main {
        kotlin.srcDir("generated/main/kotlin")
    }
    test {
        kotlin.srcDir("generated/test/kotlin")
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

// The templates are valid Kotlin for Int (docs/PLAN.md, section 11). Compiling them keeps that true for the IDE.
val templateCheck by sourceSets.creating {
    kotlin.srcDir("templates")
    kotlin.srcDir("src/main/kotlin")
}

tasks.check {
    dependsOn("checkGenerated", tasks.named(templateCheck.getCompileTaskName("kotlin")))
}

// ---- The release jar. See docs/PLAN.md, section 10.2. ----

tasks.jar {
    // The install script names the file this way too, so the version lives in the manifest instead.
    archiveFileName = "ac-primitive-collections.jar"
    manifest { attributes("Implementation-Version" to project.version) }
    from("LICENSE") { into("META-INF") }
    from("THIRD_PARTY_NOTICES") { into("META-INF") }
    doLast {
        logger.lifecycle("Jar ${project.version}: ${archiveFile.get().asFile.length()} bytes (budget 1.5 MB, section 7.6)")
    }
}

// ---- Coverage. See docs/PLAN.md, section 7.1. ----

kover {
    currentProject {
        sources { excludedSourceSets.add("templateCheck") }
    }
    reports {
        filters {
            includes { packages("ac_primitive_collections") }
        }
        verify {
            rule("Branch coverage") {
                bound {
                    minValue = 100
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH
                }
            }
        }
    }
}

// ---- Benchmarks. Numbers from here detect regressions; decisions use AtCoder's code test (section 7.6). ----

jmh {
    jvmArgs = listOf("-XX:+UseSerialGC", "-XX:ActiveProcessorCount=1", "-XX:MaxHeapSize=1000341504")
    profilers = listOf("gc")
    fork = 1
    warmupIterations = 3
    iterations = 3
    warmup = "1s"
    timeOnIteration = "1s"
    jmhVersion = "1.37"
}

dependencies {
    jmh("it.unimi.dsi:fastutil:8.5.15")
    jmh("com.carrotsearch:hppc:0.10.0")
}

// ---- Mutation testing. Nightly only. See docs/PLAN.md, section 7.1. ----

pitest {
    pitestVersion = "1.20.0"
    junit5PluginVersion = "1.2.2"
    targetClasses = setOf("ac_primitive_collections.*")
    // Allocation depends on the JIT, and the toolchain test does not touch the library.
    excludedTestClasses = setOf("ac_primitive_collections.AllocationTest", "ac_primitive_collections.JudgeToolchainTest", "ac_primitive_collections.SourceRulesTest")
    // Compiler-generated null checks.
    avoidCallsTo = setOf("kotlin.jvm.internal")
    // Inline bodies run from the caller's copy, so a mutation of the original cannot be observed.
    // The differential tests and the branch coverage gate cover them instead. See docs/mutation-review.md.
    excludedMethods = setOf(
        "all", "any", "count", "none", "filter", "map", "fold", "sumOf", "removeIf",
        "forEach", "forEachIndexed", "forEachReversed", "indexOfFirst", "indexOfLast",
        "firstOrElse", "lastOrElse", "sortBy", "sortByLong", "sortByDouble", "argsortBy", "argsortByLong", "argsortByDouble",
        "IntList", "LongList", "DoubleList", "CharList", "buildIntList", "buildLongList", "buildDoubleList", "buildCharList",
        "mapToInt", "mapToLong", "mapToDouble", "mapToChar", "forEachUnordered",
    )
    threads = Runtime.getRuntime().availableProcessors()
    outputFormats = setOf("HTML", "XML")
    timestampedReports = false
    mutationThreshold = 90
    jvmArgs = listOf("-Dops=2000")
}

// ---- Boxing and bytecode gates. See docs/PLAN.md, section 7.3. ----

val checkerClasspath by configurations.creating {
    isCanBeConsumed = false
}

val kotlincClasspath by configurations.creating {
    isCanBeConsumed = false
    isTransitive = true
}

dependencies {
    checkerClasspath(project(":bytecode-check"))
    kotlincClasspath("org.jetbrains.kotlin:kotlin-compiler-embeddable:$judgeKotlinVersion")
}

val checkLibraryBoxing by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Fails if a library class boxes outside the documented exceptions."
    classpath = checkerClasspath
    mainClass = "bytecodecheck.MainKt"
    val classes = layout.buildDirectory.dir("classes/kotlin/main")
    args("--library", "--class-prefix", "ac_primitive_collections/", classes.get().asFile.path)
    dependsOn(tasks.named("classes"))
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(judgeJdkFeature) }
}

val libraryJar = tasks.named<Jar>("jar")

/** Compiles each e2e/usage/<name>/Main.kt with the judge's compiler version, then checks MainKt. */
val checkUsageBoxing by tasks.registering {
    group = "verification"
    description = "Fails if user code boxes after the library's inline functions expand."
}

file("e2e/usage").listFiles { f -> f.isDirectory }.orEmpty().sortedBy { it.name }.forEach { usage ->
    val capitalized = usage.name.split('-').joinToString("") { part -> part.replaceFirstChar(Char::uppercase) }
    val out = layout.buildDirectory.file("usage/${usage.name}/Main.jar")
    val compile = tasks.register<JavaExec>("compileUsage$capitalized") {
        classpath = kotlincClasspath
        mainClass = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
        inputs.file(usage.resolve("Main.kt"))
        inputs.file(libraryJar.flatMap { it.archiveFile })
        outputs.file(out)
        dependsOn(libraryJar)
        javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(judgeJdkFeature) }
        doFirst {
            args(
                usage.resolve("Main.kt").path,
                "-no-stdlib", "-no-reflect",
                "-cp", libraryJar.get().archiveFile.get().asFile.path + File.pathSeparator + kotlincClasspath.files.first { it.name.startsWith("kotlin-stdlib-") && it.name.endsWith("$judgeKotlinVersion.jar") }.path,
                "-jvm-target", judgeJdkFeature.toString(),
                "-d", out.get().asFile.path,
            )
        }
    }
    val check = tasks.register<JavaExec>("checkUsageBoxing$capitalized") {
        classpath = checkerClasspath
        mainClass = "bytecodecheck.MainKt"
        dependsOn(compile)
        args("--strict", "--class-prefix", "MainKt", out.get().asFile.path)
        javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(judgeJdkFeature) }
    }
    checkUsageBoxing { dependsOn(check) }
}

tasks.check {
    dependsOn(checkLibraryBoxing, checkUsageBoxing)
}
