package kokonut.util

import java.io.File
import java.sql.*
import kokonut.core.Block
import kokonut.util.Utility.Companion.getJarDirectory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SQLite(private val customPath: String? = null) {

    val tableName = "kovault"
    val dbPath = customPath ?: getDatabasePath()

    private fun openConnection(): Connection {
        val conn = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        conn.createStatement().use { stmt -> stmt.execute("PRAGMA busy_timeout = 5000;") }
        return conn
    }

    init {
        openConnection().use { conn ->
            try {
                if (!tableExists(tableName)) {
                    val createTableSQL =
                            """
                    CREATE TABLE $tableName (
                        hash TEXT PRIMARY KEY,
                        block TEXT NOT NULL
                    );
                    """
                    conn.createStatement().use { statement -> statement.execute(createTableSQL) }
                    println("Table '$tableName' created.")
                } else {
                    println("Table '$tableName' already exists.")
                }
            } catch (e: Exception) {
                try {
                    conn.rollback()
                } catch (rollbackException: Exception) {
                    println("Failed to rollback transaction: ${rollbackException.message}")
                }
                println("An error occurred: ${e.message}")
            }
        }
    }

    fun deleteDatabase() {
        val jarDir = getJarDirectory()
        val dbFile = File(jarDir, "kovault.db")

        if (dbFile.exists()) {
            dbFile.delete()
            println("Database is deleted...")
        }
    }

    /**
     * Clear all records from the blockchain table Used when resyncing from a different genesis
     * block
     */
    fun clearTable() {
        openConnection().use { conn ->
            val deleteSQL = "DELETE FROM $tableName"
            conn.autoCommit = false

            try {
                conn.createStatement().use { statement ->
                    val deletedRows = statement.executeUpdate(deleteSQL)
                    println("🗑️ Cleared $deletedRows blocks from database")
                }
                conn.commit()
            } catch (e: Exception) {
                try {
                    conn.rollback()
                } catch (rollbackException: SQLException) {
                    println("Failed to rollback transaction: ${rollbackException.message}")
                }
                println("An error occurred while clearing table: ${e.message}")
            }
        }
    }

    // SQLlite
    fun getDatabasePath(): String {
        // Use environment variable for data directory if set (Docker friendly)
        val dataDirEnv = System.getenv("KOKONUT_DATA_DIR")

        val dbDir =
                if (!dataDirEnv.isNullOrBlank()) {
                    File(dataDirEnv).also {
                        if (!it.exists()) {
                            it.mkdirs()
                            println("Created data directory: ${it.absolutePath}")
                        }
                    }
                } else {
                    getJarDirectory()
                }

        val dbFile = File(dbDir, "kovault.db")

        if (!dbFile.exists()) {
            dbFile.createNewFile()
            println("Database file created at: ${dbFile.absolutePath}")
        } else {
            println("Database file already exists at: ${dbFile.absolutePath}")
        }

        return dbFile.absolutePath
    }

    fun tableExists(tableName: String): Boolean {
        openConnection().use { conn ->
            val metaData = conn.metaData
            metaData.getTables(null, null, tableName, null).use { resultSet ->
                return resultSet.next()
            }
        }
    }

    fun printTableStructure(tableName: String) {
        openConnection().use { conn ->
            val query = "PRAGMA table_info($tableName);"
            conn.createStatement().use { statement ->
                statement.executeQuery(query).use { resultSet ->
                    while (resultSet.next()) {
                        val columnName = resultSet.getString("name")
                        val columnType = resultSet.getString("type")
                        val isNullable = resultSet.getString("notnull")
                        val defaultValue = resultSet.getString("dflt_value")
                        println(
                                "Column: $columnName, Type: $columnType, Nullable: $isNullable, Default: $defaultValue"
                        )
                    }
                }
            }
        }
    }

    fun printTableData(tableName: String) {
        openConnection().use { conn ->
            val query = "SELECT * FROM $tableName;"
            conn.createStatement().use { statement ->
                statement.executeQuery(query).use { resultSet ->
                    val metaData = resultSet.metaData
                    val columnCount = metaData.columnCount
                    for (i in 1..columnCount) {
                        print("${metaData.getColumnName(i)}\t")
                    }
                    println()

                    while (resultSet.next()) {
                        for (i in 1..columnCount) {
                            print("${resultSet.getString(i)}\t")
                        }
                        println()
                    }
                }
            }
        }
    }

    fun fetch(): MutableList<Block> {
        openConnection().use { conn ->
            val newChain = mutableListOf<Block>()
            val query = "SELECT hash, block FROM $tableName"

            conn.prepareStatement(query).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val serializedBlock = resultSet.getString("block")
                        val block: Block = Json.decodeFromString(serializedBlock)
                        newChain.add(block)
                    }
                }
            }

            return newChain
        }
    }

    fun fetch(chain: MutableList<Block>): MutableList<Block> {
        openConnection().use { conn ->
            val newChain = mutableListOf<Block>()
            val query = "SELECT hash, block FROM $tableName"

            conn.prepareStatement(query).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val serializedBlock = resultSet.getString("block") // 직렬화된 블록 데이터
                        val block = Json.decodeFromString<Block>(serializedBlock)

                        if (!chain.contains(block)) {
                            newChain.add(block)
                        }
                    }
                }
            }

            return newChain
        }
    }

    fun insert(block: Block) {
        openConnection().use { conn ->
            val insertSQL = "INSERT INTO $tableName (hash, block) VALUES (?, ?)"
            val selectSQL = "SELECT COUNT(*) FROM $tableName WHERE hash = ?"

            conn.autoCommit = false

            try {
                val hash = block.hash
                val value = Json.encodeToString(block)

                conn.prepareStatement(selectSQL).use { preparedStatementSelect ->
                    conn.prepareStatement(insertSQL).use { preparedStatementInsert ->
                        // Check if hash already exists
                        preparedStatementSelect.setString(1, hash)
                        val count =
                                preparedStatementSelect.executeQuery().use { rs ->
                                    if (rs.next()) rs.getInt(1) else 0
                                }

                        if (count == 0) {
                            preparedStatementInsert.setString(1, hash)
                            if (value == "null") {
                                preparedStatementInsert.setNull(2, Types.NULL)
                            } else {
                                preparedStatementInsert.setString(2, value)
                            }
                            preparedStatementInsert.executeUpdate()
                        } else {
                            println("Hash $hash already exists, skipping insert.")
                        }
                    }
                }

                conn.commit()
            } catch (e: Exception) {
                try {
                    conn.rollback()
                } catch (rollbackException: SQLException) {
                    println("Failed to rollback transaction: ${rollbackException.message}")
                }
                println("An error occurred while inserting data: ${e.message}")
            }
        }
    }

    fun insert(chain: MutableList<Block>) {
        openConnection().use { conn ->
            val insertSQL = "INSERT INTO $tableName (hash, block) VALUES (?, ?)"
            val selectSQL = "SELECT COUNT(*) FROM $tableName WHERE hash = ?"

            conn.autoCommit = false

            try {
                conn.prepareStatement(selectSQL).use { preparedStatementSelect ->
                    conn.prepareStatement(insertSQL).use { preparedStatementInsert ->
                        for (block in chain) {
                            val hash = block.hash
                            val value = Json.encodeToString(block)

                            preparedStatementSelect.setString(1, hash)
                            val count =
                                    preparedStatementSelect.executeQuery().use { rs ->
                                        if (rs.next()) rs.getInt(1) else 0
                                    }

                            if (count == 0) {
                                preparedStatementInsert.setString(1, hash)
                                if (value == "null") {
                                    preparedStatementInsert.setNull(2, Types.NULL)
                                } else {
                                    preparedStatementInsert.setString(2, value)
                                }
                                preparedStatementInsert.executeUpdate()
                            } else {
                                println("Hash $hash already exists, skipping insert.")
                            }
                        }
                    }
                }

                conn.commit()
            } catch (e: Exception) {
                try {
                    conn.rollback()
                } catch (rollbackException: SQLException) {
                    println("Failed to rollback transaction: ${rollbackException.message}")
                }
                println("An error occurred while inserting data: ${e.message}")
            }
        }
    }
}
