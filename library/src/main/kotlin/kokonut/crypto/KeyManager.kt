package kokonut.crypto

import java.io.File
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

/**
 * Key management utilities for cryptographic operations
 * Handles key generation, loading, and saving
 */
object KeyManager {
    
    /**
     * Generate a new RSA key pair (2048-bit)
     */
    fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        return keyPairGenerator.generateKeyPair()
    }
    
    /**
     * Load public key from PEM file
     */
    fun loadPublicKey(pemPath: String): PublicKey {
        val publicKeyPEM = readPemFile(pemPath)
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyPEM))
        return keyFactory.generatePublic(keySpec)
    }
    
    /**
     * Load public key from File object
     */
    fun loadPublicKey(file: File): PublicKey {
        return loadPublicKey(file.path)
    }
    
    /**
     * Load private key from PEM file
     */
    fun loadPrivateKey(pemPath: String): PrivateKey {
        val privateKeyPEM = readPemFile(pemPath)
        val keyFactory = KeyFactory.getInstance("RSA")
        val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyPEM))
        return keyFactory.generatePrivate(keySpec)
    }
    
    /**
     * Load private key from File object
     */
    fun loadPrivateKey(file: File): PrivateKey {
        return loadPrivateKey(file.path)
    }
    
    /**
     * Save key pair to PEM files
     */
    fun saveKeyPairToFile(
        keyPair: KeyPair,
        privateKeyFilePath: String,
        publicKeyFilePath: String
    ) {
        val publicKeyEncoded = Base64.getEncoder().encodeToString(keyPair.public.encoded)
        File(publicKeyFilePath).writeText(
            "-----BEGIN PUBLIC KEY-----\n$publicKeyEncoded\n-----END PUBLIC KEY-----"
        )
        
        val privateKeyEncoded = Base64.getEncoder().encodeToString(keyPair.private.encoded)
        File(privateKeyFilePath).writeText(
            "-----BEGIN PRIVATE KEY-----\n$privateKeyEncoded\n-----END PRIVATE KEY-----"
        )
    }
    
    /**
     * Read and clean PEM file content
     */
    private fun readPemFile(filePath: String): String {
        return File(filePath)
            .readText()
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\n", "")
            .trim()
    }
}
