import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URL
import kokonut.core.Block
import kokonut.core.BlockChain
import kokonut.state.ValidatorState
import kokonut.util.API.Companion.addBlock
import kokonut.util.API.Companion.getBalance
import kokonut.util.API.Companion.getChain
import kokonut.util.API.Companion.performHandshake
import kokonut.util.API.Companion.stakeLock
import kokonut.util.API.Companion.startValidating
import kokonut.util.API.Companion.stopValidating
import kokonut.util.NodeType
import kokonut.util.Wallet
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.serializer

// ============================================================================
// Header Section
// ============================================================================

/** App Header Section - Displays title and connection/node state */
@Composable
fun HeaderSection(state: AppState) {
    // Title
    Text(
            text = "🥥 Kokonut Light Node",
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Connection Status
    Row {
        Text(text = "Status: ", fontWeight = FontWeight.Bold)
        Text(
                text = if (state.isConnected) "✅ Connected" else "⭕ Not Connected",
                color = if (state.isConnected) Color.Green else Color.Gray
        )
    }

    // Validator State
    Row {
        if (state.validatorAddress.isNotEmpty()) {
            Text(text = "Node State: ", fontWeight = FontWeight.Bold)
            Text(
                    text =
                            if (state.validationState == ValidatorState.VALIDATING) "🟢 Running"
                            else "⚪ Stopped",
                    color =
                            if (state.validationState == ValidatorState.VALIDATING) Color.Green
                            else Color.Gray
            )
        }
    }
}

// ============================================================================
// Wallet Info Section
// ============================================================================

/** Wallet Info Section - Displays address, balance, and staked amount */
@Composable
fun WalletInfoSection(state: AppState) {
    if (state.isConnected && state.validatorAddress.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Divider()
        Spacer(modifier = Modifier.height(8.dp))

        Text(
                text = "💰 Wallet & Stake",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row {
            Text(text = "Address: ", fontWeight = FontWeight.Bold)
            Text(text = state.validatorAddress.take(20) + "...")
        }
        Row {
            Text(text = "Wallet Balance: ", fontWeight = FontWeight.Bold)
            Text(
                    text = "${state.walletBalance} KNT",
                    color = if (state.walletBalance > 0) Color(0xFF4CAF50) else Color.Gray
            )
        }
        Row {
            Text(text = "Staked Amount: ", fontWeight = FontWeight.Bold)
            Text(
                    text = "${state.stakedAmount} KNT",
                    color = if (state.stakedAmount > 0) Color(0xFF2196F3) else Color.Gray
            )
        }
    }
}

// ============================================================================
// Connection Section
// ============================================================================

/** Connection Section - Node URL input and connect button */
@Composable
fun ConnectionSection(state: AppState) {
    Spacer(modifier = Modifier.height(16.dp))
    Divider()
    Spacer(modifier = Modifier.height(16.dp))

    // Node URL Input
    Row {
        Text(modifier = Modifier.width(100.dp).height(50.dp), text = "Node URL: ")
        TextField(
                value = state.peerAddress,
                onValueChange = { state.peerAddress = it },
                modifier = Modifier.width(300.dp).height(50.dp)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Warning if keys not loaded
    if (state.keysNotLoaded) {
        Text(
                text = "⚠️ Please load your public and private keys before connecting",
                color = Color(0xFFFF9800),
                modifier = Modifier.padding(8.dp)
        )
    }

    // Connect Button
    Button(
            onClick = { performConnection(state) },
            enabled = !state.keysNotLoaded,
            modifier = Modifier.fillMaxWidth()
    ) { Text(if (state.isConnected) "🔄 Refresh Connection" else "🤝 Connect & Login") }

    // Chain Sync Status Warning
    ChainSyncStatusSection(state)

    // Connection Message
    if (state.connectionMessage != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
                text = state.connectionMessage!!,
                color = if (state.isConnected) Color.Green else Color.Red,
                modifier = Modifier.padding(8.dp)
        )
    }
}

/** Chain Synchronization Status Section */
@Composable
private fun ChainSyncStatusSection(state: AppState) {
    if (state.isConnected && state.chainSyncStatus != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
                text = state.chainSyncStatus!!,
                color = if (state.isChainOutOfSync) Color(0xFFFF5722) else Color(0xFF4CAF50),
                modifier = Modifier.padding(8.dp)
        )

        if (state.isChainOutOfSync) {
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = { performChainSync(state) }, modifier = Modifier.fillMaxWidth()) {
                Text("🔄 Sync Chain from FullNode")
            }
        }
    }
}

/** Connection logic implementation */
private fun performConnection(state: AppState) {
    try {
        if (state.keysNotLoaded) {
            state.connectionMessage = "❌ Please load your public and private keys first"
            state.isConnected = false
            return
        }

        state.connectionMessage = "🔄 Connecting to Full Node..."

        val url = URL(state.peerAddress)

        // Load wallet
        val wallet =
                Wallet(
                        privateKeyFile = File(state.selectedPrivateKeyFilePath),
                        publicKeyFile = File(state.selectedPublicKeyFilePath)
                )

        val publicKeyString = wallet.publicKey.toString()

        // Perform handshake
        val response = url.performHandshake(publicKeyString)

        if (response.success && response.networkInfo != null) {
            state.isConnected = true
            state.networkInfo = response.networkInfo
            state.connectionMessage = "✅ ${response.message}"

            // Initialize BlockChain
            BlockChain.initialize(NodeType.LIGHT, state.peerAddress)

            // Compare chains before syncing
            val remoteChain = url.getChain()
            val localChain = BlockChain.getChain()
            state.remoteChainSize = remoteChain.size
            state.localChainSize = localChain.size

            val remoteLastHash = remoteChain.lastOrNull()?.hash ?: ""
            val localLastHash = localChain.lastOrNull()?.hash ?: ""

            if (remoteLastHash != localLastHash || state.remoteChainSize != state.localChainSize) {
                state.isChainOutOfSync = true
                state.chainSyncStatus =
                        "⚠️ Chain Mismatch Detected!\n" +
                                "Local: ${state.localChainSize} blocks (hash: ${localLastHash.take(16)}...)\n" +
                                "Remote: ${state.remoteChainSize} blocks (hash: ${remoteLastHash.take(16)}...)"
            } else {
                state.isChainOutOfSync = false
                state.chainSyncStatus = "✅ Chain is in sync (${state.localChainSize} blocks)"
            }

            // Sync chain from FullNode
            BlockChain.loadChainFromFullNode(url)
            state.networkInfo = state.networkInfo?.copy(chainSize = BlockChain.getChainSize())

            // Auto-load validator info
            state.validatorAddress = wallet.validatorAddress
            state.validationState = wallet.validationState

            // Fetch Balance
            state.walletBalance = url.getBalance(state.validatorAddress)

            // Fetch Staked Amount
            val validator = BlockChain.validatorPool.getValidator(state.validatorAddress)
            state.stakedAmount = validator?.stakedAmount ?: 0.0
        } else {
            state.isConnected = false
            state.networkInfo = null
            state.connectionMessage = "❌ ${response.message}"
        }
    } catch (e: Exception) {
        state.isConnected = false
        state.networkInfo = null
        state.connectionMessage = "❌ Connection failed: ${e.message}"
    }
}

/** Chain Synchronization logic implementation */
private fun performChainSync(state: AppState) {
    try {
        val url = URL(state.peerAddress)
        state.connectionMessage = "🔄 Syncing chain from FullNode..."

        // Get remote and local chains
        val remoteChain = url.getChain()
        val localChain = BlockChain.getChain()

        // Compare Genesis blocks
        val remoteGenesis = remoteChain.firstOrNull()
        val localGenesis = localChain.firstOrNull()

        val genesisMatch =
                if (localGenesis == null) true else remoteGenesis?.hash == localGenesis.hash

        if (!genesisMatch && localGenesis != null) {
            println("⚠️ Genesis block mismatch detected!")
            println("   Local Genesis: ${localGenesis.hash.take(16)}")
            println("   Remote Genesis: ${remoteGenesis?.hash?.take(16) ?: "none"}")
            println("🗑️ Clearing local chain...")

            BlockChain.database.clearTable()
            BlockChain.refreshFromDatabase()

            state.connectionMessage =
                    "🗑️ Cleared local chain due to Genesis mismatch. Resyncing..."
        }

        // Force sync from FullNode
        BlockChain.loadChainFromFullNode(url)

        // Re-check sync status
        val newRemoteChain = url.getChain()
        val newLocalChain = BlockChain.getChain()
        state.remoteChainSize = newRemoteChain.size
        state.localChainSize = newLocalChain.size

        val remoteLastHash = newRemoteChain.lastOrNull()?.hash ?: ""
        val localLastHash = newLocalChain.lastOrNull()?.hash ?: ""

        if (remoteLastHash == localLastHash && state.remoteChainSize == state.localChainSize) {
            state.isChainOutOfSync = false
            state.chainSyncStatus = "✅ Chain synchronized! (${state.localChainSize} blocks)"
            state.connectionMessage = "✅ Chain sync completed successfully"

            // Refresh balance
            state.walletBalance = url.getBalance(state.validatorAddress)
            val validator = BlockChain.validatorPool.getValidator(state.validatorAddress)
            state.stakedAmount = validator?.stakedAmount ?: 0.0
        } else {
            state.chainSyncStatus =
                    "❌ Sync failed - chains still differ\n" +
                            "Local: ${state.localChainSize} blocks\n" +
                            "Remote: ${state.remoteChainSize} blocks"
            state.connectionMessage = "❌ Chain sync incomplete"
        }
    } catch (e: Exception) {
        state.errorMessage = "Sync error: ${e.message}"
    }
}

// ============================================================================
// Key Management Section
// ============================================================================

/** Key Management Section - Load/Generate Public/Private Keys */
@Composable
fun KeyManagementSection(state: AppState) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
            text = "🔐 Key Management",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Load Public Key
    Row {
        Button(
                onClick = {
                    val fileDialog = FileDialog(Frame(), "Select Public Key", FileDialog.LOAD)
                    fileDialog.isVisible = true
                    fileDialog.file?.let {
                        state.selectedPublicKeyFilePath =
                                File(fileDialog.directory, it).absolutePath
                    }
                }
        ) { Text("Load Public Key") }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
                text =
                        if (state.selectedPublicKeyFilePath!!.length > 30)
                                "..." + state.selectedPublicKeyFilePath!!.takeLast(30)
                        else state.selectedPublicKeyFilePath!!
        )
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Load Private Key
    Row {
        Button(
                onClick = {
                    val fileDialog = FileDialog(Frame(), "Select Private Key", FileDialog.LOAD)
                    fileDialog.isVisible = true
                    fileDialog.file?.let {
                        state.selectedPrivateKeyFilePath =
                                File(fileDialog.directory, it).absolutePath
                    }
                }
        ) { Text("Load Private Key") }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
                text =
                        if (state.selectedPrivateKeyFilePath!!.length > 30)
                                "..." + state.selectedPrivateKeyFilePath!!.takeLast(30)
                        else state.selectedPrivateKeyFilePath!!
        )
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Generate New Keys
    Button(
            onClick = {
                val fileDialog = FileDialog(Frame(), "Save New Private Key", FileDialog.SAVE)
                fileDialog.file = "private.pem"
                fileDialog.isVisible = true
                fileDialog.file?.let {
                    val dir = fileDialog.directory
                    val privateKeyFile = File(dir, it)
                    val publicKeyFile = File(dir, "public.pem")
                    val keyPair = Wallet.generateKey()
                    Wallet.saveKeyPairToFile(
                            keyPair,
                            privateKeyFile.absolutePath,
                            publicKeyFile.absolutePath
                    )
                    state.selectedPrivateKeyFilePath = privateKeyFile.absolutePath
                    state.selectedPublicKeyFilePath = publicKeyFile.absolutePath
                    state.showKeyGenDialog = true
                }
            }
    ) { Text("Generate New Keys") }
}

// ============================================================================
// Staking Management Section
// ============================================================================

/** Staking Management Section - Deposit/Withdraw functionality */
@Composable
fun StakingManagementSection(state: AppState) {
    if (!state.isConnected) return

    Spacer(modifier = Modifier.height(16.dp))
    Divider()
    Spacer(modifier = Modifier.height(16.dp))

    Text(
            text = "🏦 Staking Management",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row {
        TextField(
                value = state.depositAmountInput,
                onValueChange = { state.depositAmountInput = it },
                label = { Text("Amount (KNT)") },
                modifier = Modifier.width(150.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))

        // Deposit Button
        Button(onClick = { performDeposit(state) }) { Text("Deposit (Lock)") }

        Spacer(modifier = Modifier.width(8.dp))

        // Withdraw Button
        Button(onClick = { performWithdraw(state) }, enabled = state.stakedAmount > 0) {
            Text("Withdraw (Unstake)")
        }
    }
}

/** Deposit logic implementation */
private fun performDeposit(state: AppState) {
    try {
        val url = URL(state.peerAddress)
        val wallet =
                Wallet(
                        File(state.selectedPrivateKeyFilePath),
                        File(state.selectedPublicKeyFilePath)
                )
        val amount = state.depositAmountInput.toDoubleOrNull() ?: 0.0

        if (amount <= 0) {
            state.errorMessage = "Please enter a valid amount"
            return
        }

        if (url.stakeLock(wallet, File(state.selectedPublicKeyFilePath), amount)) {
            state.connectionMessage = "✅ Successfully Deposited (Locked) $amount KNT"
            // Refresh balance/stake
            BlockChain.loadChainFromFullNode(url)
            state.walletBalance = url.getBalance(wallet.validatorAddress)
            val validator = BlockChain.validatorPool.getValidator(wallet.validatorAddress)
            state.stakedAmount = validator?.stakedAmount ?: 0.0
        } else {
            state.errorMessage = "Deposit failed. Check balance or server logs."
        }
    } catch (e: Exception) {
        state.errorMessage = "Error: ${e.message}"
    }
}

/** Withdraw logic implementation */
private fun performWithdraw(state: AppState) {
    try {
        // Stop validation first if running
        state.validationState = ValidatorState.READY

        val url = URL(state.peerAddress)
        val publicKeyFile = File(state.selectedPublicKeyFilePath)

        if (url.stopValidating(publicKeyFile)) {
            BlockChain.loadChainFromFullNode(url)
            state.walletBalance = url.getBalance(state.validatorAddress)
            val validator = BlockChain.validatorPool.getValidator(state.validatorAddress)
            state.stakedAmount = validator?.stakedAmount ?: 0.0
            state.connectionMessage = "✅ Successfully Withdrawn (Unstaked)"
        } else {
            state.errorMessage = "Withdraw failed"
        }
    } catch (e: Exception) {
        state.errorMessage = "Error: ${e.message}"
    }
}

// ============================================================================
// Node Operation Section
// ============================================================================

/** Node Operation Section - Start/Stop functionality */
@Composable
fun NodeOperationSection(state: AppState) {
    if (!state.isConnected) return

    Spacer(modifier = Modifier.height(16.dp))
    Divider()
    Spacer(modifier = Modifier.height(16.dp))

    Text(
            text = "⚙️ Node Operation",
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row {
        // Start Node Button
        Button(
                onClick = { startNodeValidation(state) },
                enabled =
                        state.validationState != ValidatorState.VALIDATING &&
                                state.stakedAmount >= BlockChain.getNetworkRules().minFullStake
        ) { Text("▶ Start Node") }

        Spacer(modifier = Modifier.width(8.dp))

        // Stop Node Button
        Button(
                onClick = {
                    state.validationState = ValidatorState.READY
                    state.connectionMessage = "⏸ Node Stopped (Stake remains locked)"
                },
                enabled = state.validationState == ValidatorState.VALIDATING
        ) { Text("⏹ Stop Node") }
    }
}

/** Node Validation Start logic */
private fun startNodeValidation(state: AppState) {
    try {
        val url = URL(state.peerAddress)
        val publicKeyFile = File(state.selectedPublicKeyFilePath)
        val requiredStake = BlockChain.getNetworkRules().minFullStake

        if (state.stakedAmount < requiredStake) {
            state.errorMessage =
                    "Insufficient stake. Need at least $requiredStake KNT. Please Deposit first."
            return
        }

        if (url.startValidating(publicKeyFile)) {
            state.validationState = ValidatorState.VALIDATING
            state.connectionMessage = "🚀 Node Started! Validating blocks..."

            // Start Validation Loop
            Thread {
                        val wallet =
                                Wallet(
                                        File(state.selectedPrivateKeyFilePath),
                                        File(state.selectedPublicKeyFilePath)
                                )
                        while (state.validationState == ValidatorState.VALIDATING) {
                            try {
                                Thread.sleep(5000)
                                val data = kokonut.core.Data(comment = "Validated by LightNode")
                                val block = BlockChain.validate(wallet, data)

                                @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
                                url.addBlock(
                                        Json.encodeToJsonElement(serializer<Block>(), block),
                                        File(state.selectedPublicKeyFilePath)
                                )

                                BlockChain.loadChainFromFullNode(url)
                                val newBalance = url.getBalance(state.validatorAddress)
                                state.walletBalance = newBalance

                                // Refresh UI Info
                                val rewardTx =
                                        block.data.transactions.find {
                                            it.transaction == "VALIDATOR_REWARD"
                                        }
                                state.connectionMessage =
                                        "✅ Block #${block.index} validated! Reward: ${rewardTx?.remittance ?: 0.0} KNT"
                            } catch (e: Exception) {
                                if (e.message?.contains("Chain is Invalid") == true ||
                                                e.message?.contains("No Fuel Nodes found") == true
                                ) {
                                    state.validationState = ValidatorState.FAILED
                                    state.connectionMessage = "❌ Stopped: ${e.message}"
                                }
                                // Else: just failed to get selected, wait for next turn.
                            }
                        }
                    }
                    .start()
        } else {
            state.errorMessage = "Failed to start node session on server."
        }
    } catch (e: Exception) {
        state.errorMessage = "Error: ${e.message}"
    }
}

// ============================================================================
// Dialogs Section
// ============================================================================

/** Dialogs Section - Key generation complete and error dialogs */
@Composable
fun DialogsSection(state: AppState) {
    // Key Generation Dialog
    if (state.showKeyGenDialog) {
        AlertDialog(
                onDismissRequest = { state.showKeyGenDialog = false },
                title = { Text("Keys Generated") },
                text = { Text("Keys saved to:\n${state.selectedPrivateKeyFilePath}") },
                confirmButton = {
                    Button(onClick = { state.showKeyGenDialog = false }) { Text("OK") }
                }
        )
    }

    // Error Dialog
    if (state.errorMessage != null) {
        AlertDialog(
                onDismissRequest = { state.errorMessage = null },
                title = { Text("Notice") },
                text = { Text(state.errorMessage!!) },
                confirmButton = { Button(onClick = { state.errorMessage = null }) { Text("OK") } }
        )
    }
}

// ============================================================================
// Utility Composables
// ============================================================================

/** Info Row - Utility Composable for displaying label and value */
@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = "$label: ", fontWeight = FontWeight.Medium, modifier = Modifier.width(140.dp))
        Text(text = value)
    }
}

// ============================================================================
// Cleanup Functions
// ============================================================================

/** Cleanup on App Exit */
fun performCleanup(state: AppState, peerAddress: String) {
    println("🔄 App closing - performing cleanup...")

    try {
        val pubKeyPath = state.selectedPublicKeyFilePath
        if (state.isConnected &&
                        state.stakedAmount > 0.0 &&
                        pubKeyPath != null &&
                        pubKeyPath != "Please load a public key..." &&
                        peerAddress.isNotBlank()
        ) {
            println("⏹ Stopping node and withdrawing stake: ${state.stakedAmount} KNT...")

            try {
                val url = URL(peerAddress)
                val publicKeyFile = File(pubKeyPath)

                if (url.stopValidating(publicKeyFile)) {
                    println("✅ Node stopped and stake withdrawn successfully")
                } else {
                    println("⚠️ Failed to stop node / withdraw stake")
                }
            } catch (e: Exception) {
                println("❌ Cleanup API error: ${e.message}")
            }
        }

        println("✅ Cleanup completed")
    } catch (e: Exception) {
        println("❌ Cleanup error: ${e.message}")
    }
}
