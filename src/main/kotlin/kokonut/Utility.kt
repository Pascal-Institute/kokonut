package kokonut

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher

class Utility {
    companion object {

        var difficulty = 6

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

        @Deprecated("This function is deprecated from 1.0.6")
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


        fun sendHttpPostRequest(urlString: String, jsonElement: JsonElement, publicKeyFile: File) {
            val url = URL(urlString)
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connection.setRequestProperty("Accept", "application/json")

                connection.outputStream.use { os ->
                    // Write JSON part
                    writePart(os, boundary, "json", "application/json; charset=UTF-8", jsonElement.toString().toByteArray(Charsets.UTF_8))

                    // Write file part
                    writeFilePart(os, boundary, "public_key", "application/x-pem-file", publicKeyFile)

                    // End of multipart
                    os.write("--$boundary--\r\n".toByteArray(Charsets.UTF_8))
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

        private fun writePart(outputStream: OutputStream, boundary: String, name: String, contentType: String, content: ByteArray) {
            outputStream.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
            outputStream.write("Content-Disposition: form-data; name=\"$name\"\r\n".toByteArray(Charsets.UTF_8))
            outputStream.write("Content-Type: $contentType\r\n\r\n".toByteArray(Charsets.UTF_8))
            outputStream.write(content)
            outputStream.write("\r\n".toByteArray(Charsets.UTF_8))
        }

        private fun writeFilePart(outputStream: OutputStream, boundary: String, name: String, contentType: String, file: File) {
            outputStream.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
            outputStream.write("Content-Disposition: form-data; name=\"$name\"; filename=\"${file.name}\"\r\n".toByteArray(Charsets.UTF_8))
            outputStream.write("Content-Type: $contentType\r\n\r\n".toByteArray(Charsets.UTF_8))
            Files.copy(file.toPath(), outputStream)
            outputStream.write("\r\n".toByteArray(Charsets.UTF_8))
        }
    }
}
