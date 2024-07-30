package kokonut

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class Utility {
    companion object {
        fun sendHttpGetRequest(urlString: String?) {
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

        fun sendHttpPostRequest(url: String) {
            val url = URL(url)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            // JSON Serialization
            val data = BlockData(comment = "Pascal Institute")
            val json = Json.encodeToString(data)

            // Write JSON data to the request body.
            OutputStreamWriter(connection.outputStream).use { it.write(json) }

            // Read response
            val response = connection.inputStream.bufferedReader().readText()
            println("Response: $response")

            connection.disconnect()
        }
        fun sendHttpPostRequest(urlString: String, jsonElement: JsonElement) {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")

                // Convert JsonElement to String
                val jsonString = jsonElement.toString()

                // Write JSON string to the output stream
                connection.outputStream.use { os ->
                    val input = jsonString.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                // Check response
                val responseCode = connection.responseCode
                println("Response Code: $responseCode")
                connection.inputStream.bufferedReader().use { reader ->
                    val response = reader.readText()
                    println("Response: $response")
                }
            } finally {
                connection.disconnect()
            }
        }
    }
}