package kokonut.fullnode.test

import java.io.File
import kokonut.core.Block
import kokonut.core.BlockChain
import kokonut.core.Data
import kokonut.core.Transaction
import kokonut.util.NodeType
import kokonut.util.SQLite
import kotlin.test.*

class FullNodeTest {

        private lateinit var dbFile: File

        @BeforeTest
        fun setup() {
                // Create a temporary database file for each test
                dbFile = File.createTempFile("test_blockchain", ".db")
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
        fun `test block creation and hashing`() {
                val data =
                        Data(
                                reward = 10.0,
                                ticker = "KNT",
                                validator = "test_validator_hash",
                                transactions = emptyList(),
                                comment = "Test Block"
                        )

                val block =
                        Block(
                                index = 1,
                                previousHash = "0000000000000000",
                                timestamp = System.currentTimeMillis(),
                                data = data,
                                validatorSignature = "sig123",
                                hash = ""
                        )

                val hash = block.calculateHash()
                assertNotNull(hash, "Hash should not be null")
                assertTrue(hash.isNotEmpty(), "Hash should not be empty")

                block.hash = hash
                assertTrue(block.isValid(), "Block should be valid after setting correct hash")

                // Test tampering
                block.timestamp += 1
                assertFalse(
                        block.isValid(),
                        "Block should be invalid after modifying content without rehashing"
                )
        }

        @Test
        fun `test blockchain validity with valid chain`() {
                // 1. Create Genesis Block
                val genesisData =
                        Data(
                                reward = 0.0,
                                ticker = "KNT",
                                validator = "GENESIS",
                                transactions = emptyList()
                        )
                val genesisBlock =
                        Block(
                                index = 0,
                                previousHash = "0",
                                timestamp = System.currentTimeMillis(),
                                data = genesisData,
                                validatorSignature = "",
                                hash = ""
                        )
                genesisBlock.hash = genesisBlock.calculateHash()

                // 2. Create Second Block
                val block1Data =
                        Data(
                                reward = 10.0,
                                ticker = "KNT",
                                validator = "BOOTSTRAP",
                                transactions = emptyList()
                        )
                val block1 =
                        Block(
                                index = 1,
                                previousHash = genesisBlock.hash,
                                timestamp = System.currentTimeMillis(),
                                data = block1Data,
                                validatorSignature = "sig_val1",
                                hash = ""
                        )
                block1.hash = block1.calculateHash()

                // 3. Insert into Database
                BlockChain.database.insert(genesisBlock)
                BlockChain.database.insert(block1)

                // 4. Reload Chain
                BlockChain.refreshFromDatabase()

                // 5. Verify
                assertEquals(2, BlockChain.getChainSize(), "Chain size should be 2")
                assertTrue(BlockChain.isValid(), "Blockchain should be valid")
        }

        @Test
        fun `test blockchain validity with broken link`() {
                // 1. Create Genesis Block
                val genesisBlock = createBlock(0, "0", "GENESIS")

                // 2. Create Second Block with WRONG previous hash
                val block1 = createBlock(1, "WRONG_HASH", "BOOTSTRAP")

                // 3. Insert
                BlockChain.database.insert(genesisBlock)
                BlockChain.database.insert(block1)

                // 4. Reload
                BlockChain.refreshFromDatabase()

                // 5. Verify
                assertFalse(
                        BlockChain.isValid(),
                        "Blockchain should be invalid due to broken hash link"
                )
        }

        @Test
        fun `test blockchain validity with tampered block`() {
                // 1. Create Genesis
                val genesisBlock = createBlock(0, "0", "GENESIS")

                // 2. Create Second Block
                val block1 = createBlock(1, genesisBlock.hash, "BOOTSTRAP")

                // 3. Tamper with block1 AFTER hashing
                val badBlock = block1.copy(hash = "bad_hash_value")

                BlockChain.database.insert(genesisBlock)
                BlockChain.database.insert(badBlock)

                BlockChain.refreshFromDatabase()

                assertFalse(
                        BlockChain.isValid(),
                        "Blockchain should be invalid because block hash doesn't match content"
                )
        }

        @Test
        fun `test initialization with empty db`() {
                // Should have size 0 initially
                assertEquals(0, BlockChain.getChainSize())

                // Initialize as FUEL node (simulates fresh start)
                BlockChain.initialize(NodeType.FUEL)

                assertTrue(
                        BlockChain.getChainSize() > 0,
                        "Fuel node initialization should create Genesis block"
                )
                val genesis = BlockChain.getGenesisBlock()
                assertEquals(0, genesis.index)
                assertTrue(BlockChain.isValid(), "Initialized chain should be valid")
        }

        @Test
        fun `test block with transaction`() {
                val tx =
                        Transaction(
                                transaction = "TRANSFER",
                                sender = "senderAddr",
                                receiver = "receiverAddr",
                                remittance = 50.0,
                                commission = 0.5
                        )

                val data =
                        Data(
                                reward = 0.0,
                                ticker = "KNT",
                                validator = "val",
                                transactions = listOf(tx)
                        )

                val block =
                        Block(
                                index = 5,
                                previousHash = "prev",
                                timestamp = System.currentTimeMillis(),
                                data = data,
                                validatorSignature = "sig",
                                hash = ""
                        )

                block.hash = block.calculateHash()
                assertTrue(block.isValid())

                // Verify hash affects transaction changes
                val modifiedTx = tx.copy(remittance = 100.0)
                val modifiedData = data.copy(transactions = listOf(modifiedTx))
                val tamperedBlock = block.copy(data = modifiedData)

                assertFalse(
                        tamperedBlock.isValid(),
                        "Block validity should fail if transaction content changes without rehashing"
                )
        }

        private fun createBlock(index: Long, prevHash: String, validator: String): Block {
                val data =
                        Data(
                                reward = 10.0,
                                ticker = "KNT",
                                validator = validator,
                                transactions = emptyList()
                        )
                val block =
                        Block(
                                index = index,
                                previousHash = prevHash,
                                timestamp = System.currentTimeMillis(),
                                data = data,
                                validatorSignature = "sig_$validator",
                                hash = ""
                        )
                block.hash = block.calculateHash()
                return block
        }
}
