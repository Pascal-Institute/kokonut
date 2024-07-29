package kokonut

import kotlinx.serialization.Serializable
import java.security.MessageDigest

@Serializable
data class Block(
    val version : Int,
    val index: Long,
    val previousHash: String,
    val timestamp: Long,
    val ticker : String,
    val data: BlockData,
    val difficulty : Int,
    val nonce: Double,
    val hash: String
) {
    companion object {
        fun calculateHash(version : Int, index: Long, previousHash: String, timestamp: Long, ticker: String, data: BlockData, difficulty: Int, nonce : Double): String {
            val input = "$version$index$previousHash$timestamp$ticker$data$difficulty$nonce"
            return MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray())
                .fold("") { str, it -> str + "%02x".format(it) }
        }
    }
}
