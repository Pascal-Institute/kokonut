import kokonut.BlockChain
import kokonut.Utility.Companion.sendHttpGetRequest
import kokonut.Utility.Companion.sendHttpPostRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

val blockChain = BlockChain()

fun main(): Unit = runBlocking{

    // 데이터를 로드하는 비동기 작업을 시작
    val job = launch {
        blockChain.loadChainFromNetwork()
    }

    // 데이터 로딩이 완료될 때까지 기다리기
    job.join()

    blockChain.mine()
}