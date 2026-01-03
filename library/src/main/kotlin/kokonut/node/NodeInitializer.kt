package kokonut.node

import kokonut.config.NetworkConfig
import kokonut.core.FuelNodeInfo
import kokonut.persistence.repository.BlockRepository
import kokonut.service.BlockchainService
import kokonut.util.NodeType
import kokonut.util.GenesisGenerator
import java.net.URL

/**
 * Handles blockchain initialization logic for different node types
 * Extracted from BlockChain.initialize()
 */
class NodeInitializer(
    private val repository: BlockRepository,
    private val blockchainService: BlockchainService,
    private val config: NetworkConfig = NetworkConfig()
) {
    
    /**
     * Initialize blockchain for a specific node type
     * 
     * Rules:
     * 1. Try to load from local DB (Persistence)
     * 2. If empty, try to bootstrap from peer
     * 3. If no peer/bootstrap fails:
     *    - FUEL Node: Creates new Genesis Block (starts network)
     *    - FULL/LIGHT Node: THROWS EXCEPTION (must connect to existing network)
     */
    fun initialize(nodeType: NodeType, peerAddress: String? = null) {
        // 1. Try Load from DB
        if (blockchainService.getChainSize() > 0) {
            println("✅ Blockchain loaded from local database. Size: ${blockchainService.getChainSize()}")
            
            // Safe upgrade path: if this is a Fuel node with only Genesis, 
            // ensure the one-time treasury mint block exists
            if (nodeType == NodeType.FUEL) {
                ensureGenesisTreasuryMintIfNeeded()
            }
            return
        }
        
        // 2. If DB empty, try Bootstrap
        val peer = peerAddress ?: config.knownPeer
        
        if (peer != null) {
            try {
                bootstrap(peer)
                return
            } catch (e: Exception) {
                println("❌ Bootstrap failed from peer: $peer")
                println("Error: ${e.message}")
                
                if (nodeType != NodeType.FUEL) {
                    throw IllegalStateException(
                        "Failed to bootstrap from configured peer. Check network connection or peer address."
                    )
                }
                // For Fuel Node, we might fall back to Genesis creation
            }
        }
        
        // 3. Handle Fresh Start (No Chain, No Peer)
        if (nodeType == NodeType.FUEL) {
            createGenesisChain()
        } else {
            throw IllegalStateException(
                "❌ ${nodeType} Node CANNOT create Genesis Block.\n" +
                    "MUST be connected to a Fuel Node or Peer to join the network.\n" +
                    "Please set KOKONUT_PEER environment variable."
            )
        }
    }
    
    /**
     * Create Genesis chain with initial treasury mint
     */
    private fun createGenesisChain() {
        println("🌱 Fuel Node: No existing chain and no peer found. Creating Genesis Block...")
        
        val genesis = GenesisGenerator.createGenesisBlock()
        repository.insert(genesis)
        
        // Record initial treasury funding
        val mintBlock = GenesisGenerator.createGenesisTreasuryMintBlock(
            treasuryAddress = config.treasuryAddress,
            previousHash = genesis.hash,
            index = 1,
            amount = config.genesisMintAmount
        )
        repository.insert(mintBlock)
        
        blockchainService.refreshCache()
        println("✅ Genesis chain created successfully")
    }
    
    /**
     * Bootstrap from a known peer
     */
    private fun bootstrap(peerAddress: String) {
        println("🔍 Bootstrapping from peer: $peerAddress")
        // Bootstrap logic will be implemented using network services
        // For now, this is a placeholder
        throw UnsupportedOperationException("Bootstrap implementation pending network refactoring")
    }
    
    /**
     * Ensures the Genesis treasury mint block exists exactly once
     * Only auto-inserts when the chain is effectively fresh (Genesis-only)
     */
    private fun ensureGenesisTreasuryMintIfNeeded() {
        if (blockchainService.hasGenesisTreasuryMint()) return
        
        val chainSize = blockchainService.getChainSize()
        
        // Only safe to auto-mutate a brand-new chain
        if (chainSize != 1L) {
            println(
                "⚠️ Genesis treasury mint is missing but chain is not fresh; " +
                    "skipping auto-insert to avoid rewriting history."
            )
            return
        }
        
        val genesis = blockchainService.getGenesisBlock()
        val mintBlock = GenesisGenerator.createGenesisTreasuryMintBlock(
            treasuryAddress = config.treasuryAddress,
            previousHash = genesis.hash,
            index = 1,
            amount = config.genesisMintAmount
        )
        
        repository.insert(mintBlock)
        blockchainService.refreshCache()
        
        println(
            "🏦 Inserted one-time Genesis treasury mint: " +
                "${config.genesisMintAmount} KNT -> ${config.treasuryAddress}"
        )
    }
}
