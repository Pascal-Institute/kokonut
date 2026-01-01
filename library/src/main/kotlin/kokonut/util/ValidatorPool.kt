package kokonut.util

import kokonut.core.BlockChain
import kokonut.core.Validator
import kotlin.random.Random

/** ValidatorPool manages all validators in the PoS network */
class ValidatorPool {
    companion object {
        const val MINIMUM_STAKE = 1.0 // Minimum KNT to become validator
        const val VALIDATOR_REWARD_PERCENTAGE = 0.01 // Reward ratio (paid from treasury)
        const val VALIDATOR_REWARD_TX = "VALIDATOR_REWARD"
    }

    /**
     * Deprecated: staking is chain-derived via stakeVault transfers.
     * Use the Full Node stake-lock route to append a STAKE_LOCK block.
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

    private fun computeStakeByAddress(): Map<String, Double> {
        val stakeVault = BlockChain.getStakeVaultAddress()
        val chain = BlockChain.getChain()

        val stakeByAddress = mutableMapOf<String, Double>()
        chain.forEach { block ->
            block.data.transactions.forEach { tx ->
                if (tx.receiver == stakeVault && tx.remittance > 0.0) {
                    stakeByAddress[tx.sender] = (stakeByAddress[tx.sender] ?: 0.0) + tx.remittance
                }
            }
        }
        return stakeByAddress
    }

    private fun computeBlocksValidatedByAddress(): Map<String, Long> {
        val chain = BlockChain.getChain()
        val countByAddress = mutableMapOf<String, Long>()
        chain.forEach { block ->
            val address = block.data.validator
            if (address.isNotBlank() && address != "ONBOARDING" && address != "FUEL_REGISTRY") {
                countByAddress[address] = (countByAddress[address] ?: 0L) + 1L
            }
        }
        return countByAddress
    }

    private fun computeRewardsEarnedByAddress(): Map<String, Double> {
        val treasury = BlockChain.getTreasuryAddress()
        val chain = BlockChain.getChain()
        val rewardsByAddress = mutableMapOf<String, Double>()
        chain.forEach { block ->
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
     * Select a validator based on stake-weighted probability Higher stake = higher chance of being
     * selected
     */
    fun selectValidator(): Validator? {
        val activeValidators = getActiveValidators()

        if (activeValidators.isEmpty()) {
            println("No active validators available")
            return null
        }

        val totalStake = activeValidators.sumOf { it.stakedAmount }
        val randomValue = Random.nextDouble(totalStake)

        var cumulativeStake = 0.0
        for (validator in activeValidators) {
            cumulativeStake += validator.stakedAmount
            if (randomValue <= cumulativeStake) {
                println(
                        "Selected validator: ${validator.address} (Stake: ${validator.stakedAmount})"
                )
                return validator
            }
        }

        return activeValidators.last() // Fallback
    }

    /**
     * Deprecated: rewards are recorded on-chain as a treasury-paid transaction
     * in the validated block.
     */
    fun rewardValidator(address: String, reward: Double) {
        return
    }

    /** Get all active validators */
    fun getActiveValidators(): List<Validator> {
        val stakeByAddress = computeStakeByAddress()
        val blocksByAddress = computeBlocksValidatedByAddress()
        val rewardsByAddress = computeRewardsEarnedByAddress()

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
    fun getValidator(address: String): Validator? {
        val stake = computeStakeByAddress()[address] ?: 0.0
        if (stake <= 0.0) return null

        val blocksValidated = computeBlocksValidatedByAddress()[address] ?: 0L
        val rewardsEarned = computeRewardsEarnedByAddress()[address] ?: 0.0

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
