package kokonut.config

/**
 * Network-wide configuration for the Kokonut PoS blockchain
 * Centralizes all network parameters and environment-based configuration
 */
data class NetworkConfig(
    val treasuryAddress: String = getTreasuryAddress(),
    val stakeVaultAddress: String = getStakeVaultAddress(),
    val knownPeer: String? = System.getenv("KOKONUT_PEER"),
    val networkId: String = "kokonut-mainnet",
    val genesisMintAmount: Double = 1_000_000.0
) {
    companion object {
        private const val TREASURY_ADDRESS_ENV = "KOKONUT_TREASURY_ADDRESS"
        private const val DEFAULT_TREASURY_ADDRESS = "KOKONUT_TREASURY"
        private const val STAKE_VAULT_ADDRESS_ENV = "KOKONUT_STAKE_VAULT_ADDRESS"
        private const val DEFAULT_STAKE_VAULT_ADDRESS = "KOKONUT_STAKE_VAULT"

        fun getTreasuryAddress(): String {
            return System.getenv(TREASURY_ADDRESS_ENV)?.takeIf { it.isNotBlank() }
                ?: DEFAULT_TREASURY_ADDRESS
        }

        fun getStakeVaultAddress(): String {
            return System.getenv(STAKE_VAULT_ADDRESS_ENV)?.takeIf { it.isNotBlank() }
                ?: DEFAULT_STAKE_VAULT_ADDRESS
        }
    }
}
