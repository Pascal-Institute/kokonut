package kokonut.fuelnode.test

import java.io.File
import kokonut.core.BlockChain
import kokonut.util.NodeType
import kokonut.util.SQLite
import kotlin.test.*

class FuelNodeTest {

    private lateinit var dbFile: File

    @BeforeTest
    fun setup() {
        // Create a temporary database file for each test
        dbFile = File.createTempFile("test_fuel_db", ".db")
        dbFile.deleteOnExit()

        // Inject the temporary SQLite instance into BlockChain
        BlockChain.database = SQLite(dbFile.absolutePath)

        // Ensure chain is empty/clean
        BlockChain.refreshFromDatabase()
    }

    @AfterTest
    fun tearDown() {
        // Clean up
        if (dbFile.exists()) {
            dbFile.delete()
        }
    }

    @Test
    fun `test fuel node bootstrapping creates genesis and mint`() {
        // Ensure empty
        assertEquals(0, BlockChain.getChainSize())

        // Initialize as FUEL node (simulates fresh start)
        // This simulates the behavior when a Fuel Node starts up with no existing DB and no peer.
        BlockChain.initialize(NodeType.FUEL)

        // Verify chain size
        // Logic: Genesis Block + Genesis Treasury Mint Block = 2 blocks
        val expectedSize = 2L
        assertEquals(
                expectedSize,
                BlockChain.getChainSize(),
                "Fuel node should create Genesis and Mint blocks"
        )

        val chain = BlockChain.getChain()
        val genesis = chain[0]
        val mint = chain[1]

        assertEquals(0, genesis.index)
        assertEquals("GENESIS", genesis.data.validator)

        assertEquals(1, mint.index)
        assertTrue(
                mint.data.transactions.any { it.transaction == "GENESIS_MINT" },
                "Should have GENESIS_MINT transaction in the second block"
        )

        // Verify Chain Integrity
        assertTrue(BlockChain.isValid(), "Generated chain should be valid")
    }
}
