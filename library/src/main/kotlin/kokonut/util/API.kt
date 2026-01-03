package kokonut.util

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import kokonut.core.Block
import kokonut.core.BlockChain
import kokonut.core.BlockChain.Companion.fullNode
import kokonut.core.HandshakeRequest
import kokonut.core.Policy
import kokonut.util.Utility.Companion.writeFilePart
import kokonut.util.Utility.Companion.writePart
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

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

        /**
         * Get balance for a given address from Full Node
         * @param address The wallet address (validator address) to query
         * @return Balance as Double, or 0.0 if query fails
         */
        fun URL.getBalance(address: String): Double {
            val url = URL("${this}/getBalance?address=$address")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            return try {
                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val response = reader.use { it.readText() }

                    // Response is JSON: {"address": "...", "balance": 123.45, "ticker": "KNT"}
                    val jsonElement = Json.parseToJsonElement(response)
                    jsonElement.jsonObject["balance"]?.toString()?.toDoubleOrNull() ?: 0.0
                } else {
                    println("getBalance failed with response code $responseCode")
                    0.0
                }
            } catch (e: Exception) {
                println("getBalance error: ${e.message}")
                0.0
            } finally {
                conn.disconnect()
            }
        }

        /**
         * Perform handshake with Full Node to establish connection
         * @param publicKey REQUIRED public key of the Light Node for authentication
         * @return HandshakeResponse containing network information
         * @throws IllegalArgumentException if publicKey is blank
         */
        fun URL.performHandshake(publicKey: String): kokonut.core.HandshakeResponse {
            if (publicKey.isBlank()) {
                throw IllegalArgumentException(
                        "Public key is required for handshake. Please load your public key first."
                )
            }

            val url = URL("${this}/handshake")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("Content-Type", "application/json")

            return try {
                // Create handshake request
                val request = HandshakeRequest(nodeType = "LIGHT", publicKey = publicKey)

                val requestJson = Json.encodeToString(request)

                // Send request
                conn.outputStream.use { os -> os.write(requestJson.toByteArray(Charsets.UTF_8)) }

                val responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val response = reader.use { it.readText() }

                    Json.decodeFromString(response)
                } else {
                    // Even on error, try to parse response
                    val errorStream = conn.errorStream
                    if (errorStream != null) {
                        val reader = BufferedReader(InputStreamReader(errorStream))
                        val response = reader.use { it.readText() }
                        Json.decodeFromString(response)
                    } else {
                        kokonut.core.HandshakeResponse(
                                success = false,
                                message = "Handshake failed with HTTP code $responseCode"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kokonut.core.HandshakeResponse(
                        success = false,
                        message = "Handshake error: ${e.message}"
                )
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
                    writePart(os, boundary, "json", "application/json; charset=UTF-8", byteArray)

                    // Write file part
                    writeFilePart(
                            os,
                            boundary,
                            "public_key",
                            "application/x-pem-file",
                            publicKeyFile
                    )

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

        fun URL.startValidating(publicKeyFile: File): Boolean {
            val connection = URL("${this}/startValidating").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-pem-file")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("fileName", publicKeyFile.name)

            return try {
                publicKeyFile.inputStream().use { fis ->
                    connection.outputStream.use { os -> fis.copyTo(os) }
                }

                // Check response
                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { reader ->
                        val response = reader.readText()
                        println("Response: $response, Start Validating")
                    }
                    true
                } else {
                    println("Failed with HTTP error code: $responseCode")
                    connection.errorStream?.bufferedReader()?.use { reader ->
                        val errorResponse = reader.readText()
                        println("Error Response: $errorResponse")
                    }
                    false
                }
            } catch (e: IOException) {
                e.printStackTrace()
                false
            } finally {
                connection.disconnect()
            }
        }

        fun URL.stakeLock(wallet: Wallet, publicKeyFile: File, amount: Double): Boolean {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val connection = URL("${this}/stakeLock").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setRequestProperty("Accept", "application/json")

            val timestamp = System.currentTimeMillis()
            val validatorAddress = wallet.validatorAddress
            val message = "STAKE_LOCK|$validatorAddress|$amount|$timestamp"
            val signatureBytes = Wallet.signData(message.toByteArray(), wallet.privateKey)
            val signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes)

            return try {
                connection.outputStream.use { os ->
                    writePart(
                            os,
                            boundary,
                            "amount",
                            "text/plain; charset=UTF-8",
                            amount.toString().toByteArray(Charsets.UTF_8)
                    )
                    writePart(
                            os,
                            boundary,
                            "timestamp",
                            "text/plain; charset=UTF-8",
                            timestamp.toString().toByteArray(Charsets.UTF_8)
                    )
                    writePart(
                            os,
                            boundary,
                            "signature",
                            "text/plain; charset=UTF-8",
                            signatureBase64.toByteArray(Charsets.UTF_8)
                    )

                    writeFilePart(
                            os,
                            boundary,
                            "public_key",
                            "application/x-pem-file",
                            publicKeyFile
                    )

                    os.write("--$boundary--\r\n".toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { reader ->
                        val response = reader.readText()
                        println("Response: $response, Stake Lock")
                    }
                    true
                } else {
                    println("Failed with HTTP error code: $responseCode")
                    connection.errorStream?.bufferedReader()?.use { reader ->
                        val errorResponse = reader.readText()
                        println("Error Response: $errorResponse")
                    }
                    false
                }
            } catch (e: IOException) {
                e.printStackTrace()
                false
            } finally {
                connection.disconnect()
            }
        }

        fun URL.propagate(): HttpResponse {
            val client =
                    HttpClient(CIO) {
                        install(ContentNegotiation) { json(Json { prettyPrint = true }) }
                    }

            val response: HttpResponse

            runBlocking {
                response =
                        client.post(
                                this@propagate.path +
                                        "/propagate?size=${BlockChain.getChainSize()}&id=${fullNode.id}&address=${fullNode.address}"
                        )
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
                    writeFilePart(
                            os,
                            boundary,
                            "public_key",
                            "application/x-pem-file",
                            publicKeyFile
                    )

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

        fun URL.stopValidating(publicKeyFile: File): Boolean {
            val connection = URL("${this}/stopValidating").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-pem-file")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("fileName", publicKeyFile.name)

            return try {
                publicKeyFile.inputStream().use { fis ->
                    connection.outputStream.use { os -> fis.copyTo(os) }
                }

                // Check response
                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { reader ->
                        val response = reader.readText()
                        println("Response: $response, Stop Validating")
                    }
                    true
                } else {
                    println("Failed with HTTP error code: $responseCode")
                    connection.errorStream?.bufferedReader()?.use { reader ->
                        val errorResponse = reader.readText()
                        println("Error Response: $errorResponse")
                    }
                    false
                }
            } catch (e: IOException) {
                e.printStackTrace()
                false
            } finally {
                connection.disconnect()
            }
        }

        /**
         * Send heartbeat to Fuel Node for automatic registration and health monitoring
         * @param address The address of this Full Node
         * @return true if heartbeat was successful, false otherwise
         */
        fun URL.sendHeartbeat(address: String): Boolean {
            return try {
                val connection = URL("${this}/heartbeat").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("Content-Type", "application/json")

                val jsonPayload = """{"address":"$address"}"""
                connection.outputStream.use { os ->
                    os.write(jsonPayload.toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                connection.disconnect()

                responseCode in 200..299
            } catch (e: Exception) {
                println("⚠️ Heartbeat failed: ${e.message}")
                false
            }
        }
    }
}
