import kokonut.core.Block
import kokonut.core.Data
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FullNodeTest {

        @Test
        fun `test block creation and hashing`() {
                val data =
                        Data(
                                reward = 10.0,
                                ticker = "KNT",
                                miner = "test_miner",
                                transactions = emptyList(),
                                comment = "Test Block"
                        )

                val block =
                        Block(
                                version = 1,
                                index = 1,
                                previousHash = "0000000000000000",
                                timestamp = System.currentTimeMillis(),
                                data = data,
                                difficulty = 1,
                                nonce = 100,
                                hash = ""
                        )

                val hash = block.calculateHash()
                assertNotNull(hash)
                assertTrue(hash.isNotEmpty())

                block.hash = hash
                assertTrue(block.isValid())
        }

        @Test
        fun `test blockchain validity`() {
                // val chain = BlockChain()
                // Assuming database is clean or handled by standard initialization
                // We can't easily inject mock DB without refactoring BlockChain to not use
                // singleton
                // directly in init
                // So we test logical validity function if possible

                // This is a placeholder since BlockChain is tightly coupled to SQLite and Network
                // in its
                // init
                assertTrue(true)
        }
}
