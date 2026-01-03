package kokonut.crypto

import java.io.File
import java.security.*
import kokonut.state.ValidatorState

/**
 * Wallet for validator operations
 * Refactored to use new crypto utilities
 */
class Wallet(
    val privateKeyFile: File,
    val publicKeyFile: File
) {
    var validationState = ValidatorState.READY
    
    val privateKey: PrivateKey = KeyManager.loadPrivateKey(privateKeyFile)
    val publicKey: PublicKey = KeyManager.loadPublicKey(publicKeyFile)
    private var isValid = false
    
    var validatorAddress = "0000000000000000000000000000000000000000000000000000000000000000"
    
    init {
        val data = validatorAddress.toByteArray()
        val signature = SignatureUtil.signData(data, privateKey)
        
        isValid = SignatureUtil.verifySignature(data, signature, publicKey)
        
        if (isValid) {
            validatorAddress = HashCalculator.calculateAddressHash(publicKey)
            println("Wallet is Valid")
            println("Validator Address : $validatorAddress")
        } else {
            println("Wallet is Invalid")
        }
    }
    
    fun isValid(): Boolean = isValid
    
    companion object {
        /**
         * Generate a new key pair
         */
        fun generateKey(): KeyPair = KeyManager.generateKeyPair()
        
        /**
         * Sign data using private key
         */
        fun signData(data: ByteArray, privateKey: PrivateKey): ByteArray {
            return SignatureUtil.signData(data, privateKey)
        }
        
        /**
         * Verify signature using public key
         */
        fun verifySignature(
            data: ByteArray,
            signatureBytes: ByteArray,
            publicKey: PublicKey
        ): Boolean {
            return SignatureUtil.verifySignature(data, signatureBytes, publicKey)
        }
        
        /**
         * Save key pair to files
         */
        fun saveKeyPairToFile(
            keyPair: KeyPair,
            privateKeyFilePath: String,
            publicKeyFilePath: String
        ) {
            KeyManager.saveKeyPairToFile(keyPair, privateKeyFilePath, publicKeyFilePath)
        }
        
        /**
         * Load public key from file path
         */
        fun loadPublicKey(pemPath: String): PublicKey {
            return KeyManager.loadPublicKey(pemPath)
        }
        
        /**
         * Load private key from file path
         */
        fun loadPrivateKey(pemPath: String): PrivateKey {
            return KeyManager.loadPrivateKey(pemPath)
        }
    }
}
