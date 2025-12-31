import kokonut.core.BlockDataType
import kokonut.core.FuelNodeInfo
import kokonut.util.GenesisGenerator
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test

class KokonutIntegrationTest {

    @Test
    fun testGenesisGeneration() {
        val genesis = GenesisGenerator.createGenesisBlock()
        assertNotNull(genesis.data.networkRules)
        assertEquals(0, genesis.index)
        assertEquals("GENESIS", genesis.data.validator)

        println("✅ Genesis Block Generated: ${genesis.hash}")
    }

    @Test
    fun testFuelNodeRegistrationBlock() {
        val genesis = GenesisGenerator.createGenesisBlock()
        val bootstrapBlock =
                GenesisGenerator.createBootstrapFuelBlock(
                        fuelAddress = "http://test-fuel.com",
                        fuelPublicKey = "pubkey123",
                        stake = 1000000.0,
                        previousHash = genesis.hash
                )

        assertEquals(BlockDataType.FUEL_REGISTRATION, bootstrapBlock.data.type)
        assertNotNull(bootstrapBlock.data.fuelNodeInfo)
        assertTrue(bootstrapBlock.data.fuelNodeInfo!!.isBootstrap)

        println("✅ Bootstrap Fuel Block Generated: ${bootstrapBlock.hash}")
    }

    @Test
    fun testFuelNodeInfoSerialization() {
        val info = FuelNodeInfo(address = "http://test.com", publicKey = "key", stake = 500.0)

        assertEquals("http://test.com", info.address)
        assertEquals(500.0, info.stake)
    }
}
