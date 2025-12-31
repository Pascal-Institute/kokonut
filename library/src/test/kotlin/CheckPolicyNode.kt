import kokonut.core.BlockChain
import kokonut.util.API.Companion.getPolicy

fun main() {
    // Initialize with default peer for testing
    BlockChain.initialize()

    val policy = BlockChain.getPrimaryFuelNode().getPolicy()
    println("Policy Version: ${policy.version}")
    println("Minimum Stake: ${policy.minimumStake} KNT")
}
