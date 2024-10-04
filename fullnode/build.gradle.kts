plugins {
    kotlin("jvm") version "2.0.0"
    id("io.ktor.plugin") version "2.3.4"
    kotlin("plugin.serialization") version "1.8.20"
    application
}

version = "1.0.2"

repositories {
    mavenCentral()
    maven{
        url = uri("https://jitpack.io")
    }
}

dependencies {
    testImplementation(kotlin("test"))

    //Disable kokonut implementation when develop mode :)
    implementation("com.github.Pascal-Institute:kokonut:c1e3e9d209")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("io.ktor:ktor-server-html-builder:2.3.4")
    implementation("io.ktor:ktor-server-core-jvm:2.3.4")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.4")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.4")
    implementation("ch.qos.logback:logback-classic:1.4.12")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.8.0")
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

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("server.ApplicationKt")
}