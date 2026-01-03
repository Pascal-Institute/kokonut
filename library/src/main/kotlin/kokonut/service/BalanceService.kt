package kokonut.service

import kokonut.core.Block
import kokonut.core.Transaction
import kokonut.config.NetworkConfig
import kokonut.persistence.repository.BlockRepository
import kotlin.math.pow

/**
 * Service for balance calculations and financial operations
 * Handles all currency-related queries
 */
class BalanceService(
    private val repository: BlockRepository,
    private val config: NetworkConfig = NetworkConfig()
) {
    
    /**
     * Calculate balance for a specific address
     * Sums all received transactions minus sent transactions
     */
    fun getBalance(address: String): Double {
        val chain = repository.findAll()
        var balance = 0.0
        
        chain.forEach { block ->
            block.data.transactions.forEach { tx ->
                when {
                    tx.receiver == address -> balance += tx.remittance
                    tx.sender == address -> balance -= tx.remittance
                }
            }
        }
        
        return truncate(balance)
    }
    
    /**
     * Get treasury balance
     */
    fun getTreasuryBalance(): Double {
        return getBalance(config.treasuryAddress)
    }
    
    /**
     * Calculate total currency volume in circulation
     * Genesis mint + all block rewards
     */
    fun getTotalCurrencyVolume(): Double {
        val chain = repository.findAll()
        var total = 0.0
        
        chain.forEach { block ->
            block.data.transactions.forEach { tx ->
                // Count genesis mint and block rewards (from treasury)
                if (tx.sender == config.treasuryAddress || 
                    tx.transaction == "GENESIS_MINT" || 
                    tx.transaction == "BLOCK_REWARD") {
                    total += tx.remittance
                }
            }
        }
        
        return truncate(total)
    }
    
    /**
     * Get all balances for multiple addresses efficiently
     */
    fun getBalances(addresses: List<String>): Map<String, Double> {
        val balances = addresses.associateWith { 0.0 }.toMutableMap()
        val chain = repository.findAll()
        
        chain.forEach { block ->
            block.data.transactions.forEach { tx ->
                if (tx.receiver in addresses) {
                    balances[tx.receiver] = (balances[tx.receiver] ?: 0.0) + tx.remittance
                }
                if (tx.sender in addresses) {
                    balances[tx.sender] = (balances[tx.sender] ?: 0.0) - tx.remittance
                }
            }
        }
        
        return balances.mapValues { truncate(it.value) }
    }
    
    /**
     * Truncate to 6 decimal places (micro-KNT precision)
     */
    private fun truncate(value: Double): Double {
        val scale = 10.0.pow(6)
        return kotlin.math.floor(value * scale) / scale
    }
}
