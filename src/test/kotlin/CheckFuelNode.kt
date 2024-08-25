import kokonut.URL.FUEL_NODE
import kokonut.util.Utility

fun main() {
    val policy = Utility.sendHttpGetPolicy(FUEL_NODE)
    println(policy.version)
    println(policy.difficulty)
}