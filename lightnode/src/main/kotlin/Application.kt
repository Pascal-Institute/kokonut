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
import kokonut.util.API.Companion.performHandshake
import kokonut.util.API.Companion.stakeLock
import kokonut.util.API.Companion.startValidating
import kokonut.util.NodeType
import kokonut.util.Wallet

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

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showKeyGenDialog by remember { mutableStateOf(false) }

    // Wallet balance state
    var walletBalance by remember { mutableStateOf(0.0) }

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
                        text = if (isConnected) "✅ Connected" else "⭕ Not Connected",
                        color = if (isConnected) Color.Green else Color.Gray
                )
            }

            // Validator State
            Row {
                if (validatorAddress.isNotEmpty()) {
                    Text(text = "Validator State: ", fontWeight = FontWeight.Bold)
                    Text(text = validationState.toString())
                }
            }

            // Validator Address
            if (validatorAddress.isNotEmpty()) {
                Row {
                    Text(text = "Validator Address: ", fontWeight = FontWeight.Bold)
                    Text(text = validatorAddress)
                }
            }

            // Wallet Balance (shown when connected and logged in)
            if (isConnected && validatorAddress.isNotEmpty()) {
                Row {
                    Text(text = "💰 Balance: ", fontWeight = FontWeight.Bold)
                    Text(
                            text = "$walletBalance KNT",
                            color = if (walletBalance > 0) Color(0xFF4CAF50) else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Node URL Input
            Row {
                Text(modifier = Modifier.width(100.dp).height(50.dp), text = "Node URL: ")
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
                        text = "⚠️ Please load your public and private keys before connecting",
                        color = Color(0xFFFF9800),
                        modifier = Modifier.padding(8.dp)
                )
            }

            // Connect Button
            Button(
                    onClick = {
                        try {
                            // Validate keys are loaded
                            if (selectedPublicKeyFilePath == "Please load a public key..." ||
                                            selectedPrivateKeyFilePath ==
                                                    "Please load a private key..."
                            ) {
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
                                            privateKeyFile = File(selectedPrivateKeyFilePath),
                                            publicKeyFile = File(selectedPublicKeyFilePath)
                                    )

                            val publicKeyString = wallet.publicKey.toString()

                            // Perform handshake with public key (REQUIRED)
                            val response = url.performHandshake(publicKeyString)

                            if (response.success && response.networkInfo != null) {
                                isConnected = true
                                networkInfo = response.networkInfo
                                connectionMessage = "✅ ${response.message}"

                                // Initialize blockchain
                                BlockChain.initialize(NodeType.LIGHT, peerAddress)

                                // Force sync from the connected Full Node so the local DB sees
                                // any new blocks (e.g., onboarding) created during handshake.
                                BlockChain.loadChainFromFullNode(url)
                                networkInfo =
                                        networkInfo?.copy(chainSize = BlockChain.getChainSize())

                                // Auto-load validator address
                                validatorAddress = wallet.validatorAddress
                                validationState = wallet.validationState

                                // Fetch wallet balance from Full Node
                                println("🔍 Querying balance for address: $validatorAddress")
                                walletBalance = url.getBalance(validatorAddress)
                                println("💰 Retrieved balance: $walletBalance KNT")
                            } else {
                                isConnected = false
                                networkInfo = null
                                connectionMessage = "❌ ${response.message}"
                            }
                        } catch (e: IllegalArgumentException) {
                            isConnected = false
                            networkInfo = null
                            connectionMessage = "❌ ${e.message}"
                        } catch (e: Exception) {
                            isConnected = false
                            networkInfo = null
                            connectionMessage = "❌ Connection failed: ${e.message}"
                        }
                    },
                    enabled = !keysNotLoaded, // Disable button if keys not loaded
                    modifier = Modifier.fillMaxWidth()
            ) { Text("🤝 Connect to Full Node") }

            // Connection Message
            if (connectionMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                        text = connectionMessage!!,
                        color = if (isConnected) Color.Green else Color.Red,
                        modifier = Modifier.padding(8.dp)
                )
            }

            // Network Info Display
            if (networkInfo != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text = "📊 Network Information",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.padding(start = 8.dp)) {
                    InfoRow("Network ID", networkInfo!!.networkId)
                    InfoRow("Genesis Hash", networkInfo!!.genesisHash.take(16) + "...")
                    InfoRow("Chain Size", "${networkInfo!!.chainSize} blocks")
                    InfoRow("Total Validators", "${networkInfo!!.totalValidators}")
                    InfoRow("Total KNT", "${networkInfo!!.totalCurrencyVolume}")
                    InfoRow("Fuel Nodes", "${networkInfo!!.connectedFuelNodes}")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Key Management Section
            Text(
                    text = "🔐 Key Management",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Public Key
            Row {
                Text(
                        modifier = Modifier.width(250.dp).height(25.dp),
                        text = selectedPublicKeyFilePath.toString()
                )

                Button(
                        onClick = {
                            val fileDialog = FileDialog(Frame(), "Select a File", FileDialog.LOAD)
                            fileDialog.isVisible = true
                            val selectedFile = fileDialog.file
                            if (selectedFile != null) {
                                selectedPublicKeyFilePath =
                                        File(fileDialog.directory, selectedFile).absolutePath
                            }
                        }
                ) { Text("Load Public Key") }
            }

            // Private Key
            Row {
                Text(
                        modifier = Modifier.width(250.dp).height(25.dp),
                        text = selectedPrivateKeyFilePath.toString()
                )

                Button(
                        onClick = {
                            val fileDialog = FileDialog(Frame(), "Select a File", FileDialog.LOAD)
                            fileDialog.isVisible = true
                            val selectedFile = fileDialog.file
                            if (selectedFile != null) {
                                selectedPrivateKeyFilePath =
                                        File(fileDialog.directory, selectedFile).absolutePath
                            }
                        }
                ) { Text("Load Private Key") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row {
                Button(
                        onClick = {
                            try {
                                if (!isConnected) {
                                    errorMessage = "⚠️ Please connect to a Full Node first"
                                    return@Button
                                }

                                val wallet =
                                        Wallet(
                                                privateKeyFile = File(selectedPrivateKeyFilePath),
                                                publicKeyFile = File(selectedPublicKeyFilePath)
                                        )

                                if (wallet.isValid()) {
                                    showSuccessDialog = true
                                    validatorAddress = wallet.validatorAddress
                                    validationState = wallet.validationState

                                    // Fetch wallet balance from Full Node
                                    val url = URL(peerAddress)
                                    walletBalance = url.getBalance(validatorAddress)
                                }
                            } catch (e: Exception) {
                                errorMessage = "❌ Login failed: ${e.message}"
                            }
                        }
                ) { Text("Login") }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                        onClick = {
                            try {
                                if (!isConnected) {
                                    errorMessage = "⚠️ Please connect to a Full Node first"
                                    return@Button
                                }

                                if (selectedPublicKeyFilePath == "Please load a public key..." ||
                                                selectedPrivateKeyFilePath ==
                                                        "Please load a private key..."
                                ) {
                                    errorMessage = "⚠️ Please load your keys first"
                                    return@Button
                                }

                                val wallet =
                                        Wallet(
                                                privateKeyFile = File(selectedPrivateKeyFilePath),
                                                publicKeyFile = File(selectedPublicKeyFilePath)
                                        )

                                val url = URL(peerAddress)
                                val requiredStake = BlockChain.getNetworkRules().minFullStake
                                val currentStake =
                                        BlockChain.validatorPool.getValidator(
                                                        wallet.validatorAddress
                                                )
                                                ?.stakedAmount
                                                ?: 0.0

                                if (currentStake < requiredStake) {
                                    val toLock = requiredStake - currentStake
                                    if (!url.stakeLock(
                                                    wallet,
                                                    File(selectedPublicKeyFilePath),
                                                    toLock
                                            )
                                    ) {
                                        errorMessage = "❌ Stake lock failed"
                                        return@Button
                                    }

                                    // Sync to observe the newly appended stake lock block.
                                    BlockChain.loadChainFromFullNode(url)
                                }

                                if (url.startValidating(File(selectedPublicKeyFilePath))) {
                                    validationState = ValidatorState.VALIDATING
                                    connectionMessage = "✅ Staking Started! Validating blocks..."

                                    // Launch Staking Loop in a separate thread
                                    Thread {
                                                while (validationState ==
                                                        ValidatorState.VALIDATING) {
                                                    try {
                                                        Thread.sleep(5000) // 5 seconds block time

                                                        // 1. Create Data
                                                        val data =
                                                                kokonut.core.Data(
                                                                        comment =
                                                                                "Validated by LightNode"
                                                                )

                                                        // 2. Try to validate (create block)
                                                        // This calls API to sync chain and checks
                                                        // if we are selected
                                                        val block =
                                                                BlockChain.validate(wallet, data)

                                                        // 3. If successful, send to Full Node
                                                        url.addBlock(
                                                            kotlinx.serialization.json.Json
                                                                .encodeToJsonElement(
                                                                    Block.serializer(),
                                                                    block
                                                                ),
                                                            File(selectedPublicKeyFilePath)
                                                        )

                                                        // Update UI
                                                        networkInfo =
                                                                networkInfo?.copy(
                                                                        chainSize =
                                                                                networkInfo!!
                                                                                        .chainSize +
                                                                                        1
                                                                )
                                                    } catch (e: Exception) {
                                                        // Expected: Not selected, chain not ready,
                                                        // etc.
                                                        println("Staking: ${e.message}")
                                                    }
                                                }
                                            }
                                            .start()
                                }
                            } catch (e: Exception) {
                                errorMessage = "❌ Staking failed: ${e.message}"
                            }
                        },
                        // Enable if connected and wallet valid, but not already validating
                        enabled =
                                isConnected &&
                                        selectedPublicKeyFilePath !=
                                                "Please load a public key..." &&
                                        validationState != ValidatorState.VALIDATING
                ) { Text("🔨 Start Staking") }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                        onClick = {
                            val fileDialog =
                                    FileDialog(Frame(), "Save New Private Key", FileDialog.SAVE)
                            fileDialog.file = "private.pem"
                            fileDialog.isVisible = true
                            val selectedFile = fileDialog.file
                            if (selectedFile != null) {
                                val dir = fileDialog.directory
                                val privateKeyFile = File(dir, selectedFile)
                                val publicKeyFile = File(dir, "public.pem")

                                val keyPair = Wallet.generateKey()
                                Wallet.saveKeyPairToFile(
                                        keyPair,
                                        privateKeyFile.absolutePath,
                                        publicKeyFile.absolutePath
                                )

                                selectedPrivateKeyFilePath = privateKeyFile.absolutePath
                                selectedPublicKeyFilePath = publicKeyFile.absolutePath
                                showKeyGenDialog = true
                            }
                        }
                ) { Text("Generate Keys") }
            }

            // Dialogs
            if (showSuccessDialog) {
                AlertDialog(
                        onDismissRequest = { showSuccessDialog = false },
                        title = { Text("Login Succeed") },
                        text = { Text("You have successfully logged in!") },
                        confirmButton = {
                            Button(onClick = { showSuccessDialog = false }) { Text("OK") }
                        }
                )
            }

            if (showKeyGenDialog) {
                AlertDialog(
                        onDismissRequest = { showKeyGenDialog = false },
                        title = { Text("Keys Generated") },
                        text = {
                            Text(
                                    "New keys have been saved successfully!\nPrivate: $selectedPrivateKeyFilePath\nPublic: $selectedPublicKeyFilePath"
                            )
                        },
                        confirmButton = {
                            Button(onClick = { showKeyGenDialog = false }) { Text("OK") }
                        }
                )
            }

            if (errorMessage != null) {
                AlertDialog(
                        onDismissRequest = { errorMessage = null },
                        title = { Text("Notice") },
                        text = { Text(errorMessage!!) },
                        confirmButton = { Button(onClick = { errorMessage = null }) { Text("OK") } }
                )
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = "$label: ", fontWeight = FontWeight.Medium, modifier = Modifier.width(140.dp))
        Text(text = value)
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Kokonut Lightnode") { App() }
}
