import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import kokonut.state.MiningState
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

    var minerID by remember { mutableStateOf("") }

    var miningState by remember { mutableStateOf(MiningState.READY) }

    var showSuccessDialog by remember { mutableStateOf(false) }

    MaterialTheme {
        Column {
            Row { Text(if (minerID.isNotEmpty()) miningState.toString() else "") }

            Row {
                Text("Miner ID : ")
                Text(minerID)
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

            Button(
                    onClick = {
                        // BlockChain.initialize(NodeType.LIGHT)
                        val wallet =
                                Wallet(
                                        privateKeyFile = File(selectedPrivateKeyFilePath),
                                        publicKeyFile = File(selectedPublicKeyFilePath)
                                )

                        if (wallet.isValid()) {
                            showSuccessDialog = true
                            minerID = wallet.miner
                            miningState = wallet.miningState
                        }
                    }
            ) { Text("Login") }

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
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Kokonut Lightnode") { App() }
}
