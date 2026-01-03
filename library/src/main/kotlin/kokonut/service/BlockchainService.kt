package kokonut.service

import kokonut.core.Block
import kokonut.core.Data
import kokonut.config.NetworkConfig
import kokonut.persistence.repository.BlockRepository

/**
 * Core blockchain service handling chain operations
 * Separates business logic from data access and network layers
 */
class BlockchainService(
    private val repository: BlockRepository,
    private val config: NetworkConfig = NetworkConfig()
) {
    
    fun getChain(): List<Block> = repository.findAll()
    
    fun getLastBlock(): Block = repository.findLast() 
        ?: throw IllegalStateException("Blockchain is empty")
    
    fun getGenesisBlock(): Block = repository.findByIndex(0)
        ?: throw IllegalStateException("Genesis block not found")
    
    fun getChainSize(): Long = repository.count()
    
    fun getBlockByHash(hash: String): Block? = repository.findByHash(hash)
    
    fun getBlockByIndex(index: Long): Block? = repository.findByIndex(index)
    
    fun addBlock(block: Block) {
        // Validation should happen before calling this
        repository.insert(block)
    }
    
    fun isValid(): Boolean {
        val chain = getChain()
        if (chain.isEmpty()) return false
        
        // Validate each block
        for (i in 0 until chain.size) {
            val block = chain[i]
            
            // Check block validity
            if (!block.isValid()) return false
            
            // Check chain linkage (skip genesis)
            if (i > 0) {
                val previousBlock = chain[i - 1]
                if (block.previousHash != previousBlock.hash) return false
                if (block.index != previousBlock.index + 1) return false
            }
        }
        
        return true
    }
    
    fun hasGenesisTreasuryMint(): Boolean {
        return getChain().any { block ->
            block.data.transactions.any { tx ->
                tx.transaction == "GENESIS_MINT" && tx.receiver == config.treasuryAddress
            }
        }
    }
    
    fun refreshCache() {
        if (repository is kokonut.persistence.repository.SQLiteBlockRepository) {
            repository.refreshCache()
        }
    }
}
