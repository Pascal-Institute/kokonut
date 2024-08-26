import kokonut.URLBook.FUEL_NODE
import kokonut.util.API.Companion.getPolicy

fun main() {
    val policy = FUEL_NODE.getPolicy()
    println(policy.version)
    println(policy.difficulty)
}