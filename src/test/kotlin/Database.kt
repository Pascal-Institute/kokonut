import kokonut.Utility.Companion.getDatabasePath
import kokonut.Utility.Companion.printTableData
import kokonut.Utility.Companion.printTableStructure
import kokonut.Utility.Companion.tableExists
import kokonut.core.BlockChain
import java.sql.Connection
import java.sql.DriverManager

fun main(){
    val blockChain = BlockChain()
    val dbPath = getDatabasePath()
    println("Database path: $dbPath")

    val connection: Connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")

    try {
        val tableName = "kovault"
        if (tableExists(connection, tableName)) {
            println("Table '$tableName' exists.")

            println("Table structure:")
            printTableStructure(connection, tableName)

            println("Table data:")
            printTableData(connection, tableName)
        } else {
            println("Table '$tableName' does not exist.")
        }
    } finally {
        connection.close()
    }
}