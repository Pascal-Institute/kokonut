package kokonut

import kotlinx.serialization.Serializable
import java.security.MessageDigest

@Serializable
data class Block(
    val index: Long,
    val previousHash: String,
    val timestamp: Long,
    val ticker : String,
    val data: BlockData,
    val nonce: Double,
    val hash: String
) {
    companion object {
        fun calculateHash(index: Long, previousHash: String, timestamp: Long, ticker: String, nonce : Double, data: BlockData): String {
            val input = "$index$previousHash$timestamp$ticker$data$nonce"
            return MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray())
                .fold("") { str, it -> str + "%02x".format(it) }
        }
    }
}
