plugins {
    kotlin("jvm")
    id("io.ktor.plugin") version "2.3.4"
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("ApplicationKt")
}

repositories {
    mavenCentral()
    maven{
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(project(":library"))

    implementation("io.ktor:ktor-server-html-builder:2.3.4")
    implementation("io.ktor:ktor-server-core-jvm:2.3.4")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.4")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.4")
    implementation("ch.qos.logback:logback-classic:1.4.12")
}

tasks.named("build") {
    mustRunAfter(tasks.named("clean"))
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "server.ApplicationKt"
        )
    }
}