import kokonut.core.BlockChain
import kokonut.util.API.Companion.getPolicy
import kokonut.util.NodeType

fun main() {
    // Initialize with default peer for testing (Simulated as Fuel Node)
    BlockChain.initialize(NodeType.FUEL)

    val policy = BlockChain.getPrimaryFuelNode().getPolicy()
    println("Policy Version: ${policy.version}")
    println("Minimum Stake: ${policy.minimumStake} KNT")
}
