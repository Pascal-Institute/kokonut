package kokonut.core

import java.util.*

object Version {
    const val majorIndex = 0

    private val properties: Properties = Properties().apply {
        Version.javaClass.classLoader.getResourceAsStream("kokonut.properties")?.use { load(it) }
    }

    val libraryVersion : String
        get() = properties.getProperty("kokonut_version", "0.0.0")

    val protocolVersion: Int
        get() = libraryVersion.split(".")[majorIndex].toInt()

    val genesisBlockID : String
        get() = properties.getProperty("kokonut_genesis_block")
}
