package kokonut

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher

class Utility {
    companion object {

        val difficulty = 4

        fun generateKey(): KeyPair {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            val keyPair: KeyPair = keyPairGenerator.generateKeyPair()

            try {

                val publicKey: PublicKey = keyPair.public
                val privateKey: PrivateKey = keyPair.private

                val dataToEncrypt = "Hello, World!"

                val cipher = Cipher.getInstance("RSA")
                cipher.init(Cipher.ENCRYPT_MODE, publicKey)
                val encryptedData = cipher.doFinal(dataToEncrypt.toByteArray())
                val encryptedDataBase64 = Base64.getEncoder().encodeToString(encryptedData)
                println("Encrypted Data: $encryptedDataBase64")

                cipher.init(Cipher.DECRYPT_MODE, privateKey)
                val decryptedData = cipher.doFinal(Base64.getDecoder().decode(encryptedDataBase64))
                println("Decrypted Data: ${String(decryptedData)}")


            } catch (e: Exception) {
                e.printStackTrace()
            }
            return keyPair
        }

        fun readPemFile(filePath: String): String {
            return File(filePath).readText()
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\n", "")
        }

        fun loadPublicKey(pemPath: String): PublicKey {
            val publicKeyPEM = readPemFile(pemPath)
            val keyFactory = KeyFactory.getInstance("RSA")
            val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyPEM))
            return keyFactory.generatePublic(keySpec)
        }

        fun loadPrivateKey(pemPath: String): PrivateKey {
            val privateKeyPEM = readPemFile(pemPath)
            val keyFactory = KeyFactory.getInstance("RSA")
            val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyPEM))
            return keyFactory.generatePrivate(keySpec)
        }

        fun saveKeyPairToFile(keyPair: KeyPair, privateKeyFilePath: String, publicKeyFilePath: String) {
            val publicKeyEncoded = Base64.getEncoder().encodeToString(keyPair.public.encoded)
            File(publicKeyFilePath).writeText("-----BEGIN PUBLIC KEY-----\n$publicKeyEncoded\n-----END PUBLIC KEY-----")

            val privateKeyEncoded = Base64.getEncoder().encodeToString(keyPair.private.encoded)
            File(privateKeyFilePath).writeText("-----BEGIN PRIVATE KEY-----\n$privateKeyEncoded\n-----END PRIVATE KEY-----")
        }

        fun calculateHash(publicKey: PublicKey): String {
            val input = "$publicKey"
            return MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray())
                .fold("") { str, it -> str + "%02x".format(it) }
        }

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
            val data = BlockData(miner = "d", comment = "Pascal Institute")
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
