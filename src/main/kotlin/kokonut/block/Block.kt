package kokonut.block

import kokonut.URL.FUEL_NODE
import kokonut.Utility
import kotlinx.serialization.Serializable
import java.security.MessageDigest

@Serializable
data class Block(
    val version: Int? = null,
    val index: Long,
    val previousHash: String,
    val timestamp: Long,
    val ticker: String,
    val data: BlockData,
    val difficulty: Int? = null,
    val nonce: Long,
    val hash: String,
    val reward: Double? = 0.000000
) {
    companion object {

        /**
         * Only valid for version <=2.
         * */
        fun calculateHash(
            version: Int,
            index: Long,
            previousHash: String,
            timestamp: Long,
            ticker: String,
            data: BlockData,
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
            data: BlockData,
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
        return calculateHash(version!!, index, previousHash, timestamp, ticker, data, difficulty!!, nonce)
    }

    fun calculateHash(timestamp: Long, nonce: Long, reward: Double): String {
        return calculateHash(version!!, index, previousHash, timestamp, ticker, data, difficulty!!, nonce)
    }

    fun isValid(): Boolean {

        val policy = Utility.sendHttpGetPolicy(FUEL_NODE)
        if (policy.version != this.version ||
            policy.difficulty != this.difficulty ||
            policy.reward != this.reward
        ) {
            return false
        }

        return true
    }
}
