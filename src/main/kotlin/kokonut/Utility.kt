package kokonut

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kokonut.URL.FULL_NODE_0
import kokonut.core.Block
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.file.Files
import java.security.*
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.*

class Utility {
    companion object {
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

        //SQLlite
        fun getDatabasePath(): String {
            val classLoader = Thread.currentThread().contextClassLoader
            val resource = classLoader.getResource("kovault.db")
            return resource?.toURI()?.path ?: throw IllegalStateException("Database file not found")
        }

        fun tableExists(connection: Connection, tableName: String): Boolean {
            val metaData = connection.metaData
            val resultSet: ResultSet = metaData.getTables(null, null, tableName, null)
            val exists = resultSet.next()
            resultSet.close()
            return exists
        }

        fun printTableStructure(connection: Connection, tableName: String) {
            val query = "PRAGMA table_info($tableName);"
            val statement: Statement = connection.createStatement()
            val resultSet: ResultSet = statement.executeQuery(query)

            while (resultSet.next()) {
                val columnName = resultSet.getString("name")
                val columnType = resultSet.getString("type")
                val isNullable = resultSet.getString("notnull")
                val defaultValue = resultSet.getString("dflt_value")
                println("Column: $columnName, Type: $columnType, Nullable: $isNullable, Default: $defaultValue")
            }

            resultSet.close()
            statement.close()
        }

        fun printTableData(connection: Connection, tableName: String) {
            val query = "SELECT * FROM $tableName;"
            val statement: Statement = connection.createStatement()
            val resultSet: ResultSet = statement.executeQuery(query)

            // 열의 이름을 얻어서 헤더로 출력
            val metaData = resultSet.metaData
            val columnCount = metaData.columnCount
            for (i in 1..columnCount) {
                print("${metaData.getColumnName(i)}\t")
            }
            println()

            // 데이터 출력
            while (resultSet.next()) {
                for (i in 1..columnCount) {
                    print("${resultSet.getString(i)}\t")
                }
                println()
            }

            resultSet.close()
            statement.close()
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

        fun sendHttpGetReward(urlString: String, index: Long): Double? {
            val url = URL("$urlString/getReward?index=$index")
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

        fun sendHttpPostRequest(urlString: String, byteArray: ByteArray, publicKeyFile: File) {
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