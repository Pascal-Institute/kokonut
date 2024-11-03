package kokonut.util

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kokonut.Policy
import kokonut.core.Block
import kokonut.core.BlockChain
import kokonut.core.BlockChain.Companion.FUEL_NODE
import kokonut.core.BlockChain.Companion.fullNode
import kokonut.core.BlockChain.Companion.loadFullNodes
import kokonut.core.Version
import kokonut.core.Version.genesisBlockID
import kokonut.core.Version.libraryVersion
import kokonut.core.Version.protocolVersion
import kokonut.state.MiningState
import kokonut.util.API.Companion.getGenesisBlock
import kokonut.util.API.Companion.getPolicy
import kokonut.util.API.Companion.propagate
import kotlinx.html.*
import kotlinx.serialization.json.Json
import java.io.File
import java.net.InetAddress
import java.net.URL
import java.nio.file.Paths
import java.security.PublicKey


class Router {

    companion object {

        var miners: MutableSet<Miner> = mutableSetOf()

        fun Route.root(node: NodeType) {
            if(node == NodeType.FULL) {
                get("/") {
                    call.respondHtml(HttpStatusCode.OK) {
                        head {
                            title("Kokonut Full Node")
                        }
                        body {
                            h1 { +"Full Node Registration : ${BlockChain.isRegistered()}" }
                            h1 { +"Kokonut Protocol Version : $protocolVersion" }
                            h1 { +"Kokonut Library Version : $libraryVersion" }
                            h1 { +"Timestamp : ${System.currentTimeMillis()}" }
                            h1 { +"Get Chain : /getChain" }
                            h1 { +"Get Miners : /getMiners" }
                            h1 { +"Get Last Block : /getLastBlock" }
                            h1 { +"Chain Validation : /isValid" }
                            h1 { +"Get Total Currency Volume : /getTotalCurrencyVolume" }
                            h1 { +"Get Reward : /getReward?index=index" }
                        }

                    }
                }
            } else {
                get("/") {
                    call.respondHtml(HttpStatusCode.OK) {
                        head {
                            title("Kokonut Full Node")
                        }
                        body {
                            h1 { +"Kokonut protocol version : $protocolVersion" }
                            h1 { +"Kokonut library version : $libraryVersion" }
                            h1 { +"Timestamp : ${System.currentTimeMillis()}" }
                            h1 { +"Get policy : /getPolicy" }
                            h1 { +"Get genesis block : /getGenesisBlock" }
                            h1 { +"Get full nodes : /getFullNodes" }
                        }

                    }
                }
            }
        }

        fun Route.register() {
            get("/register") {
                call.respondHtml(HttpStatusCode.OK) {
                    head {
                        title { +"Service Configuration" }
                    }
                    body {
                        h1 { +"Configure Your Service" }
                        form(
                            action = "${FUEL_NODE}/submit",
                            method = FormMethod.post,
                            encType = FormEncType.multipartFormData
                        ) {

                            p {
                                label { +"Service ID: " }
                                label { +"Public Key (.pem) : " }
                                input(type = InputType.file, name = "publicKey") {
                                    placeholder = "Enter Directory your Wallet Public Key file"
                                }
                                label { +"Private Key (.pem) : " }
                                input(type = InputType.file, name = "privateKey") {
                                    placeholder = "Enter Directory your Wallet Public Key file"
                                }
                            }

                            p {
                                submitInput { value = "Submit" }
                            }
                        }
                    }
                }
            }
        }

        fun Route.isValid() {
            get("/isValid") {
                call.respondHtml(HttpStatusCode.OK) {
                    head {
                        title("Check Chain is Valid")
                    }

                    body {
                        if (BlockChain.isValid()) {
                            h1 { +"Valid" }
                        } else {
                            h1 { +"Invalid" }
                        }
                    }
                }
            }
        }

        fun Route.getFullNodes(fullNodes: List<FullNode>) {
            get("/getFullNodes") {
                call.respond(fullNodes)
            }
        }

        fun Route.getGenesisBlock() {
            get("/getGenesisBlock") {
                val classLoader = Thread.currentThread().contextClassLoader
                val inputStream = classLoader.getResourceAsStream("${genesisBlockID}.json")

                inputStream?.use { stream ->
                    val jsonString = stream.bufferedReader().use { it.readText() }
                    val genesisBlock : Block = Json.decodeFromString<Block>(jsonString)
                    call.respond(genesisBlock)
                } ?: run {
                    println("Resource not found: ${genesisBlockID}.json")
                }
            }
        }

        fun Route.getLastBlock() {
            get("/getLastBlock") {

                val lastBlock = BlockChain.getLastBlock()

                call.respondHtml(HttpStatusCode.OK) {

                    head {
                        title("Get Last Block")
                    }
                    body {
                        h1 { +"version : ${lastBlock.version}" }
                        h1 { +"index : ${lastBlock.index}" }
                        h1 { +"previousHash : ${lastBlock.previousHash}" }
                        h1 { +"timestamp : ${lastBlock.timestamp}" }
                        h1 { +"ticker : ${lastBlock.data.ticker}" }
                        h1 { +"data : ${lastBlock.data}" }
                        h1 { +"difficulty : ${lastBlock.difficulty}" }
                        h1 { +"nonce : ${lastBlock.nonce}" }
                        h1 { +"hash : ${lastBlock.hash}" }
                    }
                }
            }
        }

        fun Route.getTotalCurrencyVolume() {
            get("/getTotalCurrencyVolume") {
                call.respondHtml(HttpStatusCode.OK) {
                    head {
                        title("Get Last Block")
                    }
                    body {
                        h1 { +"Total Currency Volume : ${BlockChain.getTotalCurrencyVolume()} KNT" }
                    }
                }
            }
        }

        fun Route.getMiners() {
            get("/getMiners") {

                call.respondHtml(HttpStatusCode.OK) {
                    head {
                        title("Kokonut Full Node")
                    }
                    body {
                        h1 { +miners.toString() }
                    }

                }
            }
        }

        fun Route.getReward() {
            get("/getReward") {
                val value = call.request.queryParameters["index"]?.toLongOrNull()

                // Mock reward value for demonstration
                val reward = value?.let { Utility.setReward(it) } ?: 0.0

                call.respond(reward)
            }
        }

        fun Route.getChain() {
            get("/getChain") {
                //Check chain
                if (!BlockChain.isValid()) {
                    call.respond(HttpStatusCode.Created, "Get Chain Failed : Server block chain is invalid")
                }
                call.respond(BlockChain.getChain())
            }
        }

        fun Route.getPolicy() {
            get("/getPolicy") {
                //5 is magic number it needs to upgrade someday
                call.respond(Policy(Version.protocolVersion, 5))
            }
        }

        fun Route.startMining() {
            post("/startMining") {
                val keyPath = "/app/key"

                Utility.createDirectory(keyPath)

                val fileName = call.request.header("fileName") ?: "public_key.pem"
                val fileBytes = call.receiveStream().use { it.readBytes() }
                val publicKeyFile = File(keyPath, fileName)
                publicKeyFile.writeBytes(fileBytes)

                val miner = Utility.calculateHash(Wallet.loadPublicKey(publicKeyFile.path))

                miners.add(Miner(miner, call.request.origin.remoteHost, MiningState.READY))

                println("Miner : $miner start mining...")
                call.respond("Mining Approved...")

                miners.find {
                    it.miner == miner
                }!!.miningState = MiningState.MINING
            }
        }

        fun Route.submit(fullNodes: MutableList<FullNode>) : MutableList<FullNode> {
            var wallet : Wallet? = null
            post("/submit") {
                val keyPath = "/app/key"
                Utility.createDirectory(keyPath)
                var publicKey: File? = null
                var privateKey: File? = null
                val multipartData = call.receiveMultipart()
                val address = call.request.uri
                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {

                            if (part.name == "publicKey") {
                                val fileBytes = part.streamProvider().use { it.readBytes() }
                                publicKey = part.originalFileName?.let { it1 -> File(keyPath, it1) }
                                publicKey!!.writeBytes(fileBytes)
                                println("validation...: ${part.originalFileName}")
                            }

                            if (part.name == "privateKey") {
                                val fileBytes = part.streamProvider().use { it.readBytes() }
                                privateKey = part.originalFileName?.let { it1 -> File(keyPath, it1) }
                                privateKey!!.writeBytes(fileBytes)
                                println("validation...: ${part.originalFileName}")
                            }

                        }

                        else -> Unit
                    }
                    part.dispose()
                }

                wallet = Wallet(privateKey!!, publicKey!!)

                val data = "verify fullnode".toByteArray()
                val signatureBytes = Wallet.signData(data, wallet!!.privateKey)
                val isValid = Wallet.verifySignature(data, signatureBytes, wallet!!.publicKey)

                if (!isValid) {
                    call.respondText("Registration failed: ${HttpStatusCode.BadRequest}")
                    return@post
                } else {
                    call.respondText("Registration succeed.: ${HttpStatusCode.OK}")
                    fullNodes.add(FullNode(id = Utility.calculateHash(wallet!!.publicKey), address = address))
                }
            }
            return fullNodes
        }

        fun Route.propagate() {
            post("/propagate") {
                val size = call.request.queryParameters["size"]?.toLongOrNull()
                val id = call.request.queryParameters["id"]
                val address = call.request.queryParameters["address"]

                if (size == null || id == null || address == null) {
                    call.respond(HttpStatusCode.Created, "Propagate Failed : Missing or Invalid request parameters")
                    return@post
                }

                if (BlockChain.getChainSize() < size) {
                    BlockChain.loadChainFromFullNode(URL(address))
                    call.respond(HttpStatusCode.OK, "Propagate Succeed")
                    BlockChain.fullNodes.forEach {
                        run {
                            if (it.address != address && it.address != fullNode.address) {
                                val response = URL(it.address).propagate()
                                println(response)
                            }
                        }
                    }

                } else if (BlockChain.getChainSize() == size) {

                } else {
                    call.respond(HttpStatusCode.Created, "Propagate Failed")
                }
            }
        }

        fun Route.addBlock() {
            post("/addBlock") {

                loadFullNodes()

                val keyPath = "/app/key"
                Utility.createDirectory(keyPath)

                val policy = FUEL_NODE.getPolicy()

                if (!BlockChain.isValid()) {
                    call.respond(HttpStatusCode.Created, "Block Add Failed : Server block chain is invalid")
                }

                val multipart = call.receiveMultipart()
                var block: Block? = null
                var publicKeyFile: File? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            if (part.name == "json") {
                                block = Json.decodeFromString(Block.serializer(), part.value)
                                println("Received JSON: $block")
                            }
                        }

                        is PartData.FileItem -> {
                            if (part.name == "public_key") {
                                val fileBytes = part.streamProvider().use { it.readBytes() }
                                publicKeyFile = part.originalFileName?.let { it1 -> File(keyPath, it1) }
                                publicKeyFile!!.writeBytes(fileBytes)
                                println("Received file: ${part.originalFileName}")
                            }
                        }

                        else -> {}
                    }
                    part.dispose()
                }

                if (block != null && publicKeyFile != null) {

                    println(block)

                    val publicKey: PublicKey = Wallet.loadPublicKey(publicKeyFile!!.path)
                    val miner: String = Utility.calculateHash(publicKey)

                    if (block!!.index == BlockChain.getLastBlock().index) {
                        call.respond(HttpStatusCode.Created, "Block Already Propagated")
                    }

                    //Check Miner
                    if (block!!.data.miner != miner) {
                        call.respond(HttpStatusCode.Created, "Block Add Failed : Invalid miner")
                    }

                    //Check Index
                    if (block!!.index != BlockChain.getLastBlock().index + 1) {
                        call.respond(
                            HttpStatusCode.Created,
                            "Block Add Failed : Invalid index, New Block index : ${block!!.index} / Last Block index ${BlockChain.getLastBlock().index}"
                        )
                    }


                    //Check Version
                    if (policy.version != block!!.version) {
                        call.respond(
                            HttpStatusCode.Created,
                            "Block Add Failed : Fuel Node version ${policy.version} and Client version ${block!!.version} is different"
                        )
                    }

                    //Check Difficulty
                    if (policy.difficulty != block!!.difficulty) {
                        call.respond(
                            HttpStatusCode.Created,
                            "Block Add Failed : Fuel Node difficulty ${policy.difficulty} and Client difficulty ${block!!.difficulty} is different"
                        )
                    }

                    //Check Hash
                    val calculatedHash = block!!.calculateHash()
                    if (block!!.hash == calculatedHash) {

                        BlockChain.database.insert(block!!)

                        call.respond(
                            HttpStatusCode.Created,
                            "Block Add Succeed and Reward ${block!!.data.reward} KNT is Recorded..."
                        )

                    } else {
                        call.respond(
                            HttpStatusCode.Created,
                            "Block Add Failed : Invalid Block, calculatedHash : ${calculatedHash} blockHash : ${block!!.hash}"
                        )
                    }
                } else {
                    call.respondText("Missing block or miner public key", status = HttpStatusCode.BadRequest)
                }
                Paths.get(keyPath).toFile().deleteRecursively()
            }
        }

        fun Route.stopMining() {
            post("/stopMining") {
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