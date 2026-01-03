package kokonut.persistence.repository

import kokonut.core.Block

/**
 * Repository interface for Block persistence operations
 * Separates data access logic from business logic
 */
interface BlockRepository : Repository<Block, Long> {
    fun findByHash(hash: String): Block?
    fun findByIndex(index: Long): Block?
    fun findLast(): Block?
    fun count(): Long
    fun exists(hash: String): Boolean
}
