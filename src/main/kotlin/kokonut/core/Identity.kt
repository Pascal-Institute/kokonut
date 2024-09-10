package kokonut.core

import java.io.InputStream
import java.util.*
import java.util.jar.Manifest

object Identity {

    const val majorIndex = 0
    const val ticker : String = "KNT"
    val isDebugMode: Boolean = System.getProperty("debug.mode") == "true"
    //For only full node
    var isRegistered = false

    private val properties: Properties = Properties().apply {
        Identity.javaClass.classLoader.getResourceAsStream("version.properties")?.use { load(it) }
    }

    private val manifest: Manifest by lazy {
        val inputStream: InputStream = Thread.currentThread().contextClassLoader.getResourceAsStream("META-INF/MANIFEST.MF")
            ?: throw IllegalStateException("Manifest file not found")
        Manifest(inputStream)
    }

    val libraryVersion : String
        get() = if(isDebugMode) manifest.mainAttributes.getValue("Implementation-Version") else properties.getProperty("version", "1.0.0")

    val protocolVersion: Int
    get() = libraryVersion.split(".")[majorIndex].toInt()
}
