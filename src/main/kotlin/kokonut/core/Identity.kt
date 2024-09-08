package kokonut.core

import java.util.*

object Identity {

    const val majorIndex = 0
    const val ticker : String = "KNT"

    //For only full node
    var isRegistered = false

    private val properties: Properties = Properties().apply {
        Identity.javaClass.classLoader.getResourceAsStream("version.properties")?.use { load(it) }
    }

    val libraryVersion : String
        get() = properties.getProperty("version", "1.0.0")

    val protocolVersion: Int
    get() = libraryVersion.split(".")[majorIndex].toInt()
}
