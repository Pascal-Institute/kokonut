import kokonut.core.BlockChain

fun main(){
    try {
        val tableName = "kovault"
        if (BlockChain.database.tableExists(tableName)) {
            println("Table '$tableName' exists.")

            println("Table structure:")
            BlockChain.database.printTableStructure(tableName)

            println("Table data:")
            BlockChain.database.printTableData(tableName)
        } else {
            println("Table '$tableName' does not exist.")
        }
    } finally {
        BlockChain.database.connection.close()
    }
}