package kokonut.core

import java.util.*

object Identity {

    const val majorIndex = 0
    const val ticker : String = "KNT"

    private val properties: Properties = Properties().apply {
        Identity.javaClass.classLoader.getResourceAsStream("version.properties")?.use { load(it) }
    }
    val protocolVersion: Int
    get() = properties.getProperty("version", "1.0.0").split(".")[majorIndex].toInt()

    val libraryVersion : String
    get() = properties.getProperty("version", "1.0.0")

}
