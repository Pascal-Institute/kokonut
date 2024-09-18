plugins {
    java
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "1.8.20"
    `maven-publish`
}

version = "4.0.9"

repositories {
    mavenCentral()
    maven{
        url = uri("https://jitpack.io")
    }
}


dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.8.0")
    implementation("io.ktor:ktor-server-core-jvm:2.3.4")
    implementation("io.ktor:ktor-server-html-builder:2.3.4")
    implementation("io.ktor:ktor-client-core:2.3.4")
    implementation("io.ktor:ktor-client-cio:2.3.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.4")
    implementation("ch.qos.logback:logback-classic:1.4.12")
    implementation("org.xerial:sqlite-jdbc:3.41.2.2")
}

tasks.test {
    useJUnitPlatform()
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
            groupId = "io.github.pascal-institute" //Navigate beyond computing oceans
            artifactId = "kokonut"

            version = project.version.toString()

            from(components["java"])
        }
    }
}