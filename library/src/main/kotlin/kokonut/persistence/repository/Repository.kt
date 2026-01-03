package kokonut.persistence.repository

/**
 * Base repository interface for data access operations
 * Provides abstraction layer between business logic and data storage
 */
interface Repository<T, ID> {
    fun insert(entity: T)
    fun insert(entities: List<T>)
    fun findAll(): List<T>
    fun findById(id: ID): T?
    fun delete(id: ID)
    fun clear()
}
