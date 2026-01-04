import java.util.Properties

val propertiesFile = file("src/main/resources/kokonut.properties")

if (propertiesFile.exists()) {
    val properties = Properties()
    properties.load(propertiesFile.inputStream())
    val kokonutVersion = properties["kokonut_version"] as String?
    version = kokonutVersion ?: "0.0.0"
}

plugins {
    java
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    `maven-publish`
}

repositories {
    mavenCentral()
    maven{
        url = uri("https://jitpack.io")
    }
}


dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")
    implementation("io.ktor:ktor-server-core-jvm:3.0.2")
    implementation("io.ktor:ktor-server-html-builder:3.0.2")
    implementation("io.ktor:ktor-server-netty-jvm:3.0.2")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.2")
    implementation("io.ktor:ktor-server-websockets-jvm:3.0.2")
    implementation("io.ktor:ktor-client-core:3.0.2")
    implementation("io.ktor:ktor-client-cio:3.0.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.2")
    implementation("io.ktor:ktor-client-websockets:3.0.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.2")
    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
}

kotlin {
    jvmToolchain(17)
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Version" to project.version
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.pascal-institute"
            artifactId = "kokonut"

            version = project.version.toString()

            from(components["java"])
        }
    }
}