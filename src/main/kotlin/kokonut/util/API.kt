package kokonut.util

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kokonut.Policy
import kokonut.util.Utility.Companion.writeFilePart
import kokonut.util.Utility.Companion.writePart
import kotlinx.serialization.json.JsonElement
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class API {
    companion object {
        suspend fun URL.isNodeHealthy(): Boolean {
            val client = HttpClient(CIO) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 3000
                }
                expectSuccess = false
            }

            return try {
                val response: HttpResponse = client.get(this)
                println("Node is healthy : ${response.status}")
                response.status.isSuccess()
            } catch (e: Exception) {
                println(e.message)
                false
            } finally {
                client.close()
            }
        }

        fun URL.sendHttpGetReward(index: Long): Double? {
            val url = URL("${this}/getReward?index=$index")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"

            return try {
                // Check the response code
                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read the response
                    val inputStream = conn.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val response = reader.use { it.readText() }

                    // Convert the response to Double
                    response.toDoubleOrNull()
                } else {
                    // Handle non-OK response codes
                    println("GET request failed with response code $responseCode")
                    null
                }
            } catch (e: Exception) {
                // Handle exceptions
                e.printStackTrace()
                null
            } finally {
                conn.disconnect()
            }
        }

        fun URL.sendHttpGetPolicy(): Policy {
            val conn = this.openConnection() as HttpURLConnection
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

                val versionMatch = versionRegex.find(html)
                val difficultyMatch = difficultyRegex.find(html)

                // Check if matches are found and extract values
                val version = versionMatch?.groups?.get(1)?.value?.toIntOrNull()
                    ?: throw IOException("Failed to parse the protocol version")
                val difficulty = difficultyMatch?.groups?.get(1)?.value?.toIntOrNull()
                    ?: throw IOException("Failed to parse the mining difficulty")


                // Create Policy data class instance
                return Policy(version, difficulty)
            } else {
                throw IOException("GET request failed with response code: $responseCode")
            }
        }

        fun URL.sendHttpPostRequest(byteArray: ByteArray, publicKeyFile: File) {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val connection = this.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setRequestProperty("Accept", "application/json")

            try {
                connection.outputStream.use { os ->
                    // Write JSON part
                    Utility.writePart(
                        os,
                        boundary,
                        "json",
                        "application/json; charset=UTF-8",
                        byteArray
                    )

                    // Write file part
                    Utility.writeFilePart(os, boundary, "public_key", "application/x-pem-file", publicKeyFile)

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


        fun URL.sendHttpPostRequest(jsonElement: JsonElement, publicKeyFile: File) {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val connection = this.openConnection() as HttpURLConnection
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
    }
}