package kokonut.util

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.net.URL
import java.nio.file.Paths
import java.security.PublicKey
import kokonut.Policy
import kokonut.core.Block
import kokonut.core.BlockChain
import kokonut.core.BlockChain.Companion.fullNode
import kokonut.core.BlockChain.Companion.loadFullNodes
import kokonut.core.BlockDataType
import kokonut.core.Data
import kokonut.core.FuelNodeInfo
import kokonut.state.MiningState
import kokonut.util.API.Companion.getFullNodes
import kokonut.util.API.Companion.getPolicy
import kokonut.util.API.Companion.propagate
import kokonut.util.Utility.Companion.protocolVersion
import kotlinx.html.*
import kotlinx.serialization.json.Json

class Router {

    companion object {

        var miners: MutableSet<Miner> = mutableSetOf()

        fun Route.root(node: NodeType) {
            if (node == NodeType.FULL) {
                get("/") {
                    call.respondHtml(HttpStatusCode.OK) {
                        head {
                            title("Kokonut Full Node")
                            style {
                                unsafe {
                                    raw(
                                            """
                                        body {
                                            font-family: Arial, sans-serif;
                                            max-width: 800px;
                                            margin: 50px auto;
                                            padding: 20px;
                                            background-color: #f5f5f5;
                                        }
                                        h1 {
                                            color: #333;
                                            font-size: 18px;
                                            margin: 10px 0;
                                        }
                                        .status {
                                            font-weight: bold;
                                            color: ${if (BlockChain.isRegistered()) "#28a745" else "#dc3545"};
                                        }
                                        .register-btn {
                                            background-color: #007bff;
                                            color: white;
                                            padding: 15px 30px;
                                            border: none;
                                            border-radius: 5px;
                                            font-size: 16px;
                                            cursor: pointer;
                                            margin: 20px 0;
                                            display: ${if (BlockChain.isRegistered()) "none" else "inline-block"};
                                        }
                                        .register-btn:hover {
                                            background-color: #0056b3;
                                        }
                                        .register-btn:disabled {
                                            background-color: #6c757d;
                                            cursor: not-allowed;
                                        }
                                        #message {
                                            padding: 10px;
                                            margin: 10px 0;
                                            border-radius: 5px;
                                            display: none;
                                        }
                                        .success {
                                            background-color: #d4edda;
                                            color: #155724;
                                            border: 1px solid #c3e6cb;
                                        }
                                        .error {
                                            background-color: #f8d7da;
                                            color: #721c24;
                                            border: 1px solid #f5c6cb;
                                        }
                                    """
                                    )
                                }
                            }
                        }
                        body {
                            h1 {
                                +"Full Node Registration : "
                                span("status") { +BlockChain.isRegistered().toString() }
                            }
                            button(classes = "register-btn") {
                                id = "registerBtn"
                                onClick = "registerNode()"
                                +"Register to Fuel Node"
                            }
                            div { id = "message" }
                            h1 { +"Kokonut Protocol Version : $protocolVersion" }
                            h1 { +"Timestamp : ${System.currentTimeMillis()}" }
                            h1 { +"Get Chain : /getChain" }
                            h1 { +"Get Miners : /getMiners" }
                            h1 { +"Get Last Block : /getLastBlock" }
                            h1 { +"Chain Validation : /isValid" }
                            h1 { +"Get Total Currency Volume : /getTotalCurrencyVolume" }
                            h1 { +"Get Reward : /getReward?index=index" }

                            script {
                                unsafe {
                                    raw(
                                            """
                                        function registerNode() {
                                            const btn = document.getElementById('registerBtn');
                                            const msg = document.getElementById('message');
                                            
                                            btn.disabled = true;
                                            btn.textContent = 'Registering...';
                                            
                                            window.location.href = '/register';
                                        }
                                    """
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                get("/") {
                    call.respondHtml(HttpStatusCode.OK) {
                        head { title("Kokonut Full Node") }
                        body {
                            h1 { +"Kokonut protocol version : $protocolVersion" }
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
                    head { title { +"Service Configuration" } }
                    body {
                        h1 { +"Configure Your Service" }
                        form(
                                action = "${BlockChain.getPrimaryFuelNode()}/submit",
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

                            p { submitInput { value = "Submit" } }
                        }
                    }
                }
            }
        }

        fun Route.isValid() {
            get("/isValid") {
                call.respondHtml(HttpStatusCode.OK) {
                    head { title("Check Chain is Valid") }

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
            get("/getFullNodes") { call.respond(fullNodes) }
        }

        fun Route.getGenesisBlock() {
            get("/getGenesisBlock") {
                try {
                    val genesisBlock = BlockChain.getGenesisBlock()
                    call.respond(genesisBlock)
                } catch (e: Exception) {
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            "Genesis Block not found: ${e.message}"
                    )
                }
            }
        }

        fun Route.getLastBlock() {
            get("/getLastBlock") {
                val lastBlock = BlockChain.getLastBlock()

                call.respondHtml(HttpStatusCode.OK) {
                    head { title("Get Last Block") }
                    body {
                        h1 { +"version : ${lastBlock.version}" }
                        h1 { +"index : ${lastBlock.index}" }
                        h1 { +"previousHash : ${lastBlock.previousHash}" }
                        h1 { +"timestamp : ${lastBlock.timestamp}" }
                        h1 { +"ticker : ${lastBlock.data.ticker}" }
                        h1 { +"data : ${lastBlock.data}" }
                        h1 { +"validatorSignature : ${lastBlock.validatorSignature}" }
                        h1 { +"hash : ${lastBlock.hash}" }
                    }
                }
            }
        }

        fun Route.getTotalCurrencyVolume() {
            get("/getTotalCurrencyVolume") {
                call.respondHtml(HttpStatusCode.OK) {
                    head { title("Get Last Block") }
                    body {
                        h1 { +"Total Currency Volume : ${BlockChain.getTotalCurrencyVolume()} KNT" }
                    }
                }
            }
        }

        fun Route.getMiners() {
            get("/getMiners") {
                call.respondHtml(HttpStatusCode.OK) {
                    head { title("Kokonut Full Node") }
                    body { h1 { +miners.toString() } }
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
                // Check chain
                if (!BlockChain.isValid()) {
                    call.respond(
                            HttpStatusCode.Created,
                            "Get Chain Failed : Server block chain is invalid"
                    )
                }
                call.respond(BlockChain.getChain())
            }
        }

        fun Route.getPolicy() {
            get("/getPolicy") {
                // Default minimum stake is 100.0 KNT
                call.respond(Policy(protocolVersion, 100.0))
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

                miners.find { it.miner == miner }!!.miningState = MiningState.MINING
            }
        }

        fun Route.submit(fullNodes: MutableList<FullNode>): MutableList<FullNode> {
            var wallet: Wallet? = null

            post("/submit") {
                val keyPath = "/app/key"
                Utility.createDirectory(keyPath)

                var publicKey: File? = null
                var privateKey: File? = null
                val multipartData = call.receiveMultipart()
                val address: String =
                        call.request.headers["Origin"]
                                ?: run {
                                    call.respondText(
                                            "Missing 'Origin' header",
                                            status = HttpStatusCode.BadRequest
                                    )
                                    return@post
                                }

                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            val fileBytes = part.streamProvider().use { it.readBytes() }
                            when (part.name) {
                                "publicKey" -> {
                                    publicKey =
                                            File(keyPath, part.originalFileName ?: "publicKey.pem")
                                                    .apply { writeBytes(fileBytes) }
                                    println("Uploaded public key: ${part.originalFileName}")
                                }
                                "privateKey" -> {
                                    privateKey =
                                            File(keyPath, part.originalFileName ?: "privateKey.pem")
                                                    .apply { writeBytes(fileBytes) }
                                    println("Uploaded private key: ${part.originalFileName}")
                                }
                            }
                        }
                        else -> Unit
                    }
                    part.dispose()
                }

                if (publicKey == null || privateKey == null) {
                    call.respondText(
                            "Missing keys in the request",
                            status = HttpStatusCode.BadRequest
                    )
                    return@post
                }

                try {
                    wallet = Wallet(privateKey!!, publicKey!!)

                    val data = "verify fullnode".toByteArray()
                    val signatureBytes = Wallet.signData(data, wallet!!.privateKey)
                    val isValid = Wallet.verifySignature(data, signatureBytes, wallet!!.publicKey)
                    val id = Utility.calculateHash(wallet!!.publicKey)

                    // Check if the node ID is already present
                    val nodeExists =
                            BlockChain.getRandomFuelNode().getFullNodes().any { it.id == id }
                    if (!isValid || nodeExists) {
                        call.respondText("Registration failed: ${HttpStatusCode.BadRequest}")
                        return@post
                    }

                    call.respondText("Registration succeeded: ${HttpStatusCode.OK}")
                    fullNodes.add(FullNode(id = id, address = address))
                } catch (e: Exception) {
                    call.respondText(
                            "An error occurred: ${e.message}",
                            status = HttpStatusCode.InternalServerError
                    )
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
                    call.respond(
                            HttpStatusCode.Created,
                            "Propagate Failed : Missing or Invalid request parameters"
                    )
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
                } else if (BlockChain.getChainSize() == size) {} else {

                    call.respond(HttpStatusCode.Created, "Propagate Failed")
                }
            }
        }

        fun Route.addBlock() {
            post("/addBlock") {
                loadFullNodes()

                val keyPath = "/app/key"
                Utility.createDirectory(keyPath)

                val policy = BlockChain.getPrimaryFuelNode().getPolicy()

                if (!BlockChain.isValid()) {
                    call.respond(
                            HttpStatusCode.Created,
                            "Block Add Failed : Server block chain is invalid"
                    )
                }

                val multipart = call.receiveMultipart()
                var block: Block? = null
                var publicKeyFile: File? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            if (part.name == "json") {
                                block = Json.decodeFromString<Block>(part.value)
                                println("Received JSON: $block")
                            }
                        }
                        is PartData.FileItem -> {
                            if (part.name == "public_key") {
                                val fileBytes = part.streamProvider().use { it.readBytes() }
                                publicKeyFile =
                                        part.originalFileName?.let { it1 -> File(keyPath, it1) }
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
                    val validator: String = Utility.calculateHash(publicKey)

                    if (block!!.index == BlockChain.getLastBlock().index) {
                        call.respond(HttpStatusCode.Created, "Block Already Propagated")
                    }

                    // Check Validator
                    if (block!!.data.validator != validator) {
                        call.respond(HttpStatusCode.Created, "Block Add Failed : Invalid validator")
                    }

                    // Check Index
                    if (block!!.index != BlockChain.getLastBlock().index + 1) {
                        call.respond(
                                HttpStatusCode.Created,
                                "Block Add Failed : Invalid index, New Block index : ${block!!.index} / Last Block index ${BlockChain.getLastBlock().index}"
                        )
                    }

                    // Check Version
                    if (policy.version != block!!.version) {
                        call.respond(
                                HttpStatusCode.Created,
                                "Block Add Failed : Fuel Node version ${policy.version} and Client version ${block!!.version} is different"
                        )
                    }

                    // Check Hash
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
                    call.respondText(
                            "Missing block or miner public key",
                            status = HttpStatusCode.BadRequest
                    )
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

        /** Get Fuel Nodes from blockchain scan */
        fun Route.getFuelNodesRoute() {
            get("/getFuelNodes") {
                val fuelNodes = BlockChain.getFuelNodes()
                call.respond(fuelNodes)
            }
        }

        /** Get Network Rules from Genesis Block */
        fun Route.getNetworkRules() {
            get("/getNetworkRules") {
                val rules = BlockChain.getNetworkRules()
                call.respond(rules)
            }
        }

        /** Register new Fuel Node (requires consensus from existing Fuel Nodes) */
        fun Route.registerFuelNode() {
            post("/registerFuelNode") {
                val request = call.receive<FuelNodeInfo>()

                // Verify stake requirement
                val rules = BlockChain.getNetworkRules()
                if (request.stake < rules.minFuelStake) {
                    call.respond(
                            HttpStatusCode.BadRequest,
                            "Insufficient stake. Required: ${rules.minFuelStake} KNT"
                    )
                    return@post
                }

                // Check max Fuel Nodes limit
                val currentFuels = BlockChain.getFuelNodes()
                if (currentFuels.size >= rules.maxFuelNodes) {
                    call.respond(
                            HttpStatusCode.BadRequest,
                            "Maximum Fuel Nodes limit reached: ${rules.maxFuelNodes}"
                    )
                    return@post
                }

                // Create registration block
                val lastBlock = BlockChain.getLastBlock()
                val registrationData =
                        Data(
                                reward = 0.0,
                                ticker = "KNT",
                                validator = "FUEL_REGISTRY",
                                transactions = emptyList(),
                                comment = "Fuel Node Registration: ${request.address}",
                                type = BlockDataType.FUEL_REGISTRATION,
                                fuelNodeInfo = request
                        )

                val registrationBlock =
                        Block(
                                version = protocolVersion,
                                index = lastBlock.index + 1,
                                previousHash = lastBlock.hash,
                                timestamp = System.currentTimeMillis(),
                                data = registrationData,
                                validatorSignature = "",
                                hash = ""
                        )

                registrationBlock.hash = registrationBlock.calculateHash()

                // Add to blockchain
                BlockChain.database.insert(registrationBlock)
                BlockChain.scanFuelNodes() // Refresh cache

                call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                                "status" to "success",
                                "message" to "Fuel Node registered successfully",
                                "blockIndex" to registrationBlock.index
                        )
                )
            }
        }
    }
}
