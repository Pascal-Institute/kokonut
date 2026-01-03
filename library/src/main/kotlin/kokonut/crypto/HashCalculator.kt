package kokonut.crypto

import java.security.MessageDigest
import java.security.PublicKey
import kotlin.text.Charsets.UTF_8

/**
 * Hash calculation utilities for blockchain operations
 * Centralized cryptographic hashing functions
 */
object HashCalculator {
    
    /**
     * Calculate SHA-256 hash of input string
     */
    fun calculateSHA256(input: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(UTF_8))
            .fold("") { str, it -> str + "%02x".format(it) }
    }
    
    /**
     * Calculate address hash from public key
     * Used for validator addresses
     */
    fun calculateAddressHash(publicKey: PublicKey): String {
        val keyBytes = publicKey.encoded
        return calculateSHA256(keyBytes.contentToString())
    }
    
    /**
     * Calculate hash for timestamp
     */
    fun calculateTimestampHash(timestamp: Long): String {
        return calculateSHA256(timestamp.toString())
    }
    
    /**
     * Calculate block hash from components
     */
    fun calculateBlockHash(
        index: Long,
        previousHash: String,
        timestamp: Long,
        data: String,
        validatorSignature: String
    ): String {
        val input = "$index$previousHash$timestamp$data$validatorSignature"
        return calculateSHA256(input)
    }
}
