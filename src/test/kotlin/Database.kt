import kokonut.core.BlockChain

fun main(){
    val blockChain = BlockChain()

    try {
        val tableName = "kovault"
        if (blockChain.database.tableExists(tableName)) {
            println("Table '$tableName' exists.")

            println("Table structure:")
            blockChain.database.printTableStructure(tableName)

            println("Table data:")
            blockChain.database.printTableData(tableName)
        } else {
            println("Table '$tableName' does not exist.")
        }
    } finally {
        blockChain.database.connection.close()
    }
}