package kokonut.util

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.PublicKey
import java.text.SimpleDateFormat
import java.util.Date
import kokonut.core.Block
import kokonut.core.BlockChain
import kokonut.core.BlockDataType
import kokonut.core.Data
import kokonut.core.FuelNodeInfo
import kokonut.core.Policy
import kokonut.core.Transaction
import kokonut.core.ValidatorOnboardingInfo
import kokonut.state.ValidatorState
import kokonut.util.API.Companion.getFullNodes
import kotlinx.coroutines.*
import kotlinx.coroutines.launch
import kotlinx.html.*
import kotlinx.serialization.json.Json

class Router {

    companion object {

        var validatorSessions: MutableSet<ValidatorSession> = mutableSetOf()
        private val onboardingLock = Any()

        private fun advertisedSelfAddress(call: ApplicationCall): String {
            val configured =
                    System.getenv("KOKONUT_ADVERTISE_ADDRESS")?.trim().orEmpty().ifBlank {
                        System.getenv("KOKONUT_FULLNODE_REWARD_RECEIVER")?.trim().orEmpty()
                    }
            if (configured.isNotEmpty()) return configured

            val forwardedProto = call.request.headers["X-Forwarded-Proto"]?.trim()
            val scheme = forwardedProto?.takeIf { it.isNotBlank() } ?: call.request.origin.scheme
            val host = call.request.host()
            val port = call.request.port()

            return if ((scheme == "http" && port == 80) || (scheme == "https" && port == 443)) {
                "$scheme://$host"
            } else {
                "$scheme://$host:$port"
            }
        }

        private fun propagateToPeer(
                peerAddress: String,
                size: Long,
                id: String,
                sourceAddress: String
        ) {
            // Encode the ID to ensure it's URL-safe
            val encodedId = java.net.URLEncoder.encode(id, "UTF-8")
            val urlStr = "$peerAddress/propagate?size=$size&id=$encodedId&address=$sourceAddress"

            var attempt = 0
            val maxRetries = 3
            var success = false

            while (attempt < maxRetries && !success) {
                attempt++
                var conn: HttpURLConnection? = null
                try {
                    val url = URL(urlStr)
                    conn = (url.openConnection() as HttpURLConnection)
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 3_000 // 3 seconds
                    conn.readTimeout = 5_000 // 5 seconds
                    conn.doOutput = true
                    conn.outputStream.use {}

                    val responseCode = conn.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        success = true
                        // success
                    } else {
                        println("⚠️ Propagation warning to $peerAddress: HTTP $responseCode")
                    }
                    conn.inputStream.use {}
                } catch (e: Exception) {
                    println(
                            "⚠️ Propagation failed to $peerAddress (Attempt $attempt/$maxRetries): ${e.message}"
                    )
                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(1000L * attempt) // Linear backoff: 1s, 2s...
                        } catch (ie: InterruptedException) {
                            Thread.currentThread().interrupt()
                        }
                    }
                } finally {
                    conn?.disconnect()
                }
            }

            if (!success) {
                println("❌ Failed to propagate to $peerAddress after $maxRetries attempts.")
            }
        }

        private fun notifyFullNodesToSyncFrom(sourceAddress: String) {
            try {
                val fuelNode = BlockChain.getPrimaryFuelNode()
                val fuelNodeAddress = fuelNode.toString().trimEnd('/')

                // Get peers including FuelNode and other FullNodes
                val peers =
                        (fuelNode.getFullNodes().map { it.address } + fuelNodeAddress)
                                .filter { it.isNotBlank() && it != sourceAddress }
                                .distinct()

                val size = BlockChain.getChainSize()
                val id = Utility.calculateHash(sourceAddress.hashCode().toLong())

                // Launch propagation in a coroutine scope to avoid blocking the main thread
                // and to process peers in parallel (or at least asynchronously)
                CoroutineScope(Dispatchers.IO).launch {
                    peers.forEach { peer ->
                        launch { propagateToPeer(peer, size, id, sourceAddress) }
                    }
                }
            } catch (e: Exception) {
                println("⚠️ Failed to initiate peer notification: ${e.message}")
            }
        }

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
                            h1 { +"Timestamp : ${System.currentTimeMillis()}" }
                            h1 { +"Get policy : /getPolicy" }
                            h1 { +"Get genesis block : /getGenesisBlock" }
                            h1 { +"Get full nodes : /getFullNodes" }
                            h1 { +"Transactions dashboard : /transactions" }
                        }
                    }
                }
            }
        }

        fun Route.transactionsDashboard() {
            get("/transactions") {
                val limit =
                        call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 2000)
                                ?: 200

                if (!BlockChain.isValid()) {
                    // Find which blocks are invalid for debugging
                    val chain = BlockChain.getChain()
                    val invalidBlocks = mutableListOf<String>()

                    for (i in chain.indices) {
                        val block = chain[i]
                        val recalcHash = block.calculateHash()

                        if (recalcHash != block.hash) {
                            invalidBlocks.add(
                                    "Block ${block.index}: hash mismatch (stored=${block.hash.take(16)}..., calculated=${recalcHash.take(16)}...)"
                            )
                            // Log detailed debug info
                            println("❌ Block ${block.index} INVALID:")
                            println("   Stored Hash: ${block.hash}")
                            println("   Calculated Hash: $recalcHash")
                            println("   Timestamp: ${block.timestamp}")
                            println("   Data: ${block.data}")
                        }

                        if (i > 0 && block.previousHash != chain[i - 1].hash) {
                            invalidBlocks.add("Block ${block.index}: previousHash mismatch")
                        }
                    }

                    val errorDetail =
                            if (invalidBlocks.isNotEmpty()) {
                                "Invalid blocks: ${invalidBlocks.joinToString("; ")}"
                            } else {
                                "Chain validation failed but no specific block error found"
                            }

                    call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            "Transactions dashboard unavailable: local chain is invalid. $errorDetail"
                    )
                    return@get
                }

                val chain = BlockChain.getChain().sortedByDescending { it.index }
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

                data class TxRow(
                        val blockIndex: Long,
                        val timestamp: Long,
                        val transaction: String,
                        val sender: String,
                        val receiver: String,
                        val remittance: Double,
                        val commission: Double
                )

                val rows =
                        chain
                                .flatMap { block ->
                                    val txs = block.data.transactions
                                    txs.map { tx ->
                                        TxRow(
                                                blockIndex = block.index,
                                                timestamp = block.timestamp,
                                                transaction = tx.transaction,
                                                sender = tx.sender,
                                                receiver = tx.receiver,
                                                remittance = tx.remittance,
                                                commission = tx.commission
                                        )
                                    }
                                }
                                .take(limit)

                val totalTx = BlockChain.getChain().sumOf { it.data.transactions.size }

                call.respondHtml(HttpStatusCode.OK) {
                    head {
                        title("Kokonut Fuel Node - Transactions")
                        style {
                            unsafe {
                                raw(
                                        """
                                    body {
                                        font-family: Arial, sans-serif;
                                        max-width: 1200px;
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
                                        font-size: 13px;
                                    }
                                    td {
                                        padding: 10px;
                                        border-bottom: 1px solid #ddd;
                                        font-size: 13px;
                                    }
                                    tr:hover {
                                        background-color: #f5f5f5;
                                    }
                                    .mono {
                                        font-family: monospace;
                                        font-size: 12px;
                                        color: #555;
                                        word-break: break-all;
                                    }
                                """
                                )
                            }
                        }
                    }
                    body {
                        h1 { +"🥥 Fuel Node Transactions" }

                        div(classes = "summary") {
                            h2 { +"Summary" }
                            p { +"Chain size: ${BlockChain.getChainSize()} blocks" }
                            p { +"Total transactions (all blocks): $totalTx" }
                            p { +"Showing most recent: ${rows.size} (limit=$limit)" }
                        }

                        if (rows.isEmpty()) {
                            p { +"No transactions found." }
                        } else {
                            table {
                                thead {
                                    tr {
                                        th { +"Block" }
                                        th { +"Time" }
                                        th { +"Type" }
                                        th { +"Sender" }
                                        th { +"Receiver" }
                                        th { +"Amount" }
                                        th { +"Fee" }
                                    }
                                }
                                tbody {
                                    rows.forEach { row ->
                                        tr {
                                            td { +row.blockIndex.toString() }
                                            td { +dateFormat.format(Date(row.timestamp)) }
                                            td { +row.transaction }
                                            td { span(classes = "mono") { +row.sender } }
                                            td { span(classes = "mono") { +row.receiver } }
                                            td { +row.remittance.toString() }
                                            td { +row.commission.toString() }
                                        }
                                    }
                                }
                            }
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
            get("/getChain") { call.respond(BlockChain.getChain()) }
        }

        fun Route.getPolicy() {
            get("/getPolicy") {
                val minimumStake = BlockChain.getNetworkRules().minFullStake
                call.respond(Policy(minimumStake))
            }
        }

        /**
         * Get balance for a given address Query parameter: address - the wallet address (validator
         * address) Returns the balance as a Double
         */
        fun Route.getBalance() {
            get("/getBalance") {
                val address = call.request.queryParameters["address"]

                if (address.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Missing 'address' query parameter")
                    return@get
                }

                try {
                    // Ensure chain is up-to-date
                    BlockChain.refreshFromDatabase()

                    val balance = BlockChain.getBalance(address)
                    println("💰 API /getBalance request for: $address -> Balance: $balance KNT")

                    call.respond(kokonut.core.BalanceResponse(address = address, balance = balance))
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            "Error: ${e.javaClass.simpleName} - ${e.message}"
                    )
                }
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

                    println("🤝 Handshake request from ${request.nodeType} node")

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

                    // Gather network information
                    val genesisBlock = BlockChain.getGenesisBlock()
                    val networkRules = BlockChain.getNetworkRules()
                    val fuelNodes = BlockChain.getFuelNodes()

                    var handshakeMessage =
                            "Handshake successful. Welcome to ${networkRules.networkId}!"

                    // 1-time validator onboarding reward on first successful LIGHT connect
                    if (request.nodeType.uppercase() == "LIGHT") {
                        val validatorAddress = Utility.calculateHash(request.publicKey)
                        synchronized(onboardingLock) {
                            val alreadyOnboarded =
                                    BlockChain.getChain().any { block ->
                                        block.data.type == BlockDataType.VALIDATOR_ONBOARDING &&
                                                block.data
                                                        .validatorOnboardingInfo
                                                        ?.validatorAddress == validatorAddress
                                    }

                            if (!alreadyOnboarded) {
                                val treasuryAddress = BlockChain.getTreasuryAddress()
                                val treasuryBalance = BlockChain.getTreasuryBalance()

                                if (treasuryBalance >= 2.0) {
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

                                    val lastBlock = BlockChain.getLastBlock()
                                    val blockTimestamp = System.currentTimeMillis()

                                    // IMPORTANT: Use block timestamp for transactions
                                    val transactions =
                                            listOf(
                                                    Transaction(
                                                            transaction =
                                                                    "VALIDATOR_ONBOARDING_FULLNODE",
                                                            sender = treasuryAddress,
                                                            receiver = fullNodeAddress,
                                                            remittance = 1.0,
                                                            commission = 0.0,
                                                            timestamp = blockTimestamp
                                                    ),
                                                    Transaction(
                                                            transaction =
                                                                    "VALIDATOR_ONBOARDING_VALIDATOR",
                                                            sender = treasuryAddress,
                                                            receiver = validatorAddress,
                                                            remittance = 1.0,
                                                            commission = 0.0,
                                                            timestamp = blockTimestamp
                                                    )
                                            )

                                    val onboardingData =
                                            Data(
                                                    reward = 0.0,
                                                    ticker = "KNT",
                                                    validator = "ONBOARDING",
                                                    transactions = transactions,
                                                    comment =
                                                            "Validator onboarding: $validatorAddress",
                                                    type = BlockDataType.VALIDATOR_ONBOARDING,
                                                    validatorOnboardingInfo = onboardingInfo
                                            )

                                    val onboardingBlock =
                                            Block(
                                                    index = lastBlock.index + 1,
                                                    previousHash = lastBlock.hash,
                                                    timestamp = blockTimestamp,
                                                    data = onboardingData,
                                                    validatorSignature = "",
                                                    hash = ""
                                            )

                                    onboardingBlock.hash = onboardingBlock.calculateHash()
                                    BlockChain.database.insert(onboardingBlock)
                                    BlockChain.refreshFromDatabase()

                                    // Best-effort: help other FullNodes converge quickly.
                                    notifyFullNodesToSyncFrom(advertisedSelfAddress(call))

                                    handshakeMessage =
                                            "Handshake successful. Onboarding reward granted."
                                } else {
                                    handshakeMessage =
                                            "Handshake successful. Onboarding skipped (insufficient treasury balance)."
                                }
                            }
                        }
                    }

                    val networkInfo =
                            kokonut.core.NetworkInfo(
                                    nodeType = "FULL",
                                    networkId = networkRules.networkId,
                                    genesisHash = genesisBlock.hash,
                                    chainSize = BlockChain.getChainSize(),
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
                                    message = handshakeMessage,
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
                var publicKeyFile: File? = null

                try {
                    val fileName = call.request.header("fileName") ?: "temp_auth_key.pem"
                    val fileBytes = call.receiveStream().use { it.readBytes() }
                    publicKeyFile = File(keyPath, fileName)
                    publicKeyFile!!.writeBytes(fileBytes)

                    val validatorAddress =
                            Utility.calculateHash(Wallet.loadPublicKey(publicKeyFile!!.path))

                    // Ensure chain info is fresh before checking stake
                    BlockChain.refreshFromDatabase()

                    val rules = BlockChain.getNetworkRules()
                    val validator = BlockChain.validatorPool.getValidator(validatorAddress)

                    // Check if validator exists and has enough stake (isActive checks both)
                    if (validator == null || validator.stakedAmount < rules.minFullStake) {
                        call.respond(
                                HttpStatusCode.BadRequest,
                                "Insufficient stake. Required: ${rules.minFullStake} KNT. Current: ${validator?.stakedAmount ?: 0.0}"
                        )
                        return@post
                    }

                    // Remove existing session if any
                    validatorSessions.removeIf { it.validatorAddress == validatorAddress }

                    val session =
                            ValidatorSession(
                                    validatorAddress,
                                    call.request.origin.remoteHost,
                                    ValidatorState.VALIDATING
                            )
                    validatorSessions.add(session)

                    println("✅ Validator started: $validatorAddress")
                    call.respond(HttpStatusCode.OK, "Validating Started")
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            "Error: ${e.javaClass.simpleName} - ${e.message}"
                    )
                } finally {
                    try {
                        publicKeyFile?.let {
                            if (it.exists()) {
                                it.delete()
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        }

        fun Route.stakeLock() {
            post("/stakeLock") {
                val keyPath = "/app/key"
                Utility.createDirectory(keyPath)
                var publicKeyFile: File? = null

                try {
                    val multipart = call.receiveMultipart()
                    var amount: Double? = null
                    var timestamp: Long? = null
                    var signatureBase64: String? = null

                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                when (part.name) {
                                    "amount" -> amount = part.value.toDoubleOrNull()
                                    "timestamp" -> timestamp = part.value.toLongOrNull()
                                    "signature" -> signatureBase64 = part.value
                                }
                            }
                            is PartData.FileItem -> {
                                if (part.name == "public_key") {
                                    val fileBytes = part.provider().toByteArray()
                                    val fileName = part.originalFileName ?: "temp_key.pem"
                                    publicKeyFile = File(keyPath, fileName)
                                    publicKeyFile!!.writeBytes(fileBytes)
                                }
                            }
                            else -> {}
                        }
                        part.dispose()
                    }

                    if (publicKeyFile == null ||
                                    !publicKeyFile!!.exists() ||
                                    amount == null ||
                                    timestamp == null ||
                                    signatureBase64.isNullOrBlank()
                    ) {
                        call.respond(
                                HttpStatusCode.BadRequest,
                                "Missing public_key, amount, timestamp, or signature"
                        )
                        return@post
                    }

                    // Use NetworkRules from local chain instead of querying Fuel Node
                    val rules = BlockChain.getNetworkRules()
                    if (amount!! < rules.minFullStake) {
                        call.respond(
                                HttpStatusCode.BadRequest,
                                "Insufficient stake. Required: ${rules.minFullStake} KNT"
                        )
                        return@post
                    }

                    val publicKey: PublicKey = Wallet.loadPublicKey(publicKeyFile!!.path)
                    val validatorAddress = Utility.calculateHash(publicKey)

                    val message = "STAKE_LOCK|$validatorAddress|${amount!!}|${timestamp!!}"
                    val signatureBytes =
                            try {
                                java.util.Base64.getDecoder().decode(signatureBase64)
                            } catch (e: Exception) {
                                null
                            }

                    if (signatureBytes == null ||
                                    !Wallet.verifySignature(
                                            message.toByteArray(),
                                            signatureBytes,
                                            publicKey
                                    )
                    ) {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid signature")
                        return@post
                    }

                    // Ensure chain info is fresh
                    BlockChain.refreshFromDatabase()
                    val balance = BlockChain.getBalance(validatorAddress)
                    if (balance < amount!!) {
                        call.respond(
                                HttpStatusCode.BadRequest,
                                "Insufficient balance. Balance: $balance KNT"
                        )
                        return@post
                    }

                    val stakeVault = BlockChain.getStakeVaultAddress()
                    val lastBlock = BlockChain.getLastBlock()
                    val blockTimestamp = System.currentTimeMillis()

                    // IMPORTANT: Use the same timestamp for transaction and block
                    val stakeTx =
                            Transaction(
                                    transaction = "STAKE_LOCK",
                                    sender = validatorAddress,
                                    receiver = stakeVault,
                                    remittance = amount!!,
                                    commission = 0.0,
                                    timestamp = blockTimestamp
                            )
                    val stakeData =
                            Data(
                                    reward = 0.0,
                                    ticker = "KNT",
                                    validator = "STAKE_LOCK",
                                    transactions = listOf(stakeTx),
                                    comment = "Stake lock: $validatorAddress",
                                    type = BlockDataType.STAKE_LOCK
                            )
                    val stakeBlock =
                            Block(
                                    index = lastBlock.index + 1,
                                    previousHash = lastBlock.hash,
                                    timestamp = blockTimestamp,
                                    data = stakeData,
                                    validatorSignature = "",
                                    hash = ""
                            )
                    stakeBlock.hash = stakeBlock.calculateHash()

                    // Debug: Log hash calculation details
                    println("📊 STAKE_LOCK Block Debug:")
                    println("   Index: ${stakeBlock.index}")
                    println("   Timestamp: ${stakeBlock.timestamp}")
                    println("   Tx Timestamp: ${stakeTx.timestamp}")
                    println("   Calculated Hash: ${stakeBlock.hash}")
                    println("   IsValid: ${stakeBlock.isValid()}")

                    BlockChain.database.insert(stakeBlock)
                    BlockChain.refreshFromDatabase()

                    // Verify chain integrity after insertion
                    if (!BlockChain.isValid()) {
                        println("⚠️ Chain integrity check failed after stake lock insertion!")
                        // Debug: Find which block is invalid
                        val chain = BlockChain.getChain()
                        chain.forEachIndexed { idx, block ->
                            val recalcHash = block.calculateHash()
                            if (recalcHash != block.hash) {
                                println("❌ Block $idx hash mismatch!")
                                println("   Stored: ${block.hash}")
                                println("   Calculated: $recalcHash")
                            }
                        }
                    } else {
                        println("✅ Chain integrity verified after stake lock insertion")
                    }

                    // Log the transaction
                    println(
                            "📝 STAKE_LOCK recorded: $validatorAddress -> $stakeVault: ${amount!!} KNT"
                    )

                    // Try to propagate, logging error if fails but not failing the request
                    try {
                        notifyFullNodesToSyncFrom(advertisedSelfAddress(call))
                    } catch (e: Exception) {
                        println("⚠️ Failed to trigger propagation: ${e.message}")
                    }

                    call.respond(HttpStatusCode.OK, "Stake locked: ${amount!!} KNT")
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            "Error: ${e.javaClass.simpleName} - ${e.message}"
                    )
                } finally {
                    try {
                        publicKeyFile?.let {
                            if (it.exists()) {
                                it.delete()
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore file delete errors
                    }
                }
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
                            kokonut.util.FullNode(
                                    id = Utility.calculateHash(address.hashCode().toLong()),
                                    address = address
                            )
                        }
                call.respond(activeNodes)
            }
        }

        /**
         * Get known Full Nodes (from local BlockChain cache) Useful for Full Nodes to relay peer
         * information to Light Nodes.
         */
        fun Route.getKnownFullNodes() {
            get("/getFullNodes") {
                // Return BlockChain.fullNodes (List<FullNode>)
                call.respond(BlockChain.fullNodes)
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
                    val selfAddress = advertisedSelfAddress(call)
                    BlockChain.fullNodes
                            .map { it.address }
                            .filter { it.isNotBlank() && it != address && it != selfAddress }
                            .distinct()
                            .forEach { peer -> propagateToPeer(peer, size, id, address) }
                } else if (BlockChain.getChainSize() == size) {} else {

                    call.respond(HttpStatusCode.Created, "Propagate Failed")
                }
            }
        }

        fun Route.addBlock() {
            post("/addBlock") {
                val keyPath = "/app/key"
                Utility.createDirectory(keyPath)
                var publicKeyFile: File? = null

                try {
                    val multipart = call.receiveMultipart()
                    var block: Block? = null

                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                if (part.name == "json") {
                                    try {
                                        block = Json.decodeFromString<Block>(part.value)
                                        println("Received JSON: $block")
                                    } catch (e: Exception) {
                                        println("Error decoding JSON: ${e.message}")
                                    }
                                }
                            }
                            is PartData.FileItem -> {
                                if (part.name == "public_key") {
                                    val fileBytes = part.provider().toByteArray()
                                    val fileName = part.originalFileName ?: "temp_block_key.pem"
                                    publicKeyFile = File(keyPath, fileName)
                                    publicKeyFile!!.writeBytes(fileBytes)
                                    println("Received file: $fileName")
                                }
                            }
                            else -> {}
                        }
                        part.dispose()
                    }

                    if (block != null && publicKeyFile != null && publicKeyFile!!.exists()) {
                        println(block)

                        val publicKey: PublicKey = Wallet.loadPublicKey(publicKeyFile!!.path)
                        val validatorString: String = Utility.calculateHash(publicKey)

                        // Refresh to get latest chain state
                        BlockChain.refreshFromDatabase()
                        val lastBlock = BlockChain.getLastBlock()

                        // Check Validator
                        if (block!!.data.validator != validatorString) {
                            call.respond(
                                    HttpStatusCode.Created,
                                    "Block Add Failed : Invalid validator"
                            )
                            return@post
                        }

                        // Check Index (Using <= to handle potential re-transmissions gracefully)
                        if (block!!.index <= lastBlock.index) {
                            if (block!!.hash == lastBlock.hash) {
                                call.respond(HttpStatusCode.Created, "Block Already Propagated")
                            } else {
                                call.respond(
                                        HttpStatusCode.Created,
                                        "Block Add Failed: Stale or invalid block index"
                                )
                            }
                            return@post
                        }

                        // Check previousHash for blockchain integrity
                        if (block!!.previousHash != lastBlock.hash) {
                            call.respond(
                                    HttpStatusCode.Created,
                                    "Block Add Failed: previousHash mismatch. Expected: ${lastBlock.hash}, Got: ${block!!.previousHash}"
                            )
                            return@post
                        }

                        // Check index is exactly lastBlock.index + 1
                        if (block!!.index != lastBlock.index + 1) {
                            call.respond(
                                    HttpStatusCode.Created,
                                    "Block Add Failed: Invalid block index. Expected: ${lastBlock.index + 1}, Got: ${block!!.index}"
                            )
                            return@post
                        }

                        // Check Hash
                        val calculatedHash = block!!.calculateHash()
                        if (block!!.hash == calculatedHash) {
                            BlockChain.database.insert(block!!)
                            BlockChain.refreshFromDatabase()

                            // Verify chain integrity after insertion
                            if (!BlockChain.isValid()) {
                                println("⚠️ Chain integrity check failed after block insertion!")
                            }

                            // Best-effort: help other FullNodes converge quickly.
                            try {
                                notifyFullNodesToSyncFrom(advertisedSelfAddress(call))
                            } catch (e: Exception) {
                                println("⚠️ Failed to trigger propagation: ${e.message}")
                            }

                            // Log transaction details
                            block!!.data.transactions.forEach { tx ->
                                println(
                                        "📝 Transaction recorded: ${tx.transaction} - ${tx.sender} -> ${tx.receiver}: ${tx.remittance} KNT"
                                )
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
                        }
                    } else {
                        call.respondText(
                                "Missing block or validator public key",
                                status = HttpStatusCode.BadRequest
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            "Error processing block: ${e.message}"
                    )
                } finally {
                    try {
                        publicKeyFile?.let {
                            if (it.exists()) {
                                it.delete()
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        }

        fun Route.stopValidating() {
            post("/stopValidating") {
                val keyPath = "/app/key"
                Utility.createDirectory(keyPath)
                var publicKeyFile: File? = null

                try {
                    val fileName = call.request.header("fileName") ?: "temp_auth_key.pem"
                    val fileBytes = call.receiveStream().use { it.readBytes() }
                    publicKeyFile = File(keyPath, fileName)
                    publicKeyFile!!.writeBytes(fileBytes)

                    val publicKey = Wallet.loadPublicKey(publicKeyFile!!.path)
                    val validatorAddress = Utility.calculateHash(publicKey)

                    println("🛑 Stopping validation for: $validatorAddress")

                    // 1. Remove from active sessions immediately
                    validatorSessions.removeIf { it.validatorAddress == validatorAddress }

                    // 2. Check staked amount
                    BlockChain.refreshFromDatabase()
                    val validator = BlockChain.validatorPool.getValidator(validatorAddress)
                    val stakedAmount = validator?.stakedAmount ?: 0.0

                    if (stakedAmount > 0) {
                        // 3. Create UNSTAKE block to return funds
                        val lastBlock = BlockChain.getLastBlock()
                        val stakeVault = BlockChain.getStakeVaultAddress()
                        val blockTimestamp = System.currentTimeMillis()

                        // Create a transaction from Vault to Validator
                        // IMPORTANT: Use the same timestamp as the block to ensure hash consistency
                        val unstakeTx =
                                Transaction(
                                        transaction = "UNSTAKE",
                                        sender = stakeVault,
                                        receiver = validatorAddress,
                                        remittance = stakedAmount,
                                        commission = 0.0,
                                        timestamp = blockTimestamp
                                )

                        val data =
                                Data(
                                        reward = 0.0,
                                        ticker = "KNT",
                                        validator = "UNSTAKE_SYSTEM",
                                        transactions = listOf(unstakeTx),
                                        comment = "Unstake: $validatorAddress",
                                        type = BlockDataType.UNSTAKE
                                )

                        val unstakeBlock =
                                Block(
                                        index = lastBlock.index + 1,
                                        previousHash = lastBlock.hash,
                                        timestamp = blockTimestamp,
                                        data = data,
                                        validatorSignature =
                                                "", // System block, no validator signature needed
                                        // typically
                                        hash = ""
                                )
                        unstakeBlock.hash = unstakeBlock.calculateHash()

                        // Debug: Log hash calculation details
                        println("📊 UNSTAKE Block Debug:")
                        println("   Index: ${unstakeBlock.index}")
                        println("   PreviousHash: ${unstakeBlock.previousHash}")
                        println("   Timestamp: ${unstakeBlock.timestamp}")
                        println("   Tx Timestamp: ${unstakeTx.timestamp}")
                        println("   Calculated Hash: ${unstakeBlock.hash}")
                        println("   IsValid: ${unstakeBlock.isValid()}")

                        // Insert and Refresh
                        BlockChain.database.insert(unstakeBlock)
                        BlockChain.refreshFromDatabase()

                        // Verify chain integrity after insertion
                        if (!BlockChain.isValid()) {
                            println("⚠️ Chain integrity check failed after unstake insertion!")
                            // Debug: Find which block is invalid
                            val chain = BlockChain.getChain()
                            chain.forEachIndexed { idx, block ->
                                val recalcHash = block.calculateHash()
                                if (recalcHash != block.hash) {
                                    println("❌ Block $idx hash mismatch!")
                                    println("   Stored: ${block.hash}")
                                    println("   Calculated: $recalcHash")
                                }
                            }
                        } else {
                            println("✅ Chain integrity verified after unstake insertion")
                        }

                        // Log the transaction
                        println(
                                "📝 UNSTAKE recorded: $stakeVault -> $validatorAddress: $stakedAmount KNT"
                        )

                        // Propagate
                        try {
                            notifyFullNodesToSyncFrom(advertisedSelfAddress(call))
                        } catch (e: Exception) {
                            println("⚠️ Failed to trigger propagation: ${e.message}")
                        }

                        call.respond(
                                HttpStatusCode.OK,
                                "Validation Stopped. Unstaked: $stakedAmount KNT and returned to wallet."
                        )
                    } else {
                        call.respond(
                                HttpStatusCode.OK,
                                "Validation Stopped. No stake found to return."
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            "Error stopping validation: ${e.message}"
                    )
                } finally {
                    try {
                        publicKeyFile?.let { if (it.exists()) it.delete() }
                    } catch (e: Exception) {
                        /* Ignore */
                    }
                }
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
