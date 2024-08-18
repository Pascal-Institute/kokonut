import kokonut.URL.FUEL_NODE
import kokonut.Utility

suspend fun main() {

    Utility.isNodeHealthy(FUEL_NODE)

    val policy = Utility.sendHttpGetPolicy(FUEL_NODE)
    println(policy.version)
    println(policy.difficulty)
    println(policy.reward)
}