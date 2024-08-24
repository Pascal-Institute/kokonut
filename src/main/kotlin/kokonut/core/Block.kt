package kokonut.core

import kokonut.URL.FUEL_NODE
import kokonut.Utility
import kotlinx.serialization.Serializable
import java.security.MessageDigest

@Serializable
data class Block(
    val version: Int? = null,
    val index: Long,
    val previousHash: String,
    var timestamp: Long,
    val data: Data,
    val difficulty: Int? = null,
    var nonce: Long,
    var hash: String,
) {
    companion object {

        fun calculateHash(block: Block) : String {
            return block.calculateHash()
        }

        /**
         * Only valid for version <=2.
         * */
        fun calculateHash(
            version: Int,
            index: Long,
            previousHash: String,
            timestamp: Long,
            ticker: String,
            data: Data,
            difficulty: Int,
            nonce: Long
        ): String {
            val input = "$version$index$previousHash$timestamp$ticker$data$difficulty$nonce"
            return MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray())
                .fold("") { str, it -> str + "%02x".format(it) }
        }

        fun calculateHash(
            version: Int,
            index: Long,
            previousHash: String,
            timestamp: Long,
            ticker: String,
            data: Data,
            difficulty: Int,
            nonce: Long,
            reward: Double
        ): String {
            val input = "$version$index$previousHash$timestamp$ticker$data$difficulty$nonce$reward"
            return MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray())
                .fold("") { str, it -> str + "%02x".format(it) }
        }
    }

    @Deprecated("until kokonut 1.0.7")
    fun calculateHash(timestamp: Long, nonce: Long): String {
        return calculateHash(version!!, index, previousHash, timestamp, data.ticker, data, difficulty!!, nonce)
    }

    @Deprecated("until kokonut 1.3.0")
    fun calculateHash(timestamp: Long, nonce: Long, reward: Double): String {
        return calculateHash(version!!, index, previousHash, timestamp, data.ticker, data, difficulty!!, nonce, reward)
    }

    fun calculateHash(): String {
        val input = "$version$index$previousHash$timestamp$data$difficulty$nonce"
        hash = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
        return hash
    }

    fun isValid(): Boolean {
        return (calculateHash() == hash)
    }
}
