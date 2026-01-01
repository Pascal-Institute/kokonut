import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap
import kokonut.core.BlockChain
import kokonut.util.NodeType
import kokonut.util.Router.Companion.getChain
import kokonut.util.Router.Companion.getFullNodes
import kokonut.util.Router.Companion.getFullNodesFromHeartbeat
import kokonut.util.Router.Companion.getGenesisBlock
import kokonut.util.Router.Companion.getPolicy
import kokonut.util.Router.Companion.heartbeat
import kokonut.util.Router.Companion.propagate
import kokonut.util.Router.Companion.root
import kokonut.util.Router.Companion.transactionsDashboard

fun main() {
    BlockChain.initialize(NodeType.FUEL)

    // ConcurrentHashMap: address -> lastHeartbeatTimestamp
    val fullNodes = ConcurrentHashMap<String, Long>()

    // Cleanup timer: Remove nodes that haven't sent heartbeat for 15 minutes
    Timer("FullNode-Cleanup", true)
            .scheduleAtFixedRate(
                    object : TimerTask() {
                        override fun run() {
                            val now = System.currentTimeMillis()
                            val timeout = 15 * 60 * 1000 // 15 minutes

                            fullNodes.entries.removeIf { (address, lastSeen) ->
                                val isStale = (now - lastSeen) > timeout
                                if (isStale) {
                                    println(
                                            "🗑️ Removing stale Full Node: $address (inactive for 15+ minutes)"
                                    )
                                }
                                isStale
                            }
                        }
                    },
                    60_000, // Initial delay: 1 minute
                    60_000 // Period: 1 minute (check every minute)
            )

    val host = System.getenv("SERVER_HOST") ?: "0.0.0.0"
    embeddedServer(Netty, host = host, port = 80) {
                install(ContentNegotiation) { json() }
                routing {
                    root(NodeType.FUEL)
                    heartbeat(fullNodes)
                    getFullNodesFromHeartbeat(fullNodes)
                    getFullNodes(fullNodes)
                    getGenesisBlock()
                    getPolicy()
                    getChain()
                    propagate()
                    transactionsDashboard()
                }
            }
            .start(true)
}
