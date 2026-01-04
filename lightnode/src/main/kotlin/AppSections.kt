import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    Card(
            elevation = 0.dp,
            backgroundColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        Column {
            Text(
                    text = "🥥 Kokonut Light Node",
                    style = MaterialTheme.typography.h4,
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(
                        label = if (state.isConnected) "Connected" else "Not Connected",
                        color = if (state.isConnected) MaterialTheme.colors.primary else Color.Gray,
                        active = state.isConnected
                )
                if (state.validatorAddress.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusChip(
                            label =
                                    if (state.validationState == ValidatorState.VALIDATING)
                                            "Validating"
                                    else "Stopped",
                            color =
                                    if (state.validationState == ValidatorState.VALIDATING)
                                            MaterialTheme.colors.primary
                                    else Color.Gray,
                            active = state.validationState == ValidatorState.VALIDATING
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(label: String, color: Color, active: Boolean) {
    Surface(
            color = color.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                    color = color,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(8.dp)
            ) {}
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                    text = label,
                    color = color,
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.Bold
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
    AnimatedVisibility(
            visible = state.isConnected && state.validatorAddress.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
    ) {
        Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                        text = "💰 Wallet & Stake",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                InfoRow("Address", state.validatorAddress)
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BalanceItem(
                            "Balance",
                            "${state.walletBalance} KNT",
                            MaterialTheme.colors.primary
                    )
                    BalanceItem(
                            "Staked",
                            "${state.stakedAmount} KNT",
                            MaterialTheme.colors.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.caption, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.body2, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BalanceItem(label: String, value: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.caption, color = Color.Gray)
        Text(
                text = value,
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold,
                color = color
        )
    }
}

// ============================================================================
// Connection Section
// ============================================================================

/** Connection Section - Node URL input and connect button */
@Composable
fun ConnectionSection(state: AppState) {
    Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            elevation = 2.dp,
            shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                    text = "🔌 Node Connection",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // URL Input
            OutlinedTextField(
                    value = state.peerAddress,
                    onValueChange = { state.peerAddress = it },
                    label = { Text("Node URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Text("🌐") },
                    shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Warnings
            AnimatedVisibility(visible = state.keysNotLoaded) {
                Text(
                        text = "⚠️ Please load your public and private keys before connecting",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Connect Button
            Button(
                    onClick = { performConnection(state) },
                    enabled = !state.keysNotLoaded,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                        if (state.isConnected) "🔄 Refresh Connection" else "🤝 Connect & Login",
                        fontWeight = FontWeight.Bold
                )
            }

            // Sync Status & Messages
            ChainSyncStatusSection(state)

            AnimatedVisibility(visible = state.connectionMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                        text = state.connectionMessage ?: "",
                        color =
                                if (state.isConnected) MaterialTheme.colors.primary
                                else MaterialTheme.colors.error,
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium
                )
            }
        }
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
    Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            elevation = 2.dp,
            shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                    text = "🔐 Key Management",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            KeyFileRow(
                    label = "Load Public Key",
                    filePath = state.selectedPublicKeyFilePath,
                    onSelect = {
                        val fileDialog = FileDialog(Frame(), "Select Public Key", FileDialog.LOAD)
                        fileDialog.isVisible = true
                        fileDialog.file?.let {
                            state.selectedPublicKeyFilePath =
                                    File(fileDialog.directory, it).absolutePath
                        }
                    }
            )

            Spacer(modifier = Modifier.height(12.dp))

            KeyFileRow(
                    label = "Load Private Key",
                    filePath = state.selectedPrivateKeyFilePath,
                    onSelect = {
                        val fileDialog = FileDialog(Frame(), "Select Private Key", FileDialog.LOAD)
                        fileDialog.isVisible = true
                        fileDialog.file?.let {
                            state.selectedPrivateKeyFilePath =
                                    File(fileDialog.directory, it).absolutePath
                        }
                    }
            )

            Spacer(modifier = Modifier.height(20.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                    onClick = {
                        val fileDialog =
                                FileDialog(Frame(), "Save New Private Key", FileDialog.SAVE)
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
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                            ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.secondary
                            )
            ) { Text("Generate New Keys", fontWeight = FontWeight.Bold, color = Color.White) }
        }
    }
}

@Composable
fun KeyFileRow(label: String, filePath: String?, onSelect: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Button(
                onClick = onSelect,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.surface)
        ) {
            Text(
                    label,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.primary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
                text =
                        if (filePath != null) {
                            val f = File(filePath)
                            if (f.name.length > 25) "..." + f.name.takeLast(25) else f.name
                        } else "Not selected",
                style = MaterialTheme.typography.body2,
                color = if (filePath != null) MaterialTheme.colors.onSurface else Color.Gray,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

// ============================================================================
// Staking Management Section
// ============================================================================

/** Staking Management Section - Deposit/Withdraw functionality */
@Composable
fun StakingManagementSection(state: AppState) {
    AnimatedVisibility(
            visible = state.isConnected,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
    ) {
        Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                        text = "🏦 Staking Management",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    OutlinedTextField(
                            value = state.depositAmountInput,
                            onValueChange = { state.depositAmountInput = it },
                            label = { Text("Amount (KNT)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                            onClick = { performDeposit(state) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(56.dp)
                    ) { Text("Deposit", fontWeight = FontWeight.Bold) }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                        onClick = { performWithdraw(state) },
                        enabled = state.stakedAmount > 0,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                                ButtonDefaults.buttonColors(
                                        backgroundColor = Color.Transparent,
                                        contentColor = MaterialTheme.colors.primary
                                ),
                        border =
                                androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colors.primary
                                ),
                        elevation = ButtonDefaults.elevation(0.dp)
                ) { Text("Withdraw / Unstake") }
            }
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
    AnimatedVisibility(
            visible = state.isConnected,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
    ) {
        Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                        text = "⚙️ Node Operation",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                            onClick = { startNodeValidation(state) },
                            enabled =
                                    state.validationState != ValidatorState.VALIDATING &&
                                            state.stakedAmount >=
                                                    BlockChain.getNetworkRules().minFullStake,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                    ButtonDefaults.buttonColors(
                                            backgroundColor = MaterialTheme.colors.primary
                                    )
                    ) { Text("▶ Start Node", color = Color.White, fontWeight = FontWeight.Bold) }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                            onClick = {
                                state.validationState = ValidatorState.READY
                                state.connectionMessage = "⏸ Node Stopped (Stake remains locked)"
                            },
                            enabled = state.validationState == ValidatorState.VALIDATING,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                    ButtonDefaults.buttonColors(
                                            backgroundColor = MaterialTheme.colors.error
                                    )
                    ) { Text("⏹ Stop Node", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
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
