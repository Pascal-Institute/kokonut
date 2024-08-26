import kokonut.URLBook.FUEL_NODE
import kokonut.util.API.Companion.sendHttpGetPolicy

fun main() {
    val policy = FUEL_NODE.sendHttpGetPolicy()
    println(policy.version)
    println(policy.difficulty)
}