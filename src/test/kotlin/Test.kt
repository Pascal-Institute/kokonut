import kokonut.BlockChain
import kokonut.Utility.Companion.sendHttpGetRequest
import kokonut.Utility.Companion.sendHttpPostRequest

fun main(){
    //sendHttpGetRequest("http://127.0.0.1:8080/getLastBlock")

    val blockChain = BlockChain()
    println(blockChain.calculateDifficultyTarget(1))
}