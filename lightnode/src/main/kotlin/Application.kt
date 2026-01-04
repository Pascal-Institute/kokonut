import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

/**
 * 🥥 Kokonut Light Node Application
 *
 * Main application entry point, combining state management and UI sections.
 *
 * File Structure:
 * - Application.kt: Main app entry point (current file)
 * - AppState.kt: App state management class
 * - AppSections.kt: UI Composable functions by section
 */
@Composable
@Preview
fun App() {
        // Initialize app state
        val state = remember { AppState() }

        // Perform cleanup on app dispose
        DisposableEffect(Unit) { onDispose { performCleanup(state, state.peerAddress) } }

        // UI Composition
        MaterialTheme {
                Column(modifier = Modifier.padding(16.dp)) {
                        // Header Section (Title, Connection State, Node State)
                        HeaderSection(state)

                        // Wallet Info Section (Shown only when connected)
                        WalletInfoSection(state)

                        // Connection Section (URL Input, Connect Button, Chain Sync)
                        ConnectionSection(state)

                        // Key Management Section (Load Public/Private Keys, Generate Keys)
                        KeyManagementSection(state)

                        // Staking Management Section (Shown only when connected)
                        StakingManagementSection(state)

                        // Node Operation Section (Shown only when connected)
                        NodeOperationSection(state)

                        // Dialogs Section (Key Gen Complete, Error Messages)
                        DialogsSection(state)
                }
        }
}

/** App Main Function */
fun main() = application {
        Window(onCloseRequest = ::exitApplication, title = "Kokonut Lightnode") { App() }
}
