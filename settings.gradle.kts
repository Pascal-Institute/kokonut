plugins {
    kotlin("jvm") version "1.9.22" apply false
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "kokonut"

// VS Code/Gradle import works best when all modules are included.
// If you want a minimal build, you can comment out the node modules.
include(":library", ":fuelnode", ":fullnode", ":lightnode")