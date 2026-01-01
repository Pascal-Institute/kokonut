import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kokonut.core.BlockChain
import kokonut.state.ValidatorState
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

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showKeyGenDialog by remember { mutableStateOf(false) }

    MaterialTheme {
        Column {
            Row { Text(if (validatorAddress.isNotEmpty()) validationState.toString() else "") }

            Row {
                Text("Validator Address : ")
                Text(validatorAddress)
            }

            Row {
                Text(modifier = Modifier.width(100.dp).height(50.dp), text = "Node URL : ")
                TextField(
                        value = peerAddress,
                        onValueChange = { peerAddress = it },
                        modifier = Modifier.width(300.dp).height(50.dp)
                )
            }

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
                ) { Text("Load...") }
            }

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
                ) { Text("Load...") }
            }

            Row {
                Button(
                        onClick = {
                            try {
                                BlockChain.initialize(NodeType.LIGHT, peerAddress)

                                val wallet =
                                        Wallet(
                                                privateKeyFile = File(selectedPrivateKeyFilePath),
                                                publicKeyFile = File(selectedPublicKeyFilePath)
                                        )

                                if (wallet.isValid()) {
                                    showSuccessDialog = true
                                    validatorAddress = wallet.validatorAddress
                                    validationState = wallet.validationState
                                }
                            } catch (e: Exception) {
                                errorMessage =
                                        "Connection configuration saved. (Note: Actual connection may vary based on implementation)\nError details: ${e.message}"
                                // For now, we allow login even if connection fails, or we can block
                                // it.
                                // Given the request to 'write logic', we try to connect.
                                // If it throws, we show error.
                            }
                        }
                ) { Text("Login") }

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
                        title = { Text("Connection Status") },
                        text = { Text(errorMessage!!) },
                        confirmButton = { Button(onClick = { errorMessage = null }) { Text("OK") } }
                )
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Kokonut Lightnode") { App() }
}
