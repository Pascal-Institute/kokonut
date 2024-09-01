package kokonut.util

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kokonut.Wallet
import kokonut.core.BlockChain
import kokonut.core.Identity
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.io.File


class Router {
    companion object{
        fun Route.root() {
            get("/") {
                call.respond(HttpStatusCode.OK, buildString {
                    appendHTML().html {
                        head {
                            title("Kokonut Full Node")
                        }
                        body {
                            h1 { +"Welcome to Kokonut!" }
                            h1 { +"Kokonut Protocol Version : ${Identity.protocolVersion}" }
                            h1 { +"Kokonut Library Version : ${Identity.libraryVersion}" }
                            h1 { +"Timestamp : ${System.currentTimeMillis()}"}
                            h1 { +"Get Chain : /getChain"}
                            h1 { +"Get Last Block : /getLastBlock"}
                            h1 { +"Chain Validation : /isValid"}
                            h1 { +"Get Total Currency Volume : /getTotalCurrencyVolume"}
                            h1 { +"Get Reward : /getReward?index=index"}
                        }
                    }
                })
            }
        }

        fun Route.isValid(blockchain: BlockChain) {
            get("/isValid") {
                call.respond(HttpStatusCode.OK, buildString {
                    appendHTML().html {
                        head {
                            title("Check Chain is Valid")
                        }

                        body {
                            if (blockchain.isValid()) {
                                h1 { + "Valid" }
                            } else {
                                h1 { + "Invalid" }
                            }
                        }
                    }
                })
            }
        }

        fun Route.getLastBlock(blockchain: BlockChain){
            get("/getLastBlock") {

                val lastBlock = blockchain.getLastBlock()

                call.respond(HttpStatusCode.OK, buildString {
                    appendHTML().html {
                        head {
                            title("Get Last Block")
                        }
                        body {
                            h1 { +"version : ${lastBlock.version}" }
                            h1 { +"index : ${lastBlock.index}" }
                            h1 { +"previousHash : ${lastBlock.previousHash}"}
                            h1 { +"timestamp : ${lastBlock.timestamp}"}
                            h1 { +"ticker : ${lastBlock.data.ticker}" }
                            h1 { +"data : ${lastBlock.data}" }
                            h1 { +"difficulty : ${lastBlock.difficulty}"}
                            h1 { +"nonce : ${lastBlock.nonce}"}
                            h1 { +"hash : ${lastBlock.hash}"}
                        }
                    }
                })
            }
        }

        fun Route.getTotalCurrencyVolume(blockchain: BlockChain){
            get("/getTotalCurrencyVolume") {
                call.respond(HttpStatusCode.OK, buildString {
                    appendHTML().html {
                        head {
                            title("Get Last Block")
                        }
                        body {
                            h1{+"Total Currency Volume : ${blockchain.getTotalCurrencyVolume()} KNT"}
                        }
                    }
                })
            }
        }

        fun Route.getReward() {
            get("/getReward"){
                val value = call.request.queryParameters["index"]?.toLongOrNull()

                // Mock reward value for demonstration
                val reward = value?.let { Utility.setReward(it) } ?: 0.0

                call.respond(reward)
            }
        }

        fun Route.getChain(blockchain: BlockChain) {
            get("/getChain") {
                //Check chain
                if(!blockchain.isValid()){
                    call.respond(HttpStatusCode.Created, "Get Chain Failed : Server block chain is invalid")
                }
                call.respond(blockchain.database.fetch())
            }
        }

        fun Route.startMining() {
            post("/startMining"){
                val keyPath = "/app/key"

                Utility.createDirectory(keyPath)

                val fileName = call.request.header("fileName") ?: "public_key.pem"
                val fileBytes = call.receiveStream().use { it.readBytes() }
                val publicKeyFile = File(keyPath, fileName)
                publicKeyFile.writeBytes(fileBytes)

                val miner = Utility.calculateHash(Wallet.loadPublicKey(publicKeyFile.path))

                println("Miner : $miner start mining...")

                call.respond("Mining Approved...")
            }
        }

        fun Route.addBlock(blockchain: BlockChain) {

        }

        fun Route.stopMining() {
            post("/stopMining"){
                val keyPath = "/app/key"
                Utility.createDirectory(keyPath)
                val fileName = call.request.header("fileName") ?: "public_key.pem"
                val fileBytes = call.receiveStream().use { it.readBytes() }
                val publicKeyFile = File(keyPath, fileName)
                publicKeyFile.writeBytes(fileBytes)

                val miner = Utility.calculateHash(Wallet.loadPublicKey(publicKeyFile.path))

                println("Miner : $miner stop mining...")

                call.respond("Mining Cancelled...")
            }
        }
    }
}