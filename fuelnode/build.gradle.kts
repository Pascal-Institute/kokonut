plugins {
    //develop
    //kotlin("jvm")
    //production
    kotlin("jvm") version "2.1.0"

    id("io.ktor.plugin") version "3.0.2"
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("ApplicationKt")
    applicationName = "knt_fuelnode"
}

repositories {
    mavenCentral()
    maven{
        url = uri("https://jitpack.io")
    }
}

dependencies {
    //develop
    implementation(project(":library"))
    //production
    //implementation("com.github.Pascal-Institute:kokonut:4.1.7")

    implementation("io.ktor:ktor-server-html-builder:3.0.2")
    implementation("io.ktor:ktor-server-core-jvm:3.0.2")
    implementation("io.ktor:ktor-server-netty-jvm:3.0.2")
    implementation("io.ktor:ktor-server-websockets-jvm:3.0.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.2")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.2")
    implementation("ch.qos.logback:logback-classic:1.5.12")
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