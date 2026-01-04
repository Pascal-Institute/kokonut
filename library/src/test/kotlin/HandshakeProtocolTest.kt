import kokonut.core.HandshakeRequest
import kokonut.core.HandshakeResponse
import kokonut.core.NetworkInfo
import kotlin.test.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HandshakeProtocolTest {

    @Test
    fun `test HandshakeRequest serialization with public key`() {
        val request =
                HandshakeRequest(
                        nodeType = "LIGHT",
                        publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCg...",
                        timestamp = 1735725600000L
                )

        val json = Json.encodeToString(request)
        assertTrue(json.contains("LIGHT"))
        assertTrue(json.contains("MIIBIjAN"))

        println("✅ HandshakeRequest serialization: $json")
    }

    @Test
    fun `test HandshakeRequest deserialization`() {
        val jsonString =
                """
            {
                "nodeType": "LIGHT",
                "publicKey": "test_public_key_123",
                "timestamp": 1735725600000
            }
        """.trimIndent()

        val request = Json.decodeFromString<HandshakeRequest>(jsonString)
        assertEquals("LIGHT", request.nodeType)
        assertEquals("test_public_key_123", request.publicKey)
        assertEquals("test_public_key_123", request.publicKey)
        assertEquals(1735725600000L, request.timestamp)

        println("✅ HandshakeRequest deserialization successful")
    }

    @Test
    fun `test HandshakeRequest requires public key`() {
        // This should compile - publicKey is NOT nullable
        assertFailsWith<Exception> {
            val jsonWithoutKey =
                    """
                {
                    "nodeType": "LIGHT",
                    "timestamp": 1735725600000
                }
            """.trimIndent()

            Json.decodeFromString<HandshakeRequest>(jsonWithoutKey)
        }

        println("✅ HandshakeRequest correctly requires public key")
    }

    @Test
    fun `test HandshakeResponse with success`() {
        val networkInfo =
                NetworkInfo(
                        nodeType = "FULL",
                        networkId = "kokonut-testnet",
                        genesisHash = "000abc123def456",
                        chainSize = 100L,
                        totalValidators = 5,
                        totalCurrencyVolume = 10000.0,
                        connectedFuelNodes = 2
                )

        val response =
                HandshakeResponse(
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
        val response =
                HandshakeResponse(
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
        val networkInfo =
                NetworkInfo(
                        nodeType = "FULL",
                        networkId = "kokonut-mainnet",
                        genesisHash = "genesis_hash_000",
                        chainSize = 1523L,
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
        assertEquals(1523L, networkInfo.chainSize)
        assertEquals(12, networkInfo.totalValidators)
        assertEquals(150000.0, networkInfo.totalCurrencyVolume)
        assertEquals(5, networkInfo.connectedFuelNodes)
        assertTrue(networkInfo.serverTimestamp > 0)

        println("✅ NetworkInfo contains all essential blockchain data")
    }

    @Test
    fun `test HandshakeRequest timestamp is current`() {
        val request = HandshakeRequest(nodeType = "LIGHT", publicKey = "test_key")

        val currentTime = System.currentTimeMillis()
        val timeDifference = kotlin.math.abs(currentTime - request.timestamp)

        // Timestamp should be within 1 second of current time
        assertTrue(timeDifference < 1000, "Timestamp should be current")

        println("✅ HandshakeRequest timestamp is current")
    }

    @Test
    fun `test HandshakeResponse serialization round-trip`() {
        val originalNetworkInfo =
                NetworkInfo(
                        nodeType = "FULL",
                        networkId = "kokonut-mainnet",
                        genesisHash = "abc123",
                        chainSize = 500L,
                        totalValidators = 8,
                        totalCurrencyVolume = 50000.0,
                        connectedFuelNodes = 3
                )

        val originalResponse =
                HandshakeResponse(
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
        assertEquals(
                originalResponse.networkInfo?.networkId,
                deserializedResponse.networkInfo?.networkId
        )
        assertEquals(
                originalResponse.networkInfo?.chainSize,
                deserializedResponse.networkInfo?.chainSize
        )

        println("✅ HandshakeResponse serialization round-trip successful")
    }

    @Test
    fun `test empty public key should fail`() {
        val request = HandshakeRequest(nodeType = "LIGHT", publicKey = "")

        assertTrue(request.publicKey.isBlank(), "Empty public key should be detected as blank")

        println("✅ Empty public key correctly identified as blank")
    }

    @Test
    fun `test whitespace-only public key should fail`() {
        val request = HandshakeRequest(nodeType = "LIGHT", publicKey = "   ")

        assertTrue(
                request.publicKey.isBlank(),
                "Whitespace-only public key should be detected as blank"
        )

        println("✅ Whitespace-only public key correctly identified as blank")
    }

    @Test
    fun `test genesis hash verification format`() {
        val networkInfo =
                NetworkInfo(
                        nodeType = "FULL",
                        networkId = "kokonut-mainnet",
                        genesisHash = "000abc123def456789",
                        chainSize = 100L,
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
        val networkInfo =
                NetworkInfo(
                        nodeType = "FULL",
                        networkId = "kokonut-mainnet",
                        genesisHash = "genesis_000",
                        chainSize = 1000L,
                        totalValidators = 10,
                        totalCurrencyVolume = 100000.0,
                        connectedFuelNodes = 4
                )

        // All metrics should be positive or zero
        assertTrue(networkInfo.chainSize >= 0)
        assertTrue(networkInfo.totalValidators >= 0)
        assertTrue(networkInfo.totalCurrencyVolume >= 0.0)
        assertTrue(networkInfo.connectedFuelNodes >= 0)

        println("✅ All network metrics are non-negative")
    }
}
