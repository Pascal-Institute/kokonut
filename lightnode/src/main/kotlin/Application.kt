import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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

@Composable
@Preview
fun App() {
    var selectedPublicKeyFilePath by remember { mutableStateOf<String?>("Please load a public key...") }
    var selectedPrivateKeyFilePath by remember { mutableStateOf<String?>("Please load a private key...") }

    MaterialTheme {
        Column {
            Row {
                Text(
                    modifier = Modifier.width(250.dp).height(25.dp),
                    text = selectedPublicKeyFilePath.toString()
                )

                Button(onClick = {
                    val fileDialog = FileDialog(Frame(), "Select a File", FileDialog.LOAD)
                    fileDialog.isVisible = true
                    val selectedFile = fileDialog.file
                    if (selectedFile != null) {
                        selectedPublicKeyFilePath = File(fileDialog.directory, selectedFile).absolutePath
                    }
                }) {
                    Text("Load...")
                }
            }

            Row {
                Text(
                    modifier = Modifier.width(250.dp).height(25.dp),
                    text = selectedPrivateKeyFilePath.toString()
                )

                Button(onClick = {
                    val fileDialog = FileDialog(Frame(), "Select a File", FileDialog.LOAD)
                    fileDialog.isVisible = true
                    val selectedFile = fileDialog.file
                    if (selectedFile != null) {
                        selectedPrivateKeyFilePath = File(fileDialog.directory, selectedFile).absolutePath
                    }
                }) {
                    Text("Load...")
                }
            }

            Button(onClick = {
                //TODO wallet login logic
            }) {
                Text("Login")
            }
        }
    }
}

fun main() = application {

    Window(onCloseRequest = ::exitApplication, title = "Kokonut Lightnode") {
        App()
    }
}
