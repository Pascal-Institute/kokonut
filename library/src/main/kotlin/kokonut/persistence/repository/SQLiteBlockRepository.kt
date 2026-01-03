package kokonut.persistence.repository

import kokonut.core.Block
import kokonut.persistence.database.Database

/**
 * SQLite implementation of BlockRepository
 * Provides concrete data access using SQLite database
 */
class SQLiteBlockRepository(
    private val database: Database
) : BlockRepository {
    
    private var cache: List<Block>? = null
    
    override fun insert(entity: Block) {
        database.insert(entity)
        cache = null // Invalidate cache
    }
    
    override fun insert(entities: List<Block>) {
        database.insert(entities)
        cache = null // Invalidate cache
    }
    
    override fun findAll(): List<Block> {
        if (cache == null) {
            cache = database.loadAll()
        }
        return cache!!
    }
    
    override fun findById(id: Long): Block? {
        return findByIndex(id)
    }
    
    override fun findByHash(hash: String): Block? {
        return findAll().find { it.hash == hash }
    }
    
    override fun findByIndex(index: Long): Block? {
        return findAll().find { it.index == index }
    }
    
    override fun findLast(): Block? {
        return findAll().maxByOrNull { it.index }
    }
    
    override fun count(): Long {
        return findAll().size.toLong()
    }
    
    override fun exists(hash: String): Boolean {
        return findByHash(hash) != null
    }
    
    override fun delete(id: Long) {
        throw UnsupportedOperationException("Block deletion not supported in blockchain")
    }
    
    override fun clear() {
        database.clear()
        cache = null
    }
    
    fun refreshCache() {
        cache = null
    }
}
