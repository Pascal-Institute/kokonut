import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kokonut.core.NetworkInfo
import kokonut.state.ValidatorState

/**
 * Class that manages the state of the Light Node application. Centrally manages all UI states and
 * network connection states.
 */
class AppState {
    // Key file paths
    var selectedPublicKeyFilePath by mutableStateOf<String?>("Please load a public key...")
    var selectedPrivateKeyFilePath by mutableStateOf<String?>("Please load a private key...")

    // Validator & Peer
    var validatorAddress by mutableStateOf("")
    var peerAddress by mutableStateOf("http://127.0.0.1:80")
    var errorMessage by mutableStateOf<String?>(null)

    // Validation state
    var validationState by mutableStateOf(ValidatorState.READY)

    // Connection state
    var isConnected by mutableStateOf(false)
    var networkInfo by mutableStateOf<NetworkInfo?>(null)
    var connectionMessage by mutableStateOf<String?>(null)
    
    // WebSocket connection state
    var isWebSocketConnected by mutableStateOf(false)
    var webSocketStatus by mutableStateOf("Not connected")

    // Dialog visibility
    var showKeyGenDialog by mutableStateOf(false)

    // Wallet balance & Stake state
    var walletBalance by mutableStateOf(0.0)
    var stakedAmount by mutableStateOf(0.0)
    var depositAmountInput by mutableStateOf("10.0")

    // Chain sync status
    var chainSyncStatus by mutableStateOf<String?>(null)
    var isChainOutOfSync by mutableStateOf(false)
    var localChainSize by mutableStateOf(0)
    var remoteChainSize by mutableStateOf(0)

    /** Checks if key files are loaded. */
    val keysNotLoaded: Boolean
        get() =
                selectedPublicKeyFilePath == "Please load a public key..." ||
                        selectedPrivateKeyFilePath == "Please load a private key..."

    /** Resets the connection state. */
    fun resetConnectionState() {
        isConnected = false
        networkInfo = null
        validatorAddress = ""
        walletBalance = 0.0
        stakedAmount = 0.0
        chainSyncStatus = null
        isChainOutOfSync = false
    }
}
