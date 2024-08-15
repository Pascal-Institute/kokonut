package kokonut

import kotlinx.serialization.Serializable
import java.math.BigInteger
import java.security.MessageDigest

@Serializable
data class Block(
    val version : Int ? = null,
    val index: Long,
    val previousHash: String,
    val timestamp: Long,
    val ticker : String,
    val data: BlockData,
    val difficulty : Int ? = null,
    val nonce: Long,
    val hash: String,
    val reward : Double ? = 0.000000
) {
    companion object {

        fun calculateHash(version : Int, index: Long, previousHash: String, timestamp: Long, ticker: String, data: BlockData, difficulty: Int, nonce : Long): String {
            val input = "$version$index$previousHash$timestamp$ticker$data$difficulty$nonce"
            return MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray())
                .fold("") { str, it -> str + "%02x".format(it) }
        }
    }

    fun calculateHash(timestamp: Long ,nonce : Long): String {
        return calculateHash(version!!, index, previousHash, timestamp, ticker, data, difficulty!!, nonce)
    }
}
