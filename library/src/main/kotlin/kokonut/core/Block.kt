package kokonut.core

import java.security.MessageDigest
import kotlinx.serialization.Serializable

@Serializable
data class Block(
        val index: Long,
        val previousHash: String,
        var timestamp: Long,
        val data: Data,
        val validatorSignature: String = "", // PoS: Validator's signature
        var hash: String,
) {
    companion object {

        fun calculateHash(block: Block): String {
            return block.calculateHash()
        }
    }

    fun calculateHash(): String {
        // PoS: Hash includes validator signature instead of nonce/difficulty
        val input = "$index$previousHash$timestamp$data$validatorSignature"
        hash =
                MessageDigest.getInstance("SHA-256").digest(input.toByteArray()).fold("") { str, it
                    ->
                    str + "%02x".format(it)
                }
        return hash
    }

    fun isValid(): Boolean {
        return (calculateHash() == hash)
    }
}
