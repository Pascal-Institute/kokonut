import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    //develop
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.1.0"
    //production
    //kotlin("jvm") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

group = "io.github.pascal-institute"
version = "1.0-SNAPSHOT"

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(project(":library"))
    implementation(compose.desktop.currentOs)
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.2")
    implementation("io.ktor:ktor-client-cio:3.0.2")
    implementation("io.ktor:ktor-client-websockets:3.0.2")
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "ApplicationKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "lightnode"
            packageVersion = "1.0.0"
        }
    }
}