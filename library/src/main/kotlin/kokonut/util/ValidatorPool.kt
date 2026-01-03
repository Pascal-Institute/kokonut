package kokonut.util

import kokonut.core.BlockChain
import kokonut.core.Transaction
import kokonut.core.Validator
import kotlin.random.Random

/** ValidatorPool manages all validators in the PoS network */
class ValidatorPool {
    companion object {
        const val MINIMUM_STAKE = 1.0 // Minimum KNT to become validator
        const val VALIDATOR_REWARD_PERCENTAGE = 0.01 // Reward ratio (paid from treasury)
        const val VALIDATOR_REWARD_TX = "VALIDATOR_REWARD"
        const val SLASH_TX_PREFIX = "SLASH:"

        fun createSlashTransaction(victimAddress: String, amount: Double): Transaction {
            return Transaction(
                    transaction = SLASH_TX_PREFIX + victimAddress,
                    sender = BlockChain.getStakeVaultAddress(),
                    receiver = BlockChain.getTreasuryAddress(),
                    remittance = amount,
                    commission = 0.0
            )
        }
    }

    /**
     * Deprecated: staking is chain-derived via stakeVault transfers. Use the Full Node stake-lock
     * route to append a STAKE_LOCK block.
     */
    fun stake(address: String, amount: Double): Boolean {
        println("stake() is deprecated. Use on-chain stakeVault transfers.")
        return false
    }

    /** Deprecated: unstaking is not supported in current on-chain model. */
    fun unstake(address: String, amount: Double): Boolean {
        println("unstake() is not supported in current on-chain model.")
        return false
    }

    private fun computeStakeByAddress(limitIndex: Long? = null): Map<String, Double> {
        val stakeVault = BlockChain.getStakeVaultAddress()
        val chain = BlockChain.getChain()
        // Filter chain by limitIndex if provided
        val effectiveChain =
                if (limitIndex != null) chain.filter { it.index <= limitIndex } else chain

        val stakeByAddress = mutableMapOf<String, Double>()
        effectiveChain.forEach { block ->
            block.data.transactions.forEach { tx ->
                if (tx.receiver == stakeVault && tx.remittance > 0.0) {
                    // Staking: Add to sender's stake
                    stakeByAddress[tx.sender] = (stakeByAddress[tx.sender] ?: 0.0) + tx.remittance
                }

                // Handle standard unstaking (if supported)
                if (tx.sender == stakeVault &&
                                tx.remittance > 0.0 &&
                                !tx.transaction.startsWith(SLASH_TX_PREFIX)
                ) {
                    // Unstaking: Subtract from receiver's stake
                    stakeByAddress[tx.receiver] =
                            (stakeByAddress[tx.receiver] ?: 0.0) - tx.remittance
                }

                // Handle Slashing
                if (tx.sender == stakeVault && tx.transaction.startsWith(SLASH_TX_PREFIX)) {
                    val victim = tx.transaction.removePrefix(SLASH_TX_PREFIX)
                    stakeByAddress[victim] = (stakeByAddress[victim] ?: 0.0) - tx.remittance
                }
            }
        }
        // Filter out negative or zero stakes (floating point safety)
        return stakeByAddress.filterValues { it > 0.0001 }
    }

    private fun computeBlocksValidatedByAddress(limitIndex: Long? = null): Map<String, Long> {
        val chain = BlockChain.getChain()
        val effectiveChain =
                if (limitIndex != null) chain.filter { it.index <= limitIndex } else chain

        val countByAddress = mutableMapOf<String, Long>()
        effectiveChain.forEach { block ->
            val address = block.data.validator
            if (address.isNotBlank() && address != "ONBOARDING" && address != "FUEL_REGISTRY") {
                countByAddress[address] = (countByAddress[address] ?: 0L) + 1L
            }
        }
        return countByAddress
    }

    private fun computeRewardsEarnedByAddress(limitIndex: Long? = null): Map<String, Double> {
        val treasury = BlockChain.getTreasuryAddress()
        val chain = BlockChain.getChain()
        val effectiveChain =
                if (limitIndex != null) chain.filter { it.index <= limitIndex } else chain

        val rewardsByAddress = mutableMapOf<String, Double>()
        effectiveChain.forEach { block ->
            block.data.transactions.forEach { tx ->
                if (tx.transaction == VALIDATOR_REWARD_TX && tx.sender == treasury) {
                    rewardsByAddress[tx.receiver] =
                            (rewardsByAddress[tx.receiver] ?: 0.0) + tx.remittance
                }
            }
        }
        return rewardsByAddress
    }

    /**
     * Select a validator based on stake-weighted probability. Uses a DETERMINISTIC seed to ensure
     * consensus (all nodes pick the same validator).
     * @param seed Random seed (usually previousBlockHash + index)
     * @param limitIndex Calculate stake based on history up to this index
     */
    fun selectValidator(seed: Long, limitIndex: Long? = null): Validator? {
        val activeValidators = getActiveValidators(limitIndex)

        if (activeValidators.isEmpty()) {
            println("No active validators available")
            return null
        }

        val totalStake = activeValidators.sumOf { it.stakedAmount }
        // Use deterministic Random based on seed
        val random = Random(seed)
        val randomValue = random.nextDouble(totalStake)

        var cumulativeStake = 0.0
        for (validator in activeValidators) {
            cumulativeStake += validator.stakedAmount
            if (randomValue <= cumulativeStake) {
                // println("Selected validator: ${validator.address} (Stake:
                // ${validator.stakedAmount})")
                return validator
            }
        }

        return activeValidators.last() // Fallback
    }

    /**
     * Deprecated: rewards are recorded on-chain as a treasury-paid transaction in the validated
     * block.
     */
    fun rewardValidator(address: String, reward: Double) {
        return
    }

    /** Get all active validators */
    fun getActiveValidators(limitIndex: Long? = null): List<Validator> {
        val stakeByAddress = computeStakeByAddress(limitIndex)
        val blocksByAddress = computeBlocksValidatedByAddress(limitIndex)
        val rewardsByAddress = computeRewardsEarnedByAddress(limitIndex)

        return stakeByAddress.entries
                .map { (address, stake) ->
                    Validator(
                            address = address,
                            stakedAmount = Utility.truncate(stake),
                            isActive = stake >= MINIMUM_STAKE,
                            blocksValidated = blocksByAddress[address] ?: 0L,
                            rewardsEarned = Utility.truncate(rewardsByAddress[address] ?: 0.0)
                    )
                }
                .filter { it.isActive && it.stakedAmount >= MINIMUM_STAKE }
    }

    /** Get validator by address */
    fun getValidator(address: String, limitIndex: Long? = null): Validator? {
        val stake = computeStakeByAddress(limitIndex)[address] ?: 0.0
        if (stake <= 0.0) return null

        val blocksValidated = computeBlocksValidatedByAddress(limitIndex)[address] ?: 0L
        val rewardsEarned = computeRewardsEarnedByAddress(limitIndex)[address] ?: 0.0

        return Validator(
                address = address,
                stakedAmount = Utility.truncate(stake),
                isActive = stake >= MINIMUM_STAKE,
                blocksValidated = blocksValidated,
                rewardsEarned = Utility.truncate(rewardsEarned)
        )
    }

    /** Get total staked amount in network */
    fun getTotalStaked(): Double {
        return Utility.truncate(getActiveValidators().sumOf { it.stakedAmount })
    }
}
