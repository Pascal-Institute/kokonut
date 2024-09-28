import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kokonut.core.BlockChain
import kokonut.util.full.Router.Companion.getChain
import kokonut.util.full.Router.Companion.getLastBlock
import kokonut.util.full.Router.Companion.getReward
import kokonut.util.full.Router.Companion.getTotalCurrencyVolume
import kokonut.util.full.Router.Companion.isValid
import kokonut.util.full.Router.Companion.startMining
import kokonut.util.full.Router.Companion.stopMining
import kokonut.util.full.Router.Companion.addBlock
import kokonut.util.full.Router.Companion.register
import kokonut.util.full.Router.Companion.root
import kokonut.util.full.Router.Companion.submit

fun main() {

    BlockChain.isRegistered()

    embeddedServer(Netty, host = "0.0.0.0", port = 8080) {
        install(ContentNegotiation) {
            json()
        }

        routing {
            root()
            register()
            submit()
            isValid()
            getLastBlock()
            getTotalCurrencyVolume()
            getReward()
            getChain()
            startMining()
            addBlock()
            stopMining()
        }
    }.start(true)
}