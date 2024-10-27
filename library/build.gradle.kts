plugins {
    java
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    `maven-publish`
}

version = "4.1.2"

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
    implementation("io.ktor:ktor-server-netty-jvm:2.3.4")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.4")
    implementation("io.ktor:ktor-client-core:2.3.4")
    implementation("io.ktor:ktor-client-cio:2.3.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.4")
    implementation("ch.qos.logback:logback-classic:1.4.12")
    implementation("org.xerial:sqlite-jdbc:3.41.2.2")
}

tasks.withType<ProcessResources> {
    from("src/main/resources/0000000000000000000000000000000000000000000000009fece4dc7e1d108d.json") {
        into("resources")
    }
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