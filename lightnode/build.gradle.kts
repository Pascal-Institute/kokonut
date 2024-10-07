plugins {
    kotlin("jvm") version "2.0.0"
}

group = "io.github.pascal-institute"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}