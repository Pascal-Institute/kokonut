import kokonut.URL.FUEL_NODE
import kokonut.Utility
import kokonut.Utility.Companion.isNodeHealthy

fun main() {
    val policy = Utility.sendHttpGetPolicy(FUEL_NODE)
    println(policy.version)
    println(policy.difficulty)
}