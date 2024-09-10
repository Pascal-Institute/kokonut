package kokonut.core

import kokonut.util.Utility.Companion.findKokonutJar
import java.io.File
import java.io.InputStream
import java.util.*
import java.util.jar.Manifest

object Identity {

    const val majorIndex = 0
    const val ticker : String = "KNT"

    var isRegistered = false
    val name : String = this::class.java.classLoader.name
    val isRelease = if(name.contains("kokonut")) true else false

    private val manifest: Manifest by lazy {
        val manifestStream: InputStream? = when(isRelease){
            true ->{
                this::class.java.classLoader.getResourceAsStream("META-INF/MANIFEST.MF")
            }
            false->{
                val jarFile = File(findKokonutJar())
                if (jarFile.exists()) {
                    val jar = java.util.jar.JarFile(jarFile)
                    jar.getInputStream(jar.getEntry("META-INF/MANIFEST.MF"))
                } else {
                    throw IllegalStateException("Manifest file not found in debugging environment")
                }
            }
        }
        Manifest(manifestStream)
    }

    val libraryVersion : String
        get() = manifest.mainAttributes.getValue("Implementation-Version")

    val protocolVersion: Int
    get() = libraryVersion.split(".")[majorIndex].toInt()
}
