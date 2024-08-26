import kokonut.AddressBook.FUEL_NODE
import kokonut.util.API.Companion.sendHttpGetPolicy

fun main() {
    val policy = sendHttpGetPolicy(FUEL_NODE)
    println(policy.version)
    println(policy.difficulty)
}