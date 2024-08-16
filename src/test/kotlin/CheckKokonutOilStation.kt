import kokonut.Utility

fun main() {
    val url = "https://pascal-institute.github.io/kokonut-oil-station/"

    val policy = Utility.sendHttpGetPolicy(url)
    println(policy.version)
    println(policy.difficulty)
    println(policy.reward)
}