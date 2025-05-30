package kokonut.util

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kokonut.Policy
import kokonut.core.Block
import kokonut.core.BlockChain
import kokonut.core.BlockChain.Companion.fullNode
import kokonut.util.Utility.Companion.writeFilePart
import kokonut.util.Utility.Companion.writePart
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class API {
    companion object {
        fun URL.isHealthy(): Boolean {
            return try {
                val connection = this.openConnection() as HttpURLConnection
                connection.connectTimeout = 1500
                connection.requestMethod = "GET"
                connection.connect()

                val responseCode = connection.responseCode
                println("Node is healthy : $responseCode")
                responseCode in 200..299
            } catch (e: Exception) {
                println(e.message)
                false
            }
        }

        fun URL.getGenesisBlock(): Block {
            val url = URL("${this}/getGenesisBlock")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"

            return try {
                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val response = reader.use { it.readText() }

                    Json.decodeFromString(response)

                } else {
                    throw RuntimeException("GET request failed with response code $responseCode")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            } finally {
                conn.disconnect()
            }
        }

        fun URL.getFullNodes(): List<FullNode> {
            val url = URL("${this}/getFullNodes")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"

            return try {
                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val response = reader.use { it.readText() }

                    Json.decodeFromString(response)

                } else {
                    throw RuntimeException("GET request failed with response code $responseCode")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            } finally {
                conn.disconnect()
            }
        }

        fun URL.getChain(): MutableList<Block> {
            val url = URL("${this}/getChain")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"

            return try {
                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val response = reader.use { it.readText() }

                    Json.decodeFromString(response)

                } else {
                    throw RuntimeException("GET request failed with response code $responseCode")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            } finally {
                conn.disconnect()
            }
        }

        fun URL.getReward(index: Long): Double? {
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

                    response.toDoubleOrNull()
                } else {
                    println("GET request failed with response code $responseCode")
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                conn.disconnect()
            }
        }

        fun URL.getPolicy(): Policy {
            val url = URL("${this}/getPolicy")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"

            return try {
                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val response = reader.use { it.readText() }

                    Json.decodeFromString(response)

                } else {
                    throw RuntimeException("GET request failed with response code $responseCode")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            } finally {
                conn.disconnect()
            }
        }

        fun URL.getCertified(byteArray: ByteArray, publicKeyFile: File) {
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
                        byteArray
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

        fun URL.startMining(publicKeyFile: File) : Boolean {
            val connection = URL("${this}/startMining").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-pem-file")
            connection.setRequestProperty("Accept", "application/json")

            try {
                publicKeyFile.inputStream().use { fis ->
                    connection.outputStream.use { os ->
                        fis.copyTo(os)
                    }
                }

                // Check response
                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { reader ->
                        val response = reader.readText()
                        println("Response: $response, Start Mining")
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

            return true
        }

        fun URL.propagate() : HttpResponse {
            val client = HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json { prettyPrint = true })
                }
            }

            val response : HttpResponse

            runBlocking {
               response = client.post(this@propagate.path + "/propagate?size=${BlockChain.getChainSize()}&id=${fullNode.id}&address=${fullNode.address}")
            }

            return response
        }

        fun URL.addBlock(jsonElement: JsonElement, publicKeyFile: File) {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val connection = URL("${this}/addBlock").openConnection() as HttpURLConnection
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

        fun URL.stopMining(publicKeyFile: File) {
            val connection = this.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-pem-file")
            connection.setRequestProperty("Accept", "application/json")

            try {
                publicKeyFile.inputStream().use { fis ->
                    connection.outputStream.use { os ->
                        fis.copyTo(os)
                    }
                }

                // Check response
                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { reader ->
                        val response = reader.readText()
                        println("Response: $response, Stop Mining")
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