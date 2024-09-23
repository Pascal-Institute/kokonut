package kokonut.util.full

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kokonut.util.Wallet
import kokonut.core.Block
import kokonut.core.BlockChain
import kokonut.core.BlockChain.POLICY_NODE
import kokonut.core.Version.libraryVersion
import kokonut.core.Version.protocolVersion
import kokonut.state.MiningState
import kokonut.util.API.Companion.addBlock
import kokonut.util.API.Companion.getPolicy
import kokonut.util.API.Companion.propagate
import kokonut.util.Miner
import kokonut.util.Utility
import kotlinx.html.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File
import java.net.URL
import java.nio.file.Paths
import java.security.PublicKey


class Router {

    companion object {

        var fullNode = FullNode("", "", "", Weights(0, 0))
        var miners: MutableSet<Miner> = mutableSetOf()

        fun Route.root() {
            get("/") {
                call.respondHtml(HttpStatusCode.OK) {
                    head {
                        title("Kokonut Full Node")
                    }
                    body {
                        h1 { +"Welcome!" }
                        h1 { +"Full Node Registration : ${BlockChain.isRegistered(fullNode)}" }
                        h1 { +"Kokonut Protocol Version : ${protocolVersion}" }
                        h1 { +"Kokonut Library Version : ${libraryVersion}" }
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
                            action = "/submit",
                            method = FormMethod.post,
                            encType = FormEncType.multipartFormData
                        ) {
                            p {
                                label { +"Service Name: " }
                                textInput(name = "serviceName") {
                                    readonly = true
                                    value = "knt_fullnode"
                                }
                            }
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
                                label { +"Service Address: " }
                                textInput(name = "serviceAddress") {
                                    placeholder = "Enter Service Domain or IP Address"
                                }
                            }
                            p {
                                label { +"Service Port: " }
                                textInput(name = "servicePort") {
                                    placeholder = "8080"
                                    value = "8080"
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

        fun Route.submit() {
            post("/submit") {
                val keyPath = "/app/key"
                Utility.createDirectory(keyPath)
                val multipartData = call.receiveMultipart()
                var serviceName = "knt_fullnode"
                var serviceAddress = ""
                var servicePort = ""
                var publicKey: File? = null
                var privateKey: File? = null

                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            if (part.name == "serviceName") {
                                serviceName = part.value
                            }

                            if (part.name == "serviceAddress") {
                                serviceAddress = part.value
                            }

                            if (part.name == "servicePort") {
                                servicePort = part.value
                            }
                        }

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

                val wallet = Wallet(privateKey!!, publicKey!!)

                val data = "verify operator".toByteArray()
                val signatureBytes = Wallet.signData(data, wallet.privateKey)
                val isValid = Wallet.verifySignature(data, signatureBytes, wallet.publicKey)

                if (!isValid) {
                    call.respondText("Invalid Wallet")
                    return@post
                }

                val serviceRegData = ServiceRegData(
                    Name = serviceName,
                    ID = Utility.calculateHash(wallet.publicKey),
                    Address = serviceAddress,
                    Port = servicePort.toInt(),
                    Check = HealthCheck(
                        HTTP = serviceAddress,
                        Interval = "300s",
                        Timeout = "30s",
                        DeregisterCriticalServiceAfter = "10m"
                    )
                )

                val client = HttpClient(CIO) {
                    install(ContentNegotiation) {
                        json(Json { prettyPrint = true })
                    }
                }

                val requestBody = Json.encodeToString(ServiceRegData.serializer(), serviceRegData)
                println("Request Body: $requestBody")

                val response: HttpResponse =
                    client.put("https://kokonut-oil.onrender.com/v1/agent/service/register") {
                        contentType(ContentType.Application.Json)
                        setBody(serviceRegData)
                    }

                println("Response Status: ${response.status}")
                println("Response Body: $response")

                if (response.status == HttpStatusCode.OK) {
                    fullNode = FullNode(
                        serviceRegData.ID,
                        serviceRegData.Name,
                        serviceRegData.Address,
                        Weights(1, 1)
                    )
                }

                Utility.createDirectory(keyPath)

                File(keyPath).deleteRecursively()

                call.respondText("Configuration Registration Successfully: ${response.status}")

                client.close()
            }
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
                            if (it.ServiceAddress != address && it.ServiceAddress != fullNode.ServiceAddress) {
                                val response = URL(it.ServiceAddress).propagate()
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

                val keyPath = "/app/key"
                Utility.createDirectory(keyPath)

                val policy = POLICY_NODE.getPolicy()

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

                    //Check Miner
                    if (block!!.data.miner != miner) {
                        call.respond(HttpStatusCode.Created, "Block Add Failed : Invalid miner")
                        return@post
                    }

                    //Check Index
                    if (block!!.index != BlockChain.getLastBlock().index + 1) {
                        call.respond(
                            HttpStatusCode.Created,
                            "Block Add Failed : Invalid index, New Block index : ${block!!.index} / Last Block index ${BlockChain.getLastBlock().index}"
                        )
                        return@post
                    }


                    //Check Version
                    if (policy.version != block!!.version) {
                        call.respond(
                            HttpStatusCode.Created,
                            "Block Add Failed : Fuel Node version ${policy.version} and Client version ${block!!.version} is different"
                        )
                        return@post
                    }

                    //Check Difficulty
                    if (policy.difficulty != block!!.difficulty) {
                        call.respond(
                            HttpStatusCode.Created,
                            "Block Add Failed : Fuel Node difficulty ${policy.difficulty} and Client difficulty ${block!!.difficulty} is different"
                        )
                        return@post
                    }

                    //Check Hash
                    val calculatedHash = block!!.calculateHash()
                    if (block!!.hash == calculatedHash) {

                        BlockChain.database.insert(block!!)

                        //propagate...
                        BlockChain.fullNodes.forEach {
                            if (it.ServiceAddress != fullNode.ServiceAddress) {
                                try {
                                    URL(it.ServiceAddress).addBlock(Json.encodeToJsonElement(block), publicKeyFile!!)
                                } catch (e: Exception) {
                                    println("Propagation Failed at ${it.ServiceAddress} : $e")
                                }
                            }
                        }



                        call.respond(
                            HttpStatusCode.Created,
                            "Block Add Succeed and Reward ${block!!.data.reward} KNT is Recorded..."
                        )

                    } else {
                        call.respond(
                            HttpStatusCode.Created,
                            "Block Add Failed : Invalid Block, calculatedHash : ${calculatedHash} blockHash : ${block!!.hash}"
                        )
                        return@post
                    }
                } else {
                    call.respondText("Missing block or miner public key", status = HttpStatusCode.BadRequest)
                    return@post
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