import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

// Define Colors
private val KokonutGreen = Color(0xFF2ECC71)
private val KokonutDarkGreen = Color(0xFF27AE60)
private val KokonutBlue = Color(0xFF3498DB)
private val KokonutRed = Color(0xFFE74C3C)
private val KokonutDark = Color(0xFF2C3E50)
private val KokonutBackground = Color(0xFFF8F9FA)

private val KokonutColors =
        lightColors(
                primary = KokonutGreen,
                primaryVariant = KokonutDarkGreen,
                secondary = KokonutBlue,
                background = KokonutBackground,
                surface = Color.White,
                error = KokonutRed,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = KokonutDark,
                onSurface = KokonutDark
        )

@Composable
fun KokonutTheme(content: @Composable () -> Unit) {
        MaterialTheme(
                colors = KokonutColors,
                typography = MaterialTheme.typography,
                shapes = MaterialTheme.shapes,
                content = content
        )
}

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
        KokonutTheme {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background
                ) {
                        Column(modifier = Modifier.padding(24.dp)) {
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
}

/** App Main Function */
fun main() = application {
        Window(onCloseRequest = ::exitApplication, title = "Kokonut Lightnode") { App() }
}
