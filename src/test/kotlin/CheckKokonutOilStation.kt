import kokonut.URL.FUEL_NODE
import kokonut.Utility

fun main() {
    val policy = Utility.sendHttpGetPolicy(FUEL_NODE)
    println(policy.version)
    println(policy.difficulty)
    println(policy.reward)
}