package kokonut.core

import java.util.*

object Identity {
    const val majorIndex = 0
    const val ticker : String = "KNT"

    //For Full Node
    var address = ""
    var isRegistered = false

    private val properties: Properties = Properties().apply {
        Identity.javaClass.classLoader.getResourceAsStream("kokonut.properties")?.use { load(it) }
    }

    val libraryVersion : String
        get() = properties.getProperty("kokonut_version", "0.0.0")

    val protocolVersion: Int
        get() = libraryVersion.split(".")[majorIndex].toInt()
}
