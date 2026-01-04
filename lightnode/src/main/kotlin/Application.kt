import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URL
import kokonut.core.Block
import kokonut.core.BlockChain
import kokonut.core.NetworkInfo
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

@Composable
@Preview
fun App() {
        var selectedPublicKeyFilePath by remember {
                mutableStateOf<String?>("Please load a public key...")
        }
        var selectedPrivateKeyFilePath by remember {
                mutableStateOf<String?>("Please load a private key...")
        }

        var validatorAddress by remember { mutableStateOf("") }
        var peerAddress by remember { mutableStateOf("http://127.0.0.1:80") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        var validationState by remember { mutableStateOf(ValidatorState.READY) }

        // Connection state
        var isConnected by remember { mutableStateOf(false) }
        var networkInfo by remember { mutableStateOf<NetworkInfo?>(null) }
        var connectionMessage by remember { mutableStateOf<String?>(null) }

        var showKeyGenDialog by remember { mutableStateOf(false) }

        // Wallet balance & Stake state
        var walletBalance by remember { mutableStateOf(0.0) }
        var stakedAmount by remember { mutableStateOf(0.0) }
        var depositAmountInput by remember { mutableStateOf("10.0") }

        // Chain sync status
        var chainSyncStatus by remember { mutableStateOf<String?>(null) }
        var isChainOutOfSync by remember { mutableStateOf(false) }
        var localChainSize by remember { mutableStateOf(0) }
        var remoteChainSize by remember { mutableStateOf(0) }

        // Cleanup on app dispose (window close)
        DisposableEffect(Unit) {
                onDispose {
                        println("🔄 App closing - performing cleanup...")

                        try {
                                // Withdraw stake if connected and has stake
                                val pubKeyPath = selectedPublicKeyFilePath
                                if (isConnected &&
                                                stakedAmount > 0.0 &&
                                                pubKeyPath != null &&
                                                pubKeyPath != "Please load a public key..." &&
                                                peerAddress.isNotBlank()
                                ) {

                                        println(
                                                "⏹ Stopping node and withdrawing stake: $stakedAmount KNT..."
                                        )

                                        try {
                                                val url = URL(peerAddress)
                                                val publicKeyFile = File(pubKeyPath)

                                                // Call stopValidating which will stop validation
                                                // and unstake
                                                if (url.stopValidating(publicKeyFile)) {
                                                        println(
                                                                "✅ Node stopped and stake withdrawn successfully"
                                                        )
                                                } else {
                                                        println(
                                                                "⚠️ Failed to stop node / withdraw stake"
                                                        )
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
        }

        MaterialTheme {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                        text =
                                                if (isConnected) "✅ Connected"
                                                else "⭕ Not Connected",
                                        color = if (isConnected) Color.Green else Color.Gray
                                )
                        }

                        // Validator State
                        Row {
                                if (validatorAddress.isNotEmpty()) {
                                        Text(text = "Node State: ", fontWeight = FontWeight.Bold)
                                        Text(
                                                text =
                                                        if (validationState ==
                                                                        ValidatorState.VALIDATING
                                                        )
                                                                "🟢 Running"
                                                        else "⚪ Stopped",
                                                color =
                                                        if (validationState ==
                                                                        ValidatorState.VALIDATING
                                                        )
                                                                Color.Green
                                                        else Color.Gray
                                        )
                                }
                        }

                        // Wallet Info (shown when connected)
                        if (isConnected && validatorAddress.isNotEmpty()) {
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
                                        Text(text = validatorAddress.take(20) + "...")
                                }
                                Row {
                                        Text(
                                                text = "Wallet Balance: ",
                                                fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                                text = "$walletBalance KNT",
                                                color =
                                                        if (walletBalance > 0) Color(0xFF4CAF50)
                                                        else Color.Gray
                                        )
                                }
                                Row {
                                        Text(text = "Staked Amount: ", fontWeight = FontWeight.Bold)
                                        Text(
                                                text = "$stakedAmount KNT",
                                                color =
                                                        if (stakedAmount > 0) Color(0xFF2196F3)
                                                        else Color.Gray
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // Node URL Input
                        Row {
                                Text(
                                        modifier = Modifier.width(100.dp).height(50.dp),
                                        text = "Node URL: "
                                )
                                TextField(
                                        value = peerAddress,
                                        onValueChange = { peerAddress = it },
                                        modifier = Modifier.width(300.dp).height(50.dp)
                                )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Warning if keys not loaded
                        val keysNotLoaded =
                                selectedPublicKeyFilePath == "Please load a public key..." ||
                                        selectedPrivateKeyFilePath == "Please load a private key..."

                        if (keysNotLoaded) {
                                Text(
                                        text =
                                                "⚠️ Please load your public and private keys before connecting",
                                        color = Color(0xFFFF9800),
                                        modifier = Modifier.padding(8.dp)
                                )
                        }

                        // Connect Button (Acts as Login)
                        Button(
                                onClick = {
                                        try {
                                                // Validate keys are loaded
                                                if (keysNotLoaded) {
                                                        connectionMessage =
                                                                "❌ Please load your public and private keys first"
                                                        isConnected = false
                                                        return@Button
                                                }

                                                connectionMessage = "🔄 Connecting to Full Node..."

                                                val url = URL(peerAddress)

                                                // Load wallet to get public key
                                                val wallet =
                                                        Wallet(
                                                                privateKeyFile =
                                                                        File(
                                                                                selectedPrivateKeyFilePath
                                                                        ),
                                                                publicKeyFile =
                                                                        File(
                                                                                selectedPublicKeyFilePath
                                                                        )
                                                        )

                                                val publicKeyString = wallet.publicKey.toString()

                                                // Perform handshake
                                                val response = url.performHandshake(publicKeyString)

                                                if (response.success && response.networkInfo != null
                                                ) {
                                                        isConnected = true
                                                        networkInfo = response.networkInfo
                                                        connectionMessage = "✅ ${response.message}"

                                                        // Initialize BlockChain
                                                        BlockChain.initialize(
                                                                NodeType.LIGHT,
                                                                peerAddress
                                                        )

                                                        // Compare chains before syncing
                                                        val remoteChain = url.getChain()
                                                        val localChain = BlockChain.getChain()
                                                        remoteChainSize = remoteChain.size
                                                        localChainSize = localChain.size

                                                        val remoteLastHash =
                                                                remoteChain.lastOrNull()?.hash ?: ""
                                                        val localLastHash =
                                                                localChain.lastOrNull()?.hash ?: ""

                                                        if (remoteLastHash != localLastHash ||
                                                                        remoteChainSize !=
                                                                                localChainSize
                                                        ) {
                                                                isChainOutOfSync = true
                                                                chainSyncStatus =
                                                                        "⚠️ Chain Mismatch Detected!\n" +
                                                                                "Local: $localChainSize blocks (hash: ${localLastHash.take(16)}...)\n" +
                                                                                "Remote: $remoteChainSize blocks (hash: ${remoteLastHash.take(16)}...)"
                                                        } else {
                                                                isChainOutOfSync = false
                                                                chainSyncStatus =
                                                                        "✅ Chain is in sync ($localChainSize blocks)"
                                                        }

                                                        // Sync chain from FullNode
                                                        BlockChain.loadChainFromFullNode(url)
                                                        networkInfo =
                                                                networkInfo?.copy(
                                                                        chainSize =
                                                                                BlockChain
                                                                                        .getChainSize()
                                                                )

                                                        // Auto-load validator info
                                                        validatorAddress = wallet.validatorAddress
                                                        validationState = wallet.validationState

                                                        // Fetch Balance
                                                        walletBalance =
                                                                url.getBalance(validatorAddress)

                                                        // Fetch Staked Amount
                                                        val validator =
                                                                BlockChain.validatorPool
                                                                        .getValidator(
                                                                                validatorAddress
                                                                        )
                                                        stakedAmount =
                                                                validator?.stakedAmount ?: 0.0
                                                } else {
                                                        isConnected = false
                                                        networkInfo = null
                                                        connectionMessage = "❌ ${response.message}"
                                                }
                                        } catch (e: Exception) {
                                                isConnected = false
                                                networkInfo = null
                                                connectionMessage =
                                                        "❌ Connection failed: ${e.message}"
                                        }
                                },
                                enabled = !keysNotLoaded,
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                Text(
                                        if (isConnected) "🔄 Refresh Connection"
                                        else "🤝 Connect & Login"
                                )
                        }

                        // Connection Message
                        // Chain Sync Status Warning
                        if (isConnected && chainSyncStatus != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                        text = chainSyncStatus!!,
                                        color =
                                                if (isChainOutOfSync) Color(0xFFFF5722)
                                                else Color(0xFF4CAF50),
                                        modifier = Modifier.padding(8.dp)
                                )

                                if (isChainOutOfSync) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                                onClick = {
                                                        try {
                                                                val url = URL(peerAddress)
                                                                connectionMessage =
                                                                        "🔄 Syncing chain from FullNode..."

                                                                // Get remote and local chains
                                                                val remoteChain = url.getChain()
                                                                val localChain =
                                                                        BlockChain.getChain()

                                                                // Compare Genesis blocks
                                                                val remoteGenesis =
                                                                        remoteChain.firstOrNull()
                                                                val localGenesis =
                                                                        localChain.firstOrNull()

                                                                val genesisMatch =
                                                                        if (localGenesis == null) {
                                                                                true // Local chain
                                                                                // is empty,
                                                                                // proceed with
                                                                                // sync
                                                                        } else {
                                                                                remoteGenesis
                                                                                        ?.hash ==
                                                                                        localGenesis
                                                                                                .hash
                                                                        }

                                                                if (!genesisMatch &&
                                                                                localGenesis != null
                                                                ) {
                                                                        // Genesis blocks differ -
                                                                        // clear local chain
                                                                        println(
                                                                                "⚠️ Genesis block mismatch detected!"
                                                                        )
                                                                        println(
                                                                                "   Local Genesis: ${localGenesis.hash.take(16)}"
                                                                        )
                                                                        println(
                                                                                "   Remote Genesis: ${remoteGenesis?.hash?.take(16) ?: "none"}"
                                                                        )
                                                                        println(
                                                                                "🗑️ Clearing local chain..."
                                                                        )

                                                                        BlockChain.database
                                                                                .clearTable()
                                                                        BlockChain
                                                                                .refreshFromDatabase()

                                                                        connectionMessage =
                                                                                "🗑️ Cleared local chain due to Genesis mismatch. Resyncing..."
                                                                }

                                                                // Force sync from FullNode
                                                                BlockChain.loadChainFromFullNode(
                                                                        url
                                                                )

                                                                // Re-check sync status
                                                                val newRemoteChain = url.getChain()
                                                                val newLocalChain =
                                                                        BlockChain.getChain()
                                                                remoteChainSize =
                                                                        newRemoteChain.size
                                                                localChainSize = newLocalChain.size

                                                                val remoteLastHash =
                                                                        newRemoteChain.lastOrNull()
                                                                                ?.hash
                                                                                ?: ""
                                                                val localLastHash =
                                                                        newLocalChain.lastOrNull()
                                                                                ?.hash
                                                                                ?: ""

                                                                if (remoteLastHash ==
                                                                                localLastHash &&
                                                                                remoteChainSize ==
                                                                                        localChainSize
                                                                ) {
                                                                        isChainOutOfSync = false
                                                                        chainSyncStatus =
                                                                                "✅ Chain synchronized! ($localChainSize blocks)"
                                                                        connectionMessage =
                                                                                "✅ Chain sync completed successfully"

                                                                        // Refresh balance
                                                                        walletBalance =
                                                                                url.getBalance(
                                                                                        validatorAddress
                                                                                )
                                                                        val validator =
                                                                                BlockChain
                                                                                        .validatorPool
                                                                                        .getValidator(
                                                                                                validatorAddress
                                                                                        )
                                                                        stakedAmount =
                                                                                validator
                                                                                        ?.stakedAmount
                                                                                        ?: 0.0
                                                                } else {
                                                                        chainSyncStatus =
                                                                                "❌ Sync failed - chains still differ\n" +
                                                                                        "Local: $localChainSize blocks\n" +
                                                                                        "Remote: $remoteChainSize blocks"
                                                                        connectionMessage =
                                                                                "❌ Chain sync incomplete"
                                                                }
                                                        } catch (e: Exception) {
                                                                errorMessage =
                                                                        "Sync error: ${e.message}"
                                                        }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                        ) { Text("🔄 Sync Chain from FullNode") }
                                }
                        }

                        if (connectionMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                        text = connectionMessage!!,
                                        color = if (isConnected) Color.Green else Color.Red,
                                        modifier = Modifier.padding(8.dp)
                                )
                        }

                        // Key Management Section
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                                text = "🔐 Key Management",
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Keys UI
                        Row {
                                Button(
                                        onClick = {
                                                val fileDialog =
                                                        FileDialog(
                                                                Frame(),
                                                                "Select Public Key",
                                                                FileDialog.LOAD
                                                        )
                                                fileDialog.isVisible = true
                                                fileDialog.file?.let {
                                                        selectedPublicKeyFilePath =
                                                                File(fileDialog.directory, it)
                                                                        .absolutePath
                                                }
                                        }
                                ) { Text("Load Public Key") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        text =
                                                if (selectedPublicKeyFilePath!!.length > 30)
                                                        "..." +
                                                                selectedPublicKeyFilePath!!
                                                                        .takeLast(30)
                                                else selectedPublicKeyFilePath!!
                                )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                                Button(
                                        onClick = {
                                                val fileDialog =
                                                        FileDialog(
                                                                Frame(),
                                                                "Select Private Key",
                                                                FileDialog.LOAD
                                                        )
                                                fileDialog.isVisible = true
                                                fileDialog.file?.let {
                                                        selectedPrivateKeyFilePath =
                                                                File(fileDialog.directory, it)
                                                                        .absolutePath
                                                }
                                        }
                                ) { Text("Load Private Key") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        text =
                                                if (selectedPrivateKeyFilePath!!.length > 30)
                                                        "..." +
                                                                selectedPrivateKeyFilePath!!
                                                                        .takeLast(30)
                                                else selectedPrivateKeyFilePath!!
                                )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                                onClick = {
                                        val fileDialog =
                                                FileDialog(
                                                        Frame(),
                                                        "Save New Private Key",
                                                        FileDialog.SAVE
                                                )
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
                                                selectedPrivateKeyFilePath =
                                                        privateKeyFile.absolutePath
                                                selectedPublicKeyFilePath =
                                                        publicKeyFile.absolutePath
                                                showKeyGenDialog = true
                                        }
                                }
                        ) { Text("Generate New Keys") }

                        if (isConnected) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(16.dp))

                                // 1. Staking Management (Deposit/Withdraw)
                                Text(
                                        text = "🏦 Staking Management",
                                        style = MaterialTheme.typography.h6,
                                        fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row {
                                        TextField(
                                                value = depositAmountInput,
                                                onValueChange = { depositAmountInput = it },
                                                label = { Text("Amount (KNT)") },
                                                modifier = Modifier.width(150.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                                onClick = {
                                                        try {
                                                                val url = URL(peerAddress)
                                                                val wallet =
                                                                        Wallet(
                                                                                File(
                                                                                        selectedPrivateKeyFilePath
                                                                                ),
                                                                                File(
                                                                                        selectedPublicKeyFilePath
                                                                                )
                                                                        )
                                                                val amount =
                                                                        depositAmountInput
                                                                                .toDoubleOrNull()
                                                                                ?: 0.0

                                                                if (amount <= 0) {
                                                                        errorMessage =
                                                                                "Please enter a valid amount"
                                                                        return@Button
                                                                }

                                                                if (url.stakeLock(
                                                                                wallet,
                                                                                File(
                                                                                        selectedPublicKeyFilePath
                                                                                ),
                                                                                amount
                                                                        )
                                                                ) {
                                                                        connectionMessage =
                                                                                "✅ Successfully Deposited (Locked) $amount KNT"
                                                                        // Refresh balance/stake
                                                                        BlockChain
                                                                                .loadChainFromFullNode(
                                                                                        url
                                                                                ) // Sync STAKE_LOCK
                                                                        // block
                                                                        walletBalance =
                                                                                url.getBalance(
                                                                                        wallet.validatorAddress
                                                                                )
                                                                        val validator =
                                                                                BlockChain
                                                                                        .validatorPool
                                                                                        .getValidator(
                                                                                                wallet.validatorAddress
                                                                                        )
                                                                        stakedAmount =
                                                                                validator
                                                                                        ?.stakedAmount
                                                                                        ?: 0.0
                                                                } else {
                                                                        errorMessage =
                                                                                "Deposit failed. Check balance or server logs."
                                                                }
                                                        } catch (e: Exception) {
                                                                errorMessage = "Error: ${e.message}"
                                                        }
                                                }
                                        ) { Text("Deposit (Lock)") }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                                onClick = {
                                                        try {
                                                                // Stop validation first if running
                                                                validationState =
                                                                        ValidatorState.READY

                                                                val url = URL(peerAddress)
                                                                val publicKeyFile =
                                                                        File(
                                                                                selectedPublicKeyFilePath
                                                                        )

                                                                if (url.stopValidating(
                                                                                publicKeyFile
                                                                        )
                                                                ) { // Calls /stopValidating which
                                                                        // triggers UNSTAKE
                                                                        BlockChain
                                                                                .loadChainFromFullNode(
                                                                                        url
                                                                                )
                                                                        walletBalance =
                                                                                url.getBalance(
                                                                                        validatorAddress
                                                                                )
                                                                        val validator =
                                                                                BlockChain
                                                                                        .validatorPool
                                                                                        .getValidator(
                                                                                                validatorAddress
                                                                                        )
                                                                        stakedAmount =
                                                                                validator
                                                                                        ?.stakedAmount
                                                                                        ?: 0.0
                                                                        connectionMessage =
                                                                                "✅ Successfully Withdrawn (Unstaked)"
                                                                } else {
                                                                        errorMessage =
                                                                                "Withdraw failed"
                                                                }
                                                        } catch (e: Exception) {
                                                                errorMessage = "Error: ${e.message}"
                                                        }
                                                },
                                                enabled = stakedAmount > 0
                                        ) { Text("Withdraw (Unstake)") }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(16.dp))

                                // 2. Node Operation (Start/Stop)
                                Text(
                                        text = "⚙️ Node Operation",
                                        style = MaterialTheme.typography.h6,
                                        fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row {
                                        Button(
                                                onClick = {
                                                        try {
                                                                val url = URL(peerAddress)
                                                                val publicKeyFile =
                                                                        File(
                                                                                selectedPublicKeyFilePath
                                                                        )
                                                                val requiredStake =
                                                                        BlockChain.getNetworkRules()
                                                                                .minFullStake

                                                                if (stakedAmount < requiredStake) {
                                                                        errorMessage =
                                                                                "Insufficient stake. Need at least $requiredStake KNT. Please Deposit first."
                                                                        return@Button
                                                                }

                                                                if (url.startValidating(
                                                                                publicKeyFile
                                                                        )
                                                                ) {
                                                                        validationState =
                                                                                ValidatorState
                                                                                        .VALIDATING
                                                                        connectionMessage =
                                                                                "🚀 Node Started! Validating blocks..."

                                                                        // Start Validation Loop
                                                                        Thread {
                                                                                        val wallet =
                                                                                                Wallet(
                                                                                                        File(
                                                                                                                selectedPrivateKeyFilePath
                                                                                                        ),
                                                                                                        File(
                                                                                                                selectedPublicKeyFilePath
                                                                                                        )
                                                                                                )
                                                                                        while (validationState ==
                                                                                                ValidatorState
                                                                                                        .VALIDATING) {
                                                                                                try {
                                                                                                        Thread.sleep(
                                                                                                                5000
                                                                                                        )
                                                                                                        val data =
                                                                                                                kokonut.core
                                                                                                                        .Data(
                                                                                                                                comment =
                                                                                                                                        "Validated by LightNode"
                                                                                                                        )
                                                                                                        val block =
                                                                                                                BlockChain
                                                                                                                        .validate(
                                                                                                                                wallet,
                                                                                                                                data
                                                                                                                        )

                                                                                                        @OptIn(
                                                                                                                kotlinx.serialization
                                                                                                                        .ExperimentalSerializationApi::class
                                                                                                        )
                                                                                                        url.addBlock(
                                                                                                                Json.encodeToJsonElement(
                                                                                                                        serializer<
                                                                                                                                Block>(),
                                                                                                                        block
                                                                                                                ),
                                                                                                                File(
                                                                                                                        selectedPublicKeyFilePath
                                                                                                                )
                                                                                                        )

                                                                                                        BlockChain
                                                                                                                .loadChainFromFullNode(
                                                                                                                        url
                                                                                                                )
                                                                                                        val newBalance =
                                                                                                                url.getBalance(
                                                                                                                        validatorAddress
                                                                                                                )
                                                                                                        walletBalance =
                                                                                                                newBalance

                                                                                                        // Refresh UI Info
                                                                                                        val rewardTx =
                                                                                                                block.data
                                                                                                                        .transactions
                                                                                                                        .find {
                                                                                                                                it.transaction ==
                                                                                                                                        "VALIDATOR_REWARD"
                                                                                                                        }
                                                                                                        connectionMessage =
                                                                                                                "✅ Block #${block.index} validated! Reward: ${rewardTx?.remittance ?: 0.0} KNT"
                                                                                                } catch (
                                                                                                        e:
                                                                                                                Exception) {
                                                                                                        if (e.message
                                                                                                                        ?.contains(
                                                                                                                                "Chain is Invalid"
                                                                                                                        ) ==
                                                                                                                        true ||
                                                                                                                        e.message
                                                                                                                                ?.contains(
                                                                                                                                        "No Fuel Nodes found"
                                                                                                                                ) ==
                                                                                                                                true
                                                                                                        ) {
                                                                                                                validationState =
                                                                                                                        ValidatorState
                                                                                                                                .FAILED
                                                                                                                connectionMessage =
                                                                                                                        "❌ Stopped: ${e.message}"
                                                                                                        }
                                                                                                        // Else: just failed to get selected,
                                                                                                        // wait for next turn.
                                                                                                }
                                                                                        }
                                                                                }
                                                                                .start()
                                                                } else {
                                                                        errorMessage =
                                                                                "Failed to start node session on server."
                                                                }
                                                        } catch (e: Exception) {
                                                                errorMessage = "Error: ${e.message}"
                                                        }
                                                },
                                                enabled =
                                                        validationState !=
                                                                ValidatorState.VALIDATING &&
                                                                stakedAmount >=
                                                                        BlockChain.getNetworkRules()
                                                                                .minFullStake
                                        ) { Text("▶ Start Node") }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                                onClick = {
                                                        validationState = ValidatorState.READY
                                                        connectionMessage =
                                                                "⏸ Node Stopped (Stake remains locked)"
                                                },
                                                enabled =
                                                        validationState == ValidatorState.VALIDATING
                                        ) { Text("⏹ Stop Node") }
                                }
                        }

                        // Dialogs
                        if (showKeyGenDialog) {
                                AlertDialog(
                                        onDismissRequest = { showKeyGenDialog = false },
                                        title = { Text("Keys Generated") },
                                        text = {
                                                Text("Keys saved to:\n$selectedPrivateKeyFilePath")
                                        },
                                        confirmButton = {
                                                Button(onClick = { showKeyGenDialog = false }) {
                                                        Text("OK")
                                                }
                                        }
                                )
                        }

                        if (errorMessage != null) {
                                AlertDialog(
                                        onDismissRequest = { errorMessage = null },
                                        title = { Text("Notice") },
                                        text = { Text(errorMessage!!) },
                                        confirmButton = {
                                                Button(onClick = { errorMessage = null }) {
                                                        Text("OK")
                                                }
                                        }
                                )
                        }
                }
        }
}

@Composable
fun InfoRow(label: String, value: String) {
        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                Text(
                        text = "$label: ",
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(140.dp)
                )
                Text(text = value)
        }
}

fun main() = application {
        Window(onCloseRequest = ::exitApplication, title = "Kokonut Lightnode") { App() }
}
