package kokonut.util

import kokonut.util.Utility.Companion.calculateHash
import java.io.File
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

class Wallet(privateKeyFile : File, val publicKeyFile: File) {

    companion object {
        fun generateKey(): KeyPair {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            val keyPair: KeyPair = keyPairGenerator.generateKeyPair()
            return keyPair
        }

        fun signData(data: ByteArray, privateKey: PrivateKey): ByteArray {
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initSign(privateKey)
            signature.update(data)
            return signature.sign()
        }

        fun verifySignature(data: ByteArray, signatureBytes: ByteArray, publicKey: PublicKey): Boolean {
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initVerify(publicKey)
            signature.update(data)
            return signature.verify(signatureBytes)
        }

        fun readPemFile(filePath: String): String {
            return File(filePath).readText()
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
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
    }

    var privateKey : PrivateKey = loadPrivateKey(privateKeyFile.path)
    var publicKey : PublicKey = loadPublicKey(publicKeyFile.path)
    private var isValid = false

    var miner = "0000000000000000000000000000000000000000000000000000000000000000"

    init {
        val data = miner.toByteArray()
        val signature = signData(data, privateKey)

        isValid = verifySignature(data, signature, publicKey)

        if(isValid){
            miner = calculateHash(publicKey)
            println("Wallet is Valid")
            println("Miner : $miner")
        }else{
            println("Wallet is Invalid")
            println("Miner : $miner")
        }
    }

    fun isValid() : Boolean{
        return isValid
    }
}