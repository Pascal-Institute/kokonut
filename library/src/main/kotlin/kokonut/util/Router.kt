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
import kokonut.core.Transaction
import kokonut.core.ValidatorOnboardingInfo
import kokonut.state.ValidatorState
import kokonut.util.API.Companion.getPolicy
import kokonut.util.API.Companion.propagate
import kokonut.util.Utility.Companion.protocolVersion
import kotlinx.html.*
import kotlinx.serialization.json.Json

class Router {

    companion object {

        var validatorSessions: MutableSet<ValidatorSession> = mutableSetOf()
        private val onboardingLock = Any()

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
                                        .status-box {
                                            background-color: #e7f3ff;
                                            border-left: 4px solid #007bff;
                                            padding: 15px;
                                            margin: 20px 0;
                                            border-radius: 5px;
                                        }
                                        .status-box h2 {
                                            margin: 0 0 10px 0;
                                            color: #007bff;
                                            font-size: 16px;
                                        }
                                        .status-box p {
                                            margin: 5px 0;
                                            font-size: 14px;
                                        }
                                    """
                                    )
                                }
                            }
                        }
                        body {
                            h1 { +"🥥 Kokonut Full Node" }

                            div(classes = "status-box") {
                                h2 { +"🔄 Automatic Registration Status" }
                                p { +"✅ Heartbeat-based registration is active" }
                                p { +"💓 Sending heartbeat every 10 minutes" }
                                p { +"🌐 Peer: ${BlockChain.knownPeer ?: "Not configured"}" }
                            }

                            h1 { +"Kokonut Protocol Version : $protocolVersion" }
                            h1 { +"Timestamp : ${System.currentTimeMillis()}" }
                            h1 { +"Get Chain : /getChain" }
                            h1 { +"Get Connected Validators : /getConnectedValidators" }
                            h1 { +"Get Active Validators : /getValidators" }
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

        fun Route.getFullNodes(fullNodes: MutableMap<String, Long>) {
            get("/getFullNodes") {
                call.respondHtml(HttpStatusCode.OK) {
                    head {
                        title("Registered Full Nodes")
                        style {
                            unsafe {
                                raw(
                                        """
                                    body {
                                        font-family: Arial, sans-serif;
                                        max-width: 1000px;
                                        margin: 50px auto;
                                        padding: 20px;
                                        background-color: #f5f5f5;
                                    }
                                    h1 {
                                        color: #333;
                                        border-bottom: 3px solid #28a745;
                                        padding-bottom: 10px;
                                    }
                                    .summary {
                                        background-color: #d4edda;
                                        border-left: 4px solid #28a745;
                                        padding: 15px;
                                        margin: 20px 0;
                                        border-radius: 5px;
                                    }
                                    table {
                                        width: 100%;
                                        border-collapse: collapse;
                                        background-color: white;
                                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                                        margin-top: 20px;
                                    }
                                    th {
                                        background-color: #28a745;
                                        color: white;
                                        padding: 12px;
                                        text-align: left;
                                        font-weight: bold;
                                    }
                                    td {
                                        padding: 10px;
                                        border-bottom: 1px solid #ddd;
                                    }
                                    tr:hover {
                                        background-color: #f5f5f5;
                                    }
                                    .node-id {
                                        font-family: monospace;
                                        font-size: 12px;
                                        color: #555;
                                    }
                                    .node-address {
                                        color: #007bff;
                                        font-weight: 500;
                                    }
                                    .no-nodes {
                                        text-align: center;
                                        padding: 40px;
                                        color: #999;
                                        font-style: italic;
                                    }
                                    .online-badge {
                                        background-color: #28a745;
                                        color: white;
                                        padding: 3px 8px;
                                        border-radius: 4px;
                                        font-size: 12px;
                                        font-weight: bold;
                                    }
                                    .last-seen {
                                        font-size: 11px;
                                        color: #666;
                                    }
                                """
                                )
                            }
                        }
                    }
                    body {
                        h1 { +"🥥 Registered Full Nodes" }

                        div(classes = "summary") {
                            h2 { +"Summary" }
                            p { +"Total Active Full Nodes: ${fullNodes.size}" }
                            p { +"Network: Kokonut Blockchain" }
                            p {
                                +"Last Updated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}"
                            }
                        }

                        if (fullNodes.isEmpty()) {
                            div(classes = "no-nodes") {
                                +"No Full Nodes registered yet. Full Nodes will appear here after heartbeat registration."
                            }
                        } else {
                            table {
                                thead {
                                    tr {
                                        th { +"#" }
                                        th { +"Node ID" }
                                        th { +"Address" }
                                        th { +"Last Heartbeat" }
                                        th { +"Status" }
                                    }
                                }
                                tbody {
                                    fullNodes.entries.sortedBy { it.key }.forEachIndexed {
                                            index,
                                            (address, lastSeen) ->
                                        val timeDiff = System.currentTimeMillis() - lastSeen
                                        val minutesAgo = timeDiff / 60000

                                        tr {
                                            td { +"${index + 1}" }
                                            td {
                                                span(classes = "node-id") {
                                                    +address.split("//").last()
                                                }
                                            }
                                            td { span(classes = "node-address") { +address } }
                                            td {
                                                span(classes = "last-seen") {
                                                    +"${minutesAgo}m ago"
                                                }
                                            }
                                            td { span(classes = "online-badge") { +"🟢 Online" } }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
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

        fun Route.getConnectedValidators() {
            get("/getConnectedValidators") {
                call.respondHtml(HttpStatusCode.OK) {
                    head { title("Kokonut Full Node") }
                    body { h1 { +validatorSessions.toString() } }
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

        fun Route.getValidators() {
            get("/getValidators") {
                val validators = BlockChain.validatorPool.getActiveValidators()

                call.respondHtml(HttpStatusCode.OK) {
                    head {
                        title("Active Validators")
                        style {
                            unsafe {
                                raw(
                                        """
                                    body {
                                        font-family: Arial, sans-serif;
                                        max-width: 1000px;
                                        margin: 50px auto;
                                        padding: 20px;
                                        background-color: #f5f5f5;
                                    }
                                    h1 {
                                        color: #333;
                                        border-bottom: 3px solid #007bff;
                                        padding-bottom: 10px;
                                    }
                                    .summary {
                                        background-color: #e7f3ff;
                                        border-left: 4px solid #007bff;
                                        padding: 15px;
                                        margin: 20px 0;
                                        border-radius: 5px;
                                    }
                                    table {
                                        width: 100%;
                                        border-collapse: collapse;
                                        background-color: white;
                                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                                        margin-top: 20px;
                                    }
                                    th {
                                        background-color: #007bff;
                                        color: white;
                                        padding: 12px;
                                        text-align: left;
                                        font-weight: bold;
                                    }
                                    td {
                                        padding: 10px;
                                        border-bottom: 1px solid #ddd;
                                    }
                                    tr:hover {
                                        background-color: #f5f5f5;
                                    }
                                    .validator-id {
                                        font-family: monospace;
                                        font-size: 12px;
                                        color: #555;
                                    }
                                    .no-validators {
                                        text-align: center;
                                        padding: 40px;
                                        color: #999;
                                        font-style: italic;
                                    }
                                """
                                )
                            }
                        }
                    }
                    body {
                        h1 { +"🥥 Active Validators" }

                        div(classes = "summary") {
                            h2 { +"Summary" }
                            p { +"Total Active Validators: ${validators.size}" }
                            p { +"Total Staked: ${BlockChain.validatorPool.getTotalStaked()} KNT" }
                            p { +"Minimum Stake Required: ${ValidatorPool.MINIMUM_STAKE} KNT" }
                        }

                        if (validators.isEmpty()) {
                            div(classes = "no-validators") {
                                +"No active validators found. Stake KNT to become a validator!"
                            }
                        } else {
                            table {
                                thead {
                                    tr {
                                        th { +"#" }
                                        th { +"Validator ID (Address)" }
                                        th { +"Staked Amount" }
                                        th { +"Blocks Validated" }
                                        th { +"Total Rewards" }
                                        th { +"Status" }
                                    }
                                }
                                tbody {
                                    validators
                                            .sortedByDescending { it.stakedAmount }
                                            .forEachIndexed { index, validator ->
                                                tr {
                                                    td { +"${index + 1}" }
                                                    td {
                                                        span(classes = "validator-id") {
                                                            +validator.address
                                                        }
                                                    }
                                                    td { +"${validator.stakedAmount} KNT" }
                                                    td { +"${validator.blocksValidated}" }
                                                    td { +"${validator.rewardsEarned} KNT" }
                                                    td {
                                                        if (validator.isActive) {
                                                            +"✅ Active"
                                                        } else {
                                                            +"❌ Inactive"
                                                        }
                                                    }
                                                }
                                            }
                                }
                            }
                        }
                    }
                }
            }
        }

        /**
         * Handshake endpoint for Light Node connection ritual Returns network information for
         * verification
         */
        fun Route.handshake() {
            post("/handshake") {
                try {
                    val request = call.receive<kokonut.core.HandshakeRequest>()

                    println(
                            "🤝 Handshake request from ${request.nodeType} node (v${request.clientVersion})"
                    )

                    // Verify public key is provided
                    if (request.publicKey.isBlank()) {
                        println("❌ Handshake rejected: Public key is required")
                        call.respond(
                                HttpStatusCode.BadRequest,
                                kokonut.core.HandshakeResponse(
                                        success = false,
                                        message =
                                                "Authentication failed: Public key is required for handshake"
                                )
                        )
                        return@post
                    }

                    // Verify protocol compatibility
                    if (request.clientVersion != protocolVersion) {
                        call.respond(
                                HttpStatusCode.OK,
                                kokonut.core.HandshakeResponse(
                                        success = false,
                                        message =
                                                "Protocol version mismatch. Server: $protocolVersion, Client: ${request.clientVersion}"
                                )
                        )
                        return@post
                    }

                    // Gather network information
                    val genesisBlock = BlockChain.getGenesisBlock()
                    val networkRules = BlockChain.getNetworkRules()
                    val fuelNodes = BlockChain.getFuelNodes()

                    val networkInfo =
                            kokonut.core.NetworkInfo(
                                    nodeType = "FULL",
                                    networkId = networkRules.networkId,
                                    genesisHash = genesisBlock.hash,
                                    chainSize = BlockChain.getChainSize(),
                                    protocolVersion = protocolVersion,
                                    totalValidators =
                                            BlockChain.validatorPool.getActiveValidators().size,
                                    totalCurrencyVolume = BlockChain.getTotalCurrencyVolume(),
                                    connectedFuelNodes = fuelNodes.size
                            )

                    println("✅ Handshake successful with ${request.nodeType} node")
                    println("   Client Public Key: ${request.publicKey.take(32)}...")

                    call.respond(
                            HttpStatusCode.OK,
                            kokonut.core.HandshakeResponse(
                                    success = true,
                                    message =
                                            "Handshake successful. Welcome to ${networkRules.networkId}!",
                                    networkInfo = networkInfo
                            )
                    )
                } catch (e: Exception) {
                    println("❌ Handshake failed: ${e.message}")
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            kokonut.core.HandshakeResponse(
                                    success = false,
                                    message = "Handshake failed: ${e.message}"
                            )
                    )
                }
            }
        }

        fun Route.startValidating() {
            post("/startValidating") {
                val keyPath = "/app/key"

                Utility.createDirectory(keyPath)

                val fileName = call.request.header("fileName") ?: "public_key.pem"
                val fileBytes = call.receiveStream().use { it.readBytes() }
                val publicKeyFile = File(keyPath, fileName)
                publicKeyFile.writeBytes(fileBytes)

                val validatorAddress =
                        Utility.calculateHash(Wallet.loadPublicKey(publicKeyFile.path))

                // 1-time validator onboarding reward (connectivity incentive)
                // - Withdraw 2 KNT from Fuel Node treasury
                // - Reward 1 KNT to this Full Node
                // - Reward 1 KNT to this Validator (Light Node wallet address)
                synchronized(onboardingLock) {
                    val alreadyOnboarded =
                        BlockChain.getChain().any { block ->
                        block.data.type == BlockDataType.VALIDATOR_ONBOARDING &&
                            block.data.validatorOnboardingInfo?.validatorAddress ==
                                validatorAddress
                        }

                    if (!alreadyOnboarded) {
                    val fuelNodeAddress =
                        try {
                            BlockChain.getPrimaryFuelNode().toString()
                        } catch (e: Exception) {
                            "FUELNODE"
                        }

                    val fullNodeAddress =
                        (System.getenv("KOKONUT_FULLNODE_REWARD_RECEIVER")
                                ?.takeIf { it.isNotBlank() }
                                ?: run {
                                    val scheme = call.request.origin.scheme
                                    val host = call.request.host()
                                    val port = call.request.port()
                                    "$scheme://$host:$port"
                                })

                    val onboardingInfo =
                        ValidatorOnboardingInfo(
                            validatorAddress = validatorAddress,
                            fullNodeAddress = fullNodeAddress,
                            fuelNodeAddress = fuelNodeAddress,
                            amountToFullNode = 1.0,
                            amountToValidator = 1.0,
                            totalWithdrawn = 2.0
                        )

                    val transactions =
                        listOf(
                            Transaction(
                                transaction = "VALIDATOR_ONBOARDING_FULLNODE",
                                sender = fuelNodeAddress,
                                receiver = fullNodeAddress,
                                remittance = 1.0,
                                commission = 0.0
                            ),
                            Transaction(
                                transaction = "VALIDATOR_ONBOARDING_VALIDATOR",
                                sender = fuelNodeAddress,
                                receiver = validatorAddress,
                                remittance = 1.0,
                                commission = 0.0
                            )
                        )

                    val lastBlock = BlockChain.getLastBlock()
                    val onboardingData =
                        Data(
                            reward = 0.0,
                            ticker = "KNT",
                            validator = "ONBOARDING",
                            transactions = transactions,
                            comment = "Validator onboarding: $validatorAddress",
                            type = BlockDataType.VALIDATOR_ONBOARDING,
                            validatorOnboardingInfo = onboardingInfo
                        )

                    val onboardingBlock =
                        Block(
                            version = protocolVersion,
                            index = lastBlock.index + 1,
                            previousHash = lastBlock.hash,
                            timestamp = System.currentTimeMillis(),
                            data = onboardingData,
                            validatorSignature = "",
                            hash = ""
                        )

                    onboardingBlock.hash = onboardingBlock.calculateHash()
                    BlockChain.database.insert(onboardingBlock)
                    BlockChain.refreshFromDatabase()

                    println(
                        "🎁 Validator onboarding completed: withdrew 2 KNT from $fuelNodeAddress, " +
                            "paid 1 KNT to Full Node ($fullNodeAddress) and 1 KNT to Validator ($validatorAddress)"
                    )
                    }
                }

                validatorSessions.add(
                        ValidatorSession(
                                validatorAddress,
                                call.request.origin.remoteHost,
                                ValidatorState.READY
                        )
                )

                println("Validator : $validatorAddress start validating...")
                call.respond("Validating Approved...")

                validatorSessions.find { it.validatorAddress == validatorAddress }!!
                        .validationState = ValidatorState.VALIDATING
            }
        }

        /**
         * Heartbeat endpoint for automatic Full Node registration Full Nodes send heartbeat every
         * 10 minutes to maintain registration Nodes that don't send heartbeat for 15 minutes are
         * automatically removed
         */
        fun Route.heartbeat(fullNodes: MutableMap<String, Long>) {
            post("/heartbeat") {
                try {
                    @kotlinx.serialization.Serializable
                    data class HeartbeatRequest(val address: String)

                    val request = call.receive<HeartbeatRequest>()
                    val normalizedAddress =
                            Utility.normalizeNodeAddress(
                                    address = request.address,
                                    remoteHost = call.request.origin.remoteHost,
                                    forwardedForHeader = call.request.headers["X-Forwarded-For"]
                            )
                    val now = System.currentTimeMillis()

                    // Update or add node with current timestamp
                    val isNewNode = !fullNodes.containsKey(normalizedAddress)
                    fullNodes[normalizedAddress] = now

                    if (isNewNode) {
                        println("✅ New Full Node registered: $normalizedAddress")
                    } else {
                        println("💓 Heartbeat received from: $normalizedAddress")
                    }

                    call.respond(
                            HttpStatusCode.OK,
                            mapOf(
                                    "status" to "success",
                                    "message" to
                                            if (isNewNode) "Registered successfully"
                                            else "Heartbeat received",
                                    "timestamp" to now
                            )
                    )
                } catch (e: Exception) {
                    println("❌ Heartbeat error: ${e.message}")
                    call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                    "status" to "error",
                                    "message" to "Invalid heartbeat request: ${e.message}"
                            )
                    )
                }
            }
        }

        /**
         * Get active Full Nodes (for backward compatibility) Converts heartbeat map to FullNode
         * list
         */
        fun Route.getFullNodesFromHeartbeat(fullNodes: MutableMap<String, Long>) {
            get("/getFullNodes") {
                val activeNodes =
                        fullNodes.map { (address, _) ->
                            FullNode(
                                    id = Utility.calculateHash(address.hashCode().toLong()),
                                    address = address
                            )
                        }
                call.respond(activeNodes)
            }
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
                        BlockChain.refreshFromDatabase()

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
                            "Missing block or validator public key",
                            status = HttpStatusCode.BadRequest
                    )
                }
                Paths.get(keyPath).toFile().deleteRecursively()
            }
        }

        fun Route.stopValidating() {
            post("/stopValidating") {
                val keyPath = "/app/key"
                Utility.createDirectory(keyPath)
                val fileName = call.request.header("fileName") ?: "public_key.pem"
                val fileBytes = call.receiveStream().use { it.readBytes() }
                val publicKeyFile = File(keyPath, fileName)
                publicKeyFile.writeBytes(fileBytes)

                val validatorAddress =
                        Utility.calculateHash(Wallet.loadPublicKey(publicKeyFile.path))

                println("Validator : $validatorAddress stop validating...")

                call.respond("Validation Cancelled...")
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
                BlockChain.refreshFromDatabase() // Refresh cache

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
