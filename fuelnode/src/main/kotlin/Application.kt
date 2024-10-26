import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kokonut.util.NodeType
import kokonut.util.Router.Companion.getGenesisBlock
import kokonut.util.Router.Companion.root

fun main() {
    embeddedServer(Netty, host = "0.0.0.0", port = 8080) {
        install(ContentNegotiation) {
            json()
        }
        routing {
            root(NodeType.FUEL)
            getGenesisBlock()
        }
    }.start(true)
}