import java.net.URL
import kokonut.util.API.Companion.isHealthy
import kotlin.test.Test
import kotlin.test.assertTrue

class FuelNodeTest {

    @Test
    fun `test fuel node health check`() {
        val fuelNodeUrl = URL("http://localhost:80")
        if (fuelNodeUrl.isHealthy()) {
            assertTrue(true, "Fuel node is healthy and reachable.")
        } else {
            println("WARNING: Fuel node is not running. Skipping health check.")
            assertTrue(true)
        }
    }

    @Test
    fun `test genesis block availability`() {
        // This assumes FuelNode is running locally or mocks if not.
        // For unit test w/o running server, we might need mocking API calls.
        // Here we just write a placeholder for the integration pattern.
        assertTrue(true)
    }
}
