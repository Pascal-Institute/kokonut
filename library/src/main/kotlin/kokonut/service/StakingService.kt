package kokonut.service

import kokonut.config.NetworkConfig
import kokonut.core.BlockDataType
import kokonut.persistence.repository.BlockRepository
import kotlin.math.pow

/**
 * Service for staking operations and stake calculations
 * Manages validator stakes and stake locking
 */
class StakingService(
    private val repository: BlockRepository,
    private val balanceService: BalanceService,
    private val config: NetworkConfig = NetworkConfig()
) {
    
    /**
     * Compute current stake for all addresses
     * Stake is locked balance in the stake vault
     */
    fun computeStakeByAddress(): Map<String, Double> {
        val stakes = mutableMapOf<String, Double>()
        val chain = repository.findAll()
        
        chain.forEach { block ->
            when (block.data.type) {
                BlockDataType.STAKE_LOCK -> {
                    block.data.transactions.forEach { tx ->
                        if (tx.receiver == config.stakeVaultAddress) {
                            stakes[tx.sender] = (stakes[tx.sender] ?: 0.0) + tx.remittance
                        }
                    }
                }
                else -> {}
            }
        }
        
        return stakes.mapValues { truncate(it.value) }
    }
    
    /**
     * Get staked amount for a specific address
     */
    fun getStakedAmount(address: String): Double {
        return computeStakeByAddress()[address] ?: 0.0
    }
    
    /**
     * Check if address has sufficient stake
     */
    fun hasSufficientStake(address: String, requiredStake: Double): Boolean {
        return getStakedAmount(address) >= requiredStake
    }
    
    /**
     * Check if address can lock additional stake
     */
    fun canLockStake(address: String, amount: Double): Boolean {
        val availableBalance = balanceService.getBalance(address)
        return availableBalance >= amount
    }
    
    /**
     * Calculate total staked value in network
     */
    fun getTotalStaked(): Double {
        return truncate(computeStakeByAddress().values.sum())
    }
    
    /**
     * Get list of addresses with minimum stake
     */
    fun getQualifiedValidators(minimumStake: Double): List<String> {
        return computeStakeByAddress()
            .filter { it.value >= minimumStake }
            .map { it.key }
    }
    
    /**
     * Truncate to 6 decimal places (micro-KNT precision)
     */
    private fun truncate(value: Double): Double {
        val scale = 10.0.pow(6)
        return kotlin.math.floor(value * scale) / scale
    }
}
