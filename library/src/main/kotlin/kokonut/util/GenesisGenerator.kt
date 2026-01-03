package kokonut.util

import kokonut.core.*

/** Genesis Block Generator Creates the first block of the blockchain with network rules */
object GenesisGenerator {

        const val GENESIS_TREASURY_MINT_AMOUNT = 1_000_000.0

        /** Create Genesis Block with network rules only (no Fuel Node list) */
        fun createGenesisBlock(
                networkId: String = "kokonut-mainnet",
                timestamp: Long = System.currentTimeMillis()
        ): Block {
                val networkRules =
                        NetworkRules(
                                networkId = networkId,
                                minFuelStake = 1_000_000.0,
                                minFullStake = 1.0,
                                fuelConsensusThreshold = 0.67,
                                maxFuelNodes = 100,
                                faucetAmount = 100.0
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
                previousHash: String,
                index: Long = 2
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
                                index = index,
                                previousHash = previousHash,
                                timestamp = System.currentTimeMillis(),
                                data = data,
                                validatorSignature = "",
                                hash = ""
                        )

                block.hash = block.calculateHash()

                return block
        }

        /**
         * Create a treasury mint block right after Genesis.
         *
         * This records an external supply injection of KNT into a treasury address.
         */
        fun createGenesisTreasuryMintBlock(
                treasuryAddress: String,
                previousHash: String,
                index: Long = 1,
                amount: Double = GENESIS_TREASURY_MINT_AMOUNT,
                timestamp: Long = System.currentTimeMillis()
        ): Block {
                // IMPORTANT: Use block timestamp for transaction to ensure hash consistency
                val mintTransaction =
                        Transaction(
                                transaction = "GENESIS_MINT",
                                sender = "GENESIS",
                                receiver = treasuryAddress,
                                remittance = amount,
                                commission = 0.0,
                                timestamp = timestamp
                        )

                val data =
                        Data(
                                reward = 0.0,
                                ticker = "KNT",
                                validator = "GENESIS",
                                transactions = listOf(mintTransaction),
                                comment = "Genesis Treasury Mint: $amount KNT -> $treasuryAddress",
                                type = BlockDataType.TRANSACTION
                        )

                val block =
                        Block(
                                index = index,
                                previousHash = previousHash,
                                timestamp = timestamp,
                                data = data,
                                validatorSignature = "",
                                hash = ""
                        )

                block.hash = block.calculateHash()
                return block
        }
}
