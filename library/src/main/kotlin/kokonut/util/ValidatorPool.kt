package kokonut.util

import kokonut.core.Validator
import kotlin.random.Random

/** ValidatorPool manages all validators in the PoS network */
class ValidatorPool {
    private val validators = mutableMapOf<String, Validator>()

    companion object {
        const val MINIMUM_STAKE = 100.0 // Minimum KNT to become validator
        const val VALIDATOR_REWARD_PERCENTAGE = 0.01 // 1% of transaction fees
    }

    /** Register a new validator or update existing stake */
    fun stake(address: String, amount: Double): Boolean {
        if (amount < MINIMUM_STAKE) {
            println("Stake amount $amount is below minimum $MINIMUM_STAKE")
            return false
        }

        val validator = validators.getOrPut(address) { Validator(address, 0.0) }

        validator.stakedAmount += amount
        validator.isActive = true

        println("Validator $address staked $amount KNT (Total: ${validator.stakedAmount})")
        return true
    }

    /** Unstake and remove from validator pool */
    fun unstake(address: String, amount: Double): Boolean {
        val validator =
                validators[address]
                        ?: run {
                            println("Validator $address not found")
                            return false
                        }

        if (amount > validator.stakedAmount) {
            println("Insufficient staked amount")
            return false
        }

        validator.stakedAmount -= amount

        if (validator.stakedAmount < MINIMUM_STAKE) {
            validator.isActive = false
            println("Validator $address deactivated (below minimum stake)")
        }

        return true
    }

    /**
     * Select a validator based on stake-weighted probability Higher stake = higher chance of being
     * selected
     */
    fun selectValidator(): Validator? {
        val activeValidators =
                validators.values.filter { it.isActive && it.stakedAmount >= MINIMUM_STAKE }

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

    /** Reward the validator for validating a block */
    fun rewardValidator(address: String, reward: Double) {
        val validator = validators[address] ?: return
        validator.blocksValidated++
        validator.rewardsEarned += reward
        println("Validator $address rewarded $reward KNT (Total: ${validator.rewardsEarned})")
    }

    /** Get all active validators */
    fun getActiveValidators(): List<Validator> {
        return validators.values.filter { it.isActive && it.stakedAmount >= MINIMUM_STAKE }
    }

    /** Get validator by address */
    fun getValidator(address: String): Validator? {
        return validators[address]
    }

    /** Get total staked amount in network */
    fun getTotalStaked(): Double {
        return validators.values.filter { it.isActive }.sumOf { it.stakedAmount }
    }
}
