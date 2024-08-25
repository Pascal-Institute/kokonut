import kokonut.core.BlockChain

fun main(){
    val blockChain = BlockChain()

    try {
        val tableName = "kovault"
        if (blockChain.sqlite.tableExists(tableName)) {
            println("Table '$tableName' exists.")

            println("Table structure:")
            blockChain.sqlite.printTableStructure(tableName)

            println("Table data:")
            blockChain.sqlite.printTableData(tableName)
        } else {
            println("Table '$tableName' does not exist.")
        }
    } finally {
        blockChain.sqlite.connection.close()
    }
}