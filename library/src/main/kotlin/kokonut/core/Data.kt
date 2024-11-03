package kokonut.core

import kokonut.core.BlockChain.Companion.TICKER
import kotlinx.serialization.Serializable

@Serializable
data class Data(
    var reward: Double,
    val ticker: String = TICKER,
    val miner: String,
    var transactions : List<Transaction> = emptyList(),
    val comment: String
)