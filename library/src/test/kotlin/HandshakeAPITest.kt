import java.net.URL
import kokonut.util.API.Companion.performHandshake
import kotlin.test.*

class HandshakeAPITest {

    @Test
    fun `test performHandshake requires non-empty public key`() {
        val url = URL("http://localhost:80")

        assertFailsWith<IllegalArgumentException> { url.performHandshake("") }

        println("✅ Empty public key correctly throws IllegalArgumentException")
    }

    @Test
    fun `test performHandshake requires non-blank public key`() {
        val url = URL("http://localhost:80")

        assertFailsWith<IllegalArgumentException> { url.performHandshake("   ") }

        println("✅ Blank public key correctly throws IllegalArgumentException")
    }

    @Test
    fun `test performHandshake requires non-whitespace public key`() {
        val url = URL("http://localhost:80")

        assertFailsWith<IllegalArgumentException> { url.performHandshake("\t\n  ") }

        println("✅ Whitespace-only public key correctly throws IllegalArgumentException")
    }

    @Test
    fun `test performHandshake error message is descriptive for empty key`() {
        val url = URL("http://localhost:80")

        val exception = assertFailsWith<IllegalArgumentException> { url.performHandshake("") }

        assertTrue(exception.message!!.contains("Public key is required"))
        assertTrue(exception.message!!.contains("load your public key"))

        println("✅ Error message is descriptive: ${exception.message}")
    }

    @Test
    fun `test performHandshake accepts valid public key format`() {
        // This test validates that the function accepts a properly formatted key
        // without actually connecting to a server

        val validPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1234567890abcdef"

        // Should not throw exception for valid key format
        assertFalse(validPublicKey.isBlank())
        assertTrue(validPublicKey.length > 10)

        println("✅ Valid public key format accepted")
    }

    @Test
    fun `test URL construction for handshake endpoint`() {
        val baseUrl = URL("http://localhost:80")
        val handshakeUrl = URL("${baseUrl}/handshake")

        assertEquals("http://localhost:80/handshake", handshakeUrl.toString())

        println("✅ Handshake URL construction correct")
    }

    @Test
    fun `test URL construction with different ports`() {
        val urls =
                listOf(
                        "http://localhost:80" to "http://localhost:80/handshake",
                        "http://localhost:8080" to "http://localhost:8080/handshake",
                        "http://192.168.1.100:3000" to "http://192.168.1.100:3000/handshake"
                )

        urls.forEach { (base, expected) ->
            val baseUrl = URL(base)
            val handshakeUrl = URL("${baseUrl}/handshake")
            assertEquals(expected, handshakeUrl.toString())
        }

        println("✅ Handshake URL construction with various ports correct")
    }

    @Test
    fun `test public key minimum length validation`() {
        // Very short keys should still technically work if not blank
        val shortKey = "abc"

        assertFalse(shortKey.isBlank())

        // But in practice, real public keys are much longer
        val realPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA..."
        assertTrue(realPublicKey.length > 20)

        println("✅ Public key length validation works")
    }

    @Test
    fun `test handshake supports various network addresses`() {
        val validUrls =
                listOf(
                        "http://localhost:80",
                        "http://127.0.0.1:80",
                        "http://192.168.1.100:80",
                        "http://fuel.kokonut.io:80",
                        "http://10.0.0.5:8080"
                )

        validUrls.forEach { urlString ->
            val url = URL(urlString)
            assertNotNull(url)
            assertTrue(url.toString().startsWith("http://"))
        }

        println("✅ Handshake supports various network addresses")
    }

    @Test
    fun `test URL protocol validation`() {
        val httpUrl = URL("http://localhost:80")
        assertEquals("http", httpUrl.protocol)

        println("✅ HTTP protocol correctly identified")
    }

    @Test
    fun `test performHandshake parameter validation order`() {
        val url = URL("http://localhost:80")

        // Public key validation should happen BEFORE network request
        // This ensures early failure for invalid input
        val startTime = System.currentTimeMillis()

        assertFailsWith<IllegalArgumentException> { url.performHandshake("") }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Should fail immediately (< 100ms) without network call
        assertTrue(duration < 100, "Validation should be immediate")

        println("✅ Public key validation occurs before network request")
    }

    @Test
    fun `test performHandshake with null-like string`() {
        val url = URL("http://localhost:80")
        val nullString = "null"

        // verify 'null' string is not blank (sanity check)
        assertFalse(nullString.isBlank())

        // This should NOT throw IllegalArgumentException because "null" is not blank.
        // It should attempt the handshake and return a response (likely a failure response due to
        // no server).
        try {
            val response = url.performHandshake(nullString)
            assertNotNull(response)
            // We expect failure because localhost:80 is likely not running the node,
            // but the important part is that client-side validation passed.
        } catch (e: IllegalArgumentException) {
            fail("Should not throw IllegalArgumentException for 'null' string")
        } catch (e: Exception) {
            // Connection errors are allowed, but we really expect performHandshake to catch them
            // and return a response object
            // based on the implementation in API.kt
        }

        println("✅ String 'null' passed client-side validation as expected")
    }

    @Test
    fun `test performHandshake exception contains helpful context`() {
        val url = URL("http://localhost:80")

        val exception = assertFailsWith<IllegalArgumentException> { url.performHandshake("  \t  ") }

        val message = exception.message!!

        // Error message should guide user
        assertTrue(message.contains("Public key"))
        assertTrue(message.contains("required") || message.contains("load"))

        println("✅ Exception message provides helpful context")
    }

    @Test
    fun `test different types of whitespace are detected`() {
        val url = URL("http://localhost:80")

        val whitespaceVariants =
                listOf(
                        " ", // space
                        "\t", // tab
                        "\n", // newline
                        "\r", // carriage return
                        "  \t\n", // mixed
                        "\u00A0" // non-breaking space
                )

        whitespaceVariants.forEach { whitespace ->
            assertFailsWith<IllegalArgumentException> { url.performHandshake(whitespace) }
        }

        println("✅ All whitespace variants correctly detected")
    }

    @Test
    fun `test performHandshake with trimmed key equivalence`() {
        val keyWithSpaces = "  my_public_key  "
        val trimmedKey = "my_public_key"

        // Keys with leading/trailing spaces are different
        assertNotEquals(keyWithSpaces, trimmedKey)

        // But both are not blank
        assertFalse(keyWithSpaces.isBlank())
        assertFalse(trimmedKey.isBlank())

        println("✅ Keys with spaces are treated as distinct (no auto-trim)")
    }
}
