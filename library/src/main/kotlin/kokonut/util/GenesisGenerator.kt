package kokonut.util

import kokonut.core.*

/** Genesis Block Generator Creates the first block of the blockchain with network rules */
object GenesisGenerator {

    /** Create Genesis Block with network rules only (no Fuel Node list) */
    fun createGenesisBlock(
            networkId: String = "kokonut-mainnet",
            timestamp: Long = System.currentTimeMillis()
    ): Block {
        val networkRules =
                NetworkRules(
                        networkId = networkId,
                        minFuelStake = 1_000_000.0,
                        minFullStake = 100.0,
                        fuelConsensusThreshold = 0.67,
                        maxFuelNodes = 100,
                        faucetAmount = 100.0,
                        protocolVersion = 4
                )

        val genesisData =
                Data(
                        reward = 0.0,
                        ticker = "KNT",
                        validator = "GENESIS",
                        transactions = emptyList(),
                        comment = "Genesis Block - Kokonut Network",
                        type = BlockDataType.TRANSACTION,
                        networkRules = networkRules
                )

        val genesisBlock =
                Block(
                        version = 4,
                        index = 0,
                        previousHash = "0",
                        timestamp = timestamp,
                        data = genesisData,
                        validatorSignature = "",
                        hash = ""
                )

        genesisBlock.hash = genesisBlock.calculateHash()

        return genesisBlock
    }

    /** Create first Fuel Node registration block (Bootstrap Fuel) */
    fun createBootstrapFuelBlock(
            fuelAddress: String,
            fuelPublicKey: String,
            stake: Double = 1_000_000.0,
            previousHash: String
    ): Block {
        val fuelNodeInfo =
                FuelNodeInfo(
                        address = fuelAddress,
                        publicKey = fuelPublicKey,
                        stake = stake,
                        registeredAt = System.currentTimeMillis(),
                        isBootstrap = true
                )

        val data =
                Data(
                        reward = 0.0,
                        ticker = "KNT",
                        validator = "BOOTSTRAP",
                        transactions = emptyList(),
                        comment = "Bootstrap Fuel Node Registration",
                        type = BlockDataType.FUEL_REGISTRATION,
                        fuelNodeInfo = fuelNodeInfo
                )

        val block =
                Block(
                        version = 4,
                        index = 1,
                        previousHash = previousHash,
                        timestamp = System.currentTimeMillis(),
                        data = data,
                        validatorSignature = "",
                        hash = ""
                )

        block.hash = block.calculateHash()

        return block
    }
}
