package kokonut.util

import kokonut.util.API.Companion.isHealthy
import java.io.*
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.security.*
import java.util.*
import kotlin.math.*

class Utility {
    companion object {

        const val majorIndex = 0

        private val properties: Properties = Properties().apply {
            Utility::class.java.classLoader.getResourceAsStream("kokonut.properties")?.use { load(it) }
        }

        val libraryVersion : String
            get() = properties.getProperty("kokonut_version", "0.0.0")

        val protocolVersion: Int
            get() = libraryVersion.split(".")[majorIndex].toInt()

        val genesisBlockID : String
            get() = properties.getProperty("kokonut_genesis_block")

        fun checkHealth(fullNodes: MutableList<FullNode>) {
            val timer = Timer()
            timer.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    fullNodes.forEach { node ->
                        println("Checking health of node: ${node.address}")
                        if(!URL(node.address).isHealthy()){
                            fullNodes.remove(node)
                        }
                    }
                }
            }, 0, 300_000)
        }

        fun getJarDirectory(): File {
            val uri: URI = Utility::class.java.protectionDomain.codeSource.location.toURI()
            return File(Paths.get(uri).parent.toString())
        }

        fun setReward(index: Long) : Double {

            val intendedTotalBlockOfYear = 365 * 144

            val x = index.toDouble() / intendedTotalBlockOfYear

            val constant = 16.230619
            val exponent = -0.57721

            val expTerm = exp(exponent * x)

            val logTerm =  log2(2.0 + x)

            val value = truncate((expTerm / logTerm) * constant)

            return value
        }

        fun truncate(value: Double): Double {
            val scale = 10.0.pow(6)
            return floor(value * scale) / scale
        }

        fun findKokonutJar(): String {
            val buildDir = File("build/libs")

            val kokonutJar = buildDir.listFiles { _, name ->
                name.startsWith("kokonut-") && name.endsWith(".jar")
            }?.firstOrNull()  // 첫 번째 파일을 선택

            return kokonutJar!!.absolutePath
        }

        fun calculateHash(timestamp : Long): String {
            val input = "$timestamp"
            return MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray())
                .fold("") { str, it -> str + "%02x".format(it) }
        }

        fun calculateHash(publicKey: PublicKey): String {
            val input = "$publicKey"
            return MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray())
                .fold("") { str, it -> str + "%02x".format(it) }
        }

        fun createFile(filePath: String, content: String) {
            val file = File(filePath)
            file.writeText(content)
        }

        fun createDirectory(path: String) {
            val dir = File(path)
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }

        fun writePart(
            outputStream: OutputStream,
            boundary: String,
            name: String,
            contentType: String,
            content: ByteArray
        ) {
            outputStream.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
            outputStream.write("Content-Disposition: form-data; name=\"$name\"\r\n".toByteArray(Charsets.UTF_8))
            outputStream.write("Content-Type: $contentType\r\n\r\n".toByteArray(Charsets.UTF_8))
            outputStream.write(content)
            outputStream.write("\r\n".toByteArray(Charsets.UTF_8))
        }

        fun writeFilePart(
            outputStream: OutputStream,
            boundary: String,
            name: String,
            contentType: String,
            file: File
        ) {
            outputStream.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
            outputStream.write(
                "Content-Disposition: form-data; name=\"$name\"; filename=\"${file.name}\"\r\n".toByteArray(
                    Charsets.UTF_8
                )
            )
            outputStream.write("Content-Type: $contentType\r\n\r\n".toByteArray(Charsets.UTF_8))
            Files.copy(file.toPath(), outputStream)
            outputStream.write("\r\n".toByteArray(Charsets.UTF_8))
        }
    }
}