import kokonut.URL.FUEL_NODE
import kokonut.util.API.Companion.sendHttpGetPolicy
import kokonut.util.Utility

fun main() {
    val policy = sendHttpGetPolicy(FUEL_NODE)
    println(policy.version)
    println(policy.difficulty)
}