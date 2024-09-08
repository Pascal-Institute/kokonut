package kokonut

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kokonut.util.full.Fullnode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.net.URL

object URLBook{

    lateinit var fullNodes :List<Fullnode>
    lateinit var FULL_NODE_0 : URL

    val json = Json {
        ignoreUnknownKeys = true
    }
    val POLICY_NODE = URL("https://pascal-institute.github.io/kokonut-oil-station")
    val FULL_STORAGE = URL("https://api.github.com/repos/Pascal-Institute/kovault/contents/")
    val FULL_RAW_STORAGE = URL("https://raw.githubusercontent.com/Pascal-Institute/kovault/main/")
    val FUEL_NODE = URL("https://kokonut-oil.onrender.com/v1/catalog/service/knt_fullnode")

    init {
        runBlocking {
            loadFullnodeServices()
        }
        FULL_NODE_0 = URL(fullNodes[0].ServiceAddress)
    }

    suspend fun loadFullnodeServices(): List<Fullnode> {
        val client = HttpClient()
        val response: HttpResponse = client.get(FUEL_NODE)
        client.close()
        fullNodes = json.decodeFromString<List<Fullnode>>(response.body())
        return fullNodes
    }
}
