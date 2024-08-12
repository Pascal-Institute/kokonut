import kokonut.Block
import kokonut.BlockChain
import kokonut.Utility.Companion.sendHttpPostRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

val blockChain = BlockChain()

fun main(): Unit = runBlocking{

    // 데이터를 로드하는 비동기 작업을 시작
    val job = launch {
        blockChain.loadChainFromNetwork()
    }

    // 데이터 로딩이 완료될 때까지 기다리기
    job.join()

    val newBlock : Block = blockChain.mine()
    var json = Json.encodeToJsonElement(newBlock)
    sendHttpPostRequest("http://kokonut.iptime.org:2030/addBlock", json)

}