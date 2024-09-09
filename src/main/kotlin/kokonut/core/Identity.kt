package kokonut.core

import java.util.*

object Identity {

    const val majorIndex = 0
    const val ticker : String = "KNT"

    var isRegistered = false

    private val properties: Properties = Properties().apply {
        Thread.currentThread().contextClassLoader.getResourceAsStream("version.properties")?.use { load(it) }
    }

    val libraryVersion : String
        get() = properties.getProperty("version", "1.0.0")

    val protocolVersion: Int
    get() = libraryVersion.split(".")[majorIndex].toInt()
}
