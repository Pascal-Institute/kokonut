package kokonut.util

import com.google.gson.Gson
import kokonut.core.Block
import kokonut.core.BlockChain
import kokonut.util.Utility.Companion.getJarDirectory
import kotlinx.serialization.json.Json
import java.io.File
import java.sql.*

class SQLite {

    private val gson = Gson()
    val tableName = "kovault"
    val dbPath = getDatabasePath()
    var connection: Connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")

    init {
        try {
            // 테이블 존재 여부 확인
            if (!tableExists(tableName)) {
                val createTableSQL = """
                CREATE TABLE $tableName (
                    hash TEXT PRIMARY KEY,
                    block TEXT NOT NULL
                );
                """
                val statement: Statement = connection.createStatement()
                statement.execute(createTableSQL)
                println("Table '$tableName' created.")
            } else {
                println("Table '$tableName' already exists.")
            }
        } catch (e: Exception) {
            // 오류 발생 시 롤백
            try {
                connection.rollback()
            } catch (rollbackException: Exception) {
                println("Failed to rollback transaction: ${rollbackException.message}")
            }
            println("An error occurred: ${e.message}")
        } finally {
            // 자원 정리
            try {
                connection.close()
            } catch (closeException: Exception) {
                println("Failed to close connection: ${closeException.message}")
            }
        }
    }

    //SQLlite
    fun getDatabasePath(): String {
        val jarDir = getJarDirectory() // 현재 실행 중인 애플리케이션의 JAR 파일 디렉토리 경로를 가져옴
        val dbFile = File(jarDir, "kovault.db")

        if (!dbFile.exists()) {
            dbFile.createNewFile()
            println("Database file created at: ${dbFile.absolutePath}")
        } else {
            println("Database file already exists at: ${dbFile.absolutePath}")
        }

        return dbFile.absolutePath
    }

    fun tableExists(tableName: String): Boolean {
        connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        val metaData = connection.metaData
        val resultSet: ResultSet = metaData.getTables(null, null, tableName, null)
        val exists = resultSet.next()
        resultSet.close()
        return exists
    }

    fun printTableStructure(tableName: String) {
        connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        val query = "PRAGMA table_info($tableName);"
        val statement: Statement = connection.createStatement()
        val resultSet: ResultSet = statement.executeQuery(query)

        while (resultSet.next()) {
            val columnName = resultSet.getString("name")
            val columnType = resultSet.getString("type")
            val isNullable = resultSet.getString("notnull")
            val defaultValue = resultSet.getString("dflt_value")
            println("Column: $columnName, Type: $columnType, Nullable: $isNullable, Default: $defaultValue")
        }

        resultSet.close()
        statement.close()
        connection.close()
    }

    fun printTableData(tableName: String) {
        connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        val query = "SELECT * FROM $tableName;"
        val statement: Statement = connection.createStatement()
        val resultSet: ResultSet = statement.executeQuery(query)

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

        resultSet.close()
        statement.close()
        connection.close()
    }

    fun fetch(): MutableList<Block> {

        connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        val newChain = mutableListOf<Block>()
        val query = "SELECT hash, block FROM tableName"

        val statement = connection.prepareStatement(query)
        val resultSet = statement.executeQuery()

        while (resultSet.next()) {
            val serializedBlock = resultSet.getString("block") // 직렬화된 블록 데이터
            val block = Json.decodeFromString<Block>(serializedBlock)
            newChain.add(block)
        }

        return newChain
    }

    fun fetch(chain: MutableList<Block>): MutableList<Block> {

        connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        val newChain = mutableListOf<Block>()
        val query = "SELECT hash, block FROM tableName"

        val statement = connection.prepareStatement(query)
        val resultSet = statement.executeQuery()

        while (resultSet.next()) {
            val serializedBlock = resultSet.getString("block") // 직렬화된 블록 데이터
            val block = Json.decodeFromString<Block>(serializedBlock)

            if (!chain.contains(block)) {
                newChain.add(block)
            }
        }

        return newChain
    }

    fun insert(block: Block) {
        val connection: Connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        val insertSQL = "INSERT INTO $tableName (hash, block) VALUES (?, ?)"
        val selectSQL = "SELECT COUNT(*) FROM $tableName WHERE hash = ?"

        val preparedStatementInsert: PreparedStatement = connection.prepareStatement(insertSQL)
        val preparedStatementSelect: PreparedStatement = connection.prepareStatement(selectSQL)

        connection.autoCommit = false

        try {
            val hash = block.hash
            val value = gson.toJson(block)

            // Check if hash already exists
            preparedStatementSelect.setString(1, hash)
            val resultSet: ResultSet = preparedStatementSelect.executeQuery()
            val count = if (resultSet.next()) resultSet.getInt(1) else 0

            if (count == 0) {
                preparedStatementInsert.setString(1, hash)
                preparedStatementInsert.setString(2, value)
                val rowsAffected = preparedStatementInsert.executeUpdate()
            } else {
                println("Hash $hash already exists, skipping insert.")
            }

            connection.commit()
            println("All blocks have been processed.")
        } catch (e: Exception) {
            try {
                connection.rollback()
            } catch (rollbackException: SQLException) {
                println("Failed to rollback transaction: ${rollbackException.message}")
            }
            println("An error occurred while inserting data: ${e.message}")
        } finally {
            connection.close()
        }
    }

    fun insert(chain: MutableList<Block>) {
        val connection: Connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        val insertSQL = "INSERT INTO $tableName (hash, block) VALUES (?, ?)"
        val selectSQL = "SELECT COUNT(*) FROM $tableName WHERE hash = ?"

        val preparedStatementInsert: PreparedStatement = connection.prepareStatement(insertSQL)
        val preparedStatementSelect: PreparedStatement = connection.prepareStatement(selectSQL)

        connection.autoCommit = false

        try {
            for (block in chain) {
                val hash = block.hash
                val value = gson.toJson(block)

                // Check if hash already exists
                preparedStatementSelect.setString(1, hash)
                val resultSet: ResultSet = preparedStatementSelect.executeQuery()
                val count = if (resultSet.next()) resultSet.getInt(1) else 0

                if (count == 0) {
                    // Insert if hash does not exist
                    preparedStatementInsert.setString(1, hash)
                    preparedStatementInsert.setString(2, value)
                    val rowsAffected = preparedStatementInsert.executeUpdate()
                } else {
                    println("Hash $hash already exists, skipping insert.")
                }
            }

            connection.commit()
            println("All blocks have been processed.")
        } catch (e: Exception) {
            try {
                connection.rollback()
            } catch (rollbackException: SQLException) {
                println("Failed to rollback transaction: ${rollbackException.message}")
            }
            println("An error occurred while inserting data: ${e.message}")
        } finally {
            connection.close()
        }
    }
}