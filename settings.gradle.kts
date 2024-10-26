plugins {
    kotlin("jvm") version "1.9.22" apply false
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "kokonut"
include(":lightnode")
