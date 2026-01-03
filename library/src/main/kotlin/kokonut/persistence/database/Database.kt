package kokonut.persistence.database

import kokonut.core.Block

/**
 * Database interface abstracting storage implementation
 * Allows swapping between SQLite, PostgreSQL, or other databases
 */
interface Database {
    fun insert(block: Block)
    fun insert(blocks: List<Block>)
    fun loadAll(): List<Block>
    fun clear()
    fun close()
}
