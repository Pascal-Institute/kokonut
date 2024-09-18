package kokonut

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kokonut.util.Utility
import kokonut.util.Utility.Companion.getLongestChainFullNode
import kokonut.util.full.FullNode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.net.URL

object URLBook{

    lateinit var fullNodes :List<FullNode>
    var FULL_NODE_0 = URL("https://www.github.com")

    val json = Json {
        ignoreUnknownKeys = true
    }
    val POLICY_NODE = URL("https://pascal-institute.github.io/kokonut-oil-station")
    val GENESIS_NODE = URL("https://api.github.com/repos/Pascal-Institute/genesis_node/contents/")
    val GENESIS_RAW_NODE = URL("https://raw.githubusercontent.com/Pascal-Institute/genesis_node/main/")
    val FUEL_NODE = URL("https://kokonut-oil.onrender.com/v1/catalog/service/knt_fullnode")

    init {
        runBlocking {
            loadFullnodeServices()
        }
        if(fullNodes.isNotEmpty()){
            FULL_NODE_0 = URL(getLongestChainFullNode().ServiceAddress)
        }
    }

    suspend fun loadFullnodeServices(): List<FullNode> {
        val client = HttpClient()
        val response: HttpResponse = client.get(FUEL_NODE)
        client.close()
        fullNodes = try{
            json.decodeFromString<List<FullNode>>(response.body())}
        catch (e : Exception){
            emptyList<FullNode>()
        }
        return  fullNodes
    }
}
