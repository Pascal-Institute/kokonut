import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kokonut.util.FullNode
import kokonut.util.NodeType
import kokonut.util.Router.Companion.getFullNodes
import kokonut.util.Router.Companion.getGenesisBlock
import kokonut.util.Router.Companion.root
import kokonut.util.Router.Companion.submit

fun main() {

    var fullNodes = mutableListOf<FullNode>()

    embeddedServer(Netty, host = "0.0.0.0", port = 8080) {
        install(ContentNegotiation) {
            json()
        }
        routing {
            root(NodeType.FUEL)
            fullNodes = submit(fullNodes)
            getGenesisBlock()
            getFullNodes(fullNodes)
        }
    }.start(true)
}