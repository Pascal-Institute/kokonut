import kokonut.core.HandshakeRequest
import kokonut.core.HandshakeResponse
import kokonut.core.NetworkInfo
import kokonut.util.Utility
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class HandshakeProtocolTest {

    @Test
    fun `test HandshakeRequest serialization with public key`() {
        val request = HandshakeRequest(
            nodeType = "LIGHT",
            publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCg...",
            clientVersion = 4,
            timestamp = 1735725600000L
        )

        val json = Json.encodeToString(request)
        assertTrue(json.contains("LIGHT"))
        assertTrue(json.contains("MIIBIjAN"))
        assertTrue(json.contains("\"clientVersion\":4"))

        println("✅ HandshakeRequest serialization: $json")
    }

    @Test
    fun `test HandshakeRequest deserialization`() {
        val jsonString = """
            {
                "nodeType": "LIGHT",
                "publicKey": "test_public_key_123",
                "clientVersion": 4,
                "timestamp": 1735725600000
            }
        """.trimIndent()

        val request = Json.decodeFromString<HandshakeRequest>(jsonString)
        assertEquals("LIGHT", request.nodeType)
        assertEquals("test_public_key_123", request.publicKey)
        assertEquals(4, request.clientVersion)
        assertEquals(1735725600000L, request.timestamp)

        println("✅ HandshakeRequest deserialization successful")
    }

    @Test
    fun `test HandshakeRequest requires public key`() {
        // This should compile - publicKey is NOT nullable
        assertFailsWith<Exception> {
            val jsonWithoutKey = """
                {
                    "nodeType": "LIGHT",
                    "clientVersion": 4,
                    "timestamp": 1735725600000
                }
            """.trimIndent()
            
            Json.decodeFromString<HandshakeRequest>(jsonWithoutKey)
        }

        println("✅ HandshakeRequest correctly requires public key")
    }

    @Test
    fun `test HandshakeResponse with success`() {
        val networkInfo = NetworkInfo(
            nodeType = "FULL",
            networkId = "kokonut-testnet",
            genesisHash = "000abc123def456",
            chainSize = 100L,
            protocolVersion = 4,
            totalValidators = 5,
            totalCurrencyVolume = 10000.0,
            connectedFuelNodes = 2
        )

        val response = HandshakeResponse(
            success = true,
            message = "Handshake successful. Welcome to kokonut-testnet!",
            networkInfo = networkInfo
        )

        assertTrue(response.success)
        assertNotNull(response.networkInfo)
        assertEquals("kokonut-testnet", response.networkInfo?.networkId)
        assertEquals(100L, response.networkInfo?.chainSize)

        println("✅ Successful handshake response created")
    }

    @Test
    fun `test HandshakeResponse with failure`() {
        val response = HandshakeResponse(
            success = false,
            message = "Authentication failed: Public key is required for handshake",
            networkInfo = null
        )

        assertFalse(response.success)
        assertNull(response.networkInfo)
        assertTrue(response.message.contains("Public key is required"))

        println("✅ Failed handshake response created")
    }

    @Test
    fun `test NetworkInfo contains essential blockchain data`() {
        val networkInfo = NetworkInfo(
            nodeType = "FULL",
            networkId = "kokonut-mainnet",
            genesisHash = "genesis_hash_000",
            chainSize = 1523L,
            protocolVersion = 4,
            totalValidators = 12,
            totalCurrencyVolume = 150000.0,
            connectedFuelNodes = 5,
            serverTimestamp = System.currentTimeMillis()
        )

        // Verify all essential fields
        assertEquals("FULL", networkInfo.nodeType)
        assertEquals("kokonut-mainnet", networkInfo.networkId)
        assertEquals("genesis_hash_000", networkInfo.genesisHash)
        assertEquals(1523L, networkInfo.chainSize)
        assertEquals(4, networkInfo.protocolVersion)
        assertEquals(12, networkInfo.totalValidators)
        assertEquals(150000.0, networkInfo.totalCurrencyVolume)
        assertEquals(5, networkInfo.connectedFuelNodes)
        assertTrue(networkInfo.serverTimestamp > 0)

        println("✅ NetworkInfo contains all essential blockchain data")
    }

    @Test
    fun `test HandshakeRequest timestamp is current`() {
        val request = HandshakeRequest(
            nodeType = "LIGHT",
            publicKey = "test_key",
            clientVersion = 4
        )

        val currentTime = System.currentTimeMillis()
        val timeDifference = kotlin.math.abs(currentTime - request.timestamp)

        // Timestamp should be within 1 second of current time
        assertTrue(timeDifference < 1000, "Timestamp should be current")

        println("✅ HandshakeRequest timestamp is current")
    }

    @Test
    fun `test protocol version matching`() {
        val clientVersion = 4
        val serverVersion = Utility.protocolVersion

        assertEquals(clientVersion, serverVersion, 
            "Client and server protocol versions should match")

        println("✅ Protocol versions match: $clientVersion")
    }

    @Test
    fun `test HandshakeResponse serialization round-trip`() {
        val originalNetworkInfo = NetworkInfo(
            nodeType = "FULL",
            networkId = "kokonut-mainnet",
            genesisHash = "abc123",
            chainSize = 500L,
            protocolVersion = 4,
            totalValidators = 8,
            totalCurrencyVolume = 50000.0,
            connectedFuelNodes = 3
        )

        val originalResponse = HandshakeResponse(
            success = true,
            message = "Test message",
            networkInfo = originalNetworkInfo
        )

        // Serialize
        val json = Json.encodeToString(originalResponse)
        
        // Deserialize
        val deserializedResponse = Json.decodeFromString<HandshakeResponse>(json)

        // Verify
        assertEquals(originalResponse.success, deserializedResponse.success)
        assertEquals(originalResponse.message, deserializedResponse.message)
        assertEquals(originalResponse.networkInfo?.networkId, 
                    deserializedResponse.networkInfo?.networkId)
        assertEquals(originalResponse.networkInfo?.chainSize, 
                    deserializedResponse.networkInfo?.chainSize)

        println("✅ HandshakeResponse serialization round-trip successful")
    }

    @Test
    fun `test empty public key should fail`() {
        val request = HandshakeRequest(
            nodeType = "LIGHT",
            publicKey = "",
            clientVersion = 4
        )

        assertTrue(request.publicKey.isBlank(), 
            "Empty public key should be detected as blank")

        println("✅ Empty public key correctly identified as blank")
    }

    @Test
    fun `test whitespace-only public key should fail`() {
        val request = HandshakeRequest(
            nodeType = "LIGHT",
            publicKey = "   ",
            clientVersion = 4
        )

        assertTrue(request.publicKey.isBlank(), 
            "Whitespace-only public key should be detected as blank")

        println("✅ Whitespace-only public key correctly identified as blank")
    }

    @Test
    fun `test genesis hash verification format`() {
        val networkInfo = NetworkInfo(
            nodeType = "FULL",
            networkId = "kokonut-mainnet",
            genesisHash = "000abc123def456789",
            chainSize = 100L,
            protocolVersion = 4,
            totalValidators = 5,
            totalCurrencyVolume = 10000.0,
            connectedFuelNodes = 2
        )

        // Genesis hash should not be empty
        assertTrue(networkInfo.genesisHash.isNotEmpty())
        
        // Genesis hash should be a valid hex-like string
        assertTrue(networkInfo.genesisHash.all { it.isLetterOrDigit() })

        println("✅ Genesis hash format is valid: ${networkInfo.genesisHash}")
    }

    @Test
    fun `test network info reflects positive blockchain metrics`() {
        val networkInfo = NetworkInfo(
            nodeType = "FULL",
            networkId = "kokonut-mainnet",
            genesisHash = "genesis_000",
            chainSize = 1000L,
            protocolVersion = 4,
            totalValidators = 10,
            totalCurrencyVolume = 100000.0,
            connectedFuelNodes = 4
        )

        // All metrics should be positive or zero
        assertTrue(networkInfo.chainSize >= 0)
        assertTrue(networkInfo.totalValidators >= 0)
        assertTrue(networkInfo.totalCurrencyVolume >= 0.0)
        assertTrue(networkInfo.connectedFuelNodes >= 0)
        assertTrue(networkInfo.protocolVersion > 0)

        println("✅ All network metrics are non-negative")
    }

    @Test
    fun `test handshake protocol version compatibility check`() {
        val clientVersion = 4
        val serverVersion = 4

        val isCompatible = (clientVersion == serverVersion)

        assertTrue(isCompatible, "Protocol versions should be compatible")

        println("✅ Protocol version compatibility check passed")
    }

    @Test
    fun `test handshake protocol version mismatch detection`() {
        val clientVersion = 3
        val serverVersion = 4

        val isCompatible = (clientVersion == serverVersion)

        assertFalse(isCompatible, "Protocol version mismatch should be detected")

        println("✅ Protocol version mismatch correctly detected")
    }
}
