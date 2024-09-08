package kokonut

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.net.URL

object URLBook{

    var fullNodes :List<Fullnode>
    val json = Json {
        ignoreUnknownKeys = true
    }
    init {
        runBlocking {
            fullNodes = getFullnodeServices()
        }


    }

    suspend fun getFullnodeServices(): List<Fullnode> {
        val client = HttpClient()
        val response: HttpResponse = client.get("https://kokonut-oil.onrender.com/v1/catalog/service/knt_fullnode")
        val fullnodeList = json.decodeFromString<List<Fullnode>>(response.body())
        client.close()
        return fullnodeList
    }

    val POLICY_NODE = URL("https://pascal-institute.github.io/kokonut-oil-station")
    val FULL_STORAGE = URL("https://api.github.com/repos/Pascal-Institute/kovault/contents/")
    val FULL_RAW_STORAGE = URL("https://raw.githubusercontent.com/Pascal-Institute/kovault/main/")
    val FULL_NODE_0 = URL(fullNodes[0].ServiceAddress)
    val FUEL_NODE = URL("https://pascal-institute.github.io/kokonut-oil-station/")
}
