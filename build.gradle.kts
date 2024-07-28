plugins {
    kotlin("jvm") version "2.0.0"
    id("io.ktor.plugin") version "2.3.4"
    kotlin("plugin.serialization") version "1.8.20"
    `maven-publish`
    application
}

repositories {
    mavenCentral()
    maven{
        url = uri("https://jitpack.io")
    }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.ktor:ktor-client-core:2.3.4")
    implementation("io.ktor:ktor-client-cio:2.3.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.4")
    implementation("ch.qos.logback:logback-classic:1.4.12")
}


application {
    mainClass.set("kokonut.Main")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.pascal.institute" //Navigate beyond computing oceans
            artifactId = "kokonut"

            version = "1.0.0"

            from(components["java"])
        }
    }
}