package kokonut

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kokonut.core.Block
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.security.*
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow

class Utility {
    companion object {

        fun getReward(block: Block) : Double {

            val totalBlockOfYear = 365 * 144

            val x = (block.index).toDouble() / totalBlockOfYear

            val constant = 16.230619
            val exponent = -0.57721

            val expTerm = exp(exponent * x)

            val logTerm =  log2(2.0 + x)

            val value = (expTerm / logTerm) * constant

            return truncate(value)
        }

        private fun truncate(value: Double): Double {
            val scale = 10.0.pow(6)
            return floor(value * scale) / scale
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

        suspend fun isNodeHealthy(url: String): Boolean {
            val client = HttpClient(CIO) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 3000
                }
                expectSuccess = false
            }

            return try {
                val response: HttpResponse = client.get(url)
                println("Node is healthy : ${response.status}")
                response.status.isSuccess()
            } catch (e: Exception) {
                println(e.message)
                false
            } finally {
                client.close()
            }
        }

        suspend fun sendHttpGetRequest(urlString: String?) {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val `in` = BufferedReader(InputStreamReader(conn.inputStream))
                var inputLine: String?
                val response = StringBuffer()
                while ((`in`.readLine().also { inputLine = it }) != null) {
                    response.append(inputLine)
                }
                `in`.close()
                println("Response: $response")
            } else {
                println("GET request failed: $responseCode")
            }
        }

        fun sendHttpGetPolicy(urlString: String): Policy {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val `in` = BufferedReader(InputStreamReader(conn.inputStream))
                val response = StringBuffer()

                var inputLine: String?
                while (`in`.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }
                `in`.close()

                val html = response.toString()

                // Extract values using regular expressions
                val versionRegex = Regex("version : (\\d+)")
                val difficultyRegex = Regex("difficulty : (\\d+)")
                val rewardRegex = Regex("reward : (\\d+\\.\\d+)")

                val versionMatch = versionRegex.find(html)
                val difficultyMatch = difficultyRegex.find(html)
                val rewardMatch = rewardRegex.find(html)

                // Check if matches are found and extract values
                val version = versionMatch?.groups?.get(1)?.value?.toIntOrNull()
                    ?: throw IOException("Failed to parse the protocol version")
                val difficulty = difficultyMatch?.groups?.get(1)?.value?.toIntOrNull()
                    ?: throw IOException("Failed to parse the mining difficulty")
                val reward = rewardMatch?.groups?.get(1)?.value?.toDoubleOrNull()
                    ?: throw IOException("Failed to parse the mining reward")

                // Create Policy data class instance
                return Policy(version, difficulty, reward)
            } else {
                throw IOException("GET request failed with response code: $responseCode")
            }
        }


        fun sendHttpPostRequest(urlString: String, jsonElement: JsonElement, publicKeyFile: File) {
            val url = URL(urlString)
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setRequestProperty("Accept", "application/json")

            try {
                connection.outputStream.use { os ->
                    // Write JSON part
                    writePart(
                        os,
                        boundary,
                        "json",
                        "application/json; charset=UTF-8",
                        jsonElement.toString().toByteArray(Charsets.UTF_8)
                    )

                    // Write file part
                    writeFilePart(os, boundary, "public_key", "application/x-pem-file", publicKeyFile)

                    // End of multipart
                    os.write("--$boundary--\r\n".toByteArray(Charsets.UTF_8))
                }

                // Check response
                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { reader ->
                        val response = reader.readText()
                        println("Response: $response")
                    }
                } else {
                    println("Failed with HTTP error code: $responseCode")
                    connection.errorStream?.bufferedReader()?.use { reader ->
                        val errorResponse = reader.readText()
                        println("Error Response: $errorResponse")
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                connection.disconnect()
            }
        }

        private fun writePart(
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

        private fun writeFilePart(
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