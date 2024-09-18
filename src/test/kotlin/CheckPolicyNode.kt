import kokonut.core.BlockChain.Companion.POLICY_NODE
import kokonut.util.API.Companion.getPolicy

fun main() {
    val policy = POLICY_NODE.getPolicy()
    println(policy.version)
    println(policy.difficulty)
}