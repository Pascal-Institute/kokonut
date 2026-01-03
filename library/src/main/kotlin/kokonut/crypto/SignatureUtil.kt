package kokonut.crypto

import java.security.*

/**
 * Digital signature utilities for blockchain operations
 * Handles signing and verification using RSA
 */
object SignatureUtil {
    
    /**
     * Sign data using private key
     */
    fun signData(data: ByteArray, privateKey: PrivateKey): ByteArray {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }
    
    /**
     * Sign string data using private key
     */
    fun signData(data: String, privateKey: PrivateKey): ByteArray {
        return signData(data.toByteArray(), privateKey)
    }
    
    /**
     * Verify signature using public key
     */
    fun verifySignature(
        data: ByteArray,
        signatureBytes: ByteArray,
        publicKey: PublicKey
    ): Boolean {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initVerify(publicKey)
        signature.update(data)
        return signature.verify(signatureBytes)
    }
    
    /**
     * Verify signature for string data
     */
    fun verifySignature(
        data: String,
        signatureBytes: ByteArray,
        publicKey: PublicKey
    ): Boolean {
        return verifySignature(data.toByteArray(), signatureBytes, publicKey)
    }
}
