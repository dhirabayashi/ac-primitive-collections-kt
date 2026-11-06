plugins {
    kotlin("jvm")
    application
}
application { mainClass = "bundler.MainKt" }
dependencies { testImplementation(kotlin("test")) }
