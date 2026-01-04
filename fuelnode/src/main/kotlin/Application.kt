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

/** Constants for node health management */
private object NodeHealthConfig {
    const val STALE_NODE_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes
    const val CLEANUP_INTERVAL_MS = 60_000L // 1 minute
    const val CLEANUP_INITIAL_DELAY_MS = 60_000L // 1 minute
    const val DEFAULT_SERVER_HOST = "0.0.0.0"
    const val DEFAULT_SERVER_PORT = 80
}

fun main() {
    BlockChain.initialize(NodeType.FUEL)

    // ConcurrentHashMap: address -> lastHeartbeatTimestamp
    val fullNodes = ConcurrentHashMap<String, Long>()

    // Cleanup timer: Remove nodes that haven't sent heartbeat within timeout period
    Timer("FullNode-Cleanup", true)
            .scheduleAtFixedRate(
                    object : TimerTask() {
                        override fun run() {
                            val now = System.currentTimeMillis()
                            fullNodes.entries.removeIf { (address, lastSeen) ->
                                val isStale = (now - lastSeen) > NodeHealthConfig.STALE_NODE_TIMEOUT_MS
                                if (isStale) {
                                    println("🗑️ Removing stale Full Node: $address (inactive)")
                                }
                                isStale
                            }
                        }
                    },
                    NodeHealthConfig.CLEANUP_INITIAL_DELAY_MS,
                    NodeHealthConfig.CLEANUP_INTERVAL_MS
            )

    val host = System.getenv("SERVER_HOST") ?: NodeHealthConfig.DEFAULT_SERVER_HOST
    embeddedServer(Netty, host = host, port = NodeHealthConfig.DEFAULT_SERVER_PORT) {
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
                }
            }
            .start(true)
}
