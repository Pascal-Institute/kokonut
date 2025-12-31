plugins {
    //develop
    //kotlin("jvm")
    //production
    kotlin("jvm") version "1.9.22"

    id("io.ktor.plugin") version "2.3.4"
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("ApplicationKt")
    applicationName = "knt_fullnode"
}

repositories {
    mavenCentral()
    maven{
        url = uri("https://jitpack.io")
    }
}

dependencies {
    //develop
    //implementation(project(":library"))
    //production
    implementation("com.github.Pascal-Institute:kokonut:4.1.7")

    implementation("io.ktor:ktor-server-html-builder:2.3.4")
    implementation("io.ktor:ktor-server-html-builder:2.3.4")
    implementation("io.ktor:ktor-server-core-jvm:2.3.4")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.4")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.4")
    implementation("ch.qos.logback:logback-classic:1.4.12")
    testImplementation(kotlin("test"))
}

tasks.named("build") {
    mustRunAfter(tasks.named("clean"))
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "ApplicationKt"
        )
    }
}