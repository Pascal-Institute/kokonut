package kokonut.util

import kokonut.core.Block
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class GitHub(var remoteUrl : String, var repoPath: String) {

        fun clone() : String{
            return executeCMD(listOf("git", "clone", remoteUrl, repoPath), File(repoPath))
        }

        fun remote() : String{
            return executeCMD(listOf("git", "remote", "update"), File(repoPath))
        }

        fun add() : String{
            return executeCMD(listOf("git", "add", "."), File(repoPath))
        }

        fun commit(block: Block) : String{
            return executeCMD(listOf("git", "commit", "-m", "Add Index ${block.index} Block to Chain"), File(repoPath))
        }

        fun push() : String{
            return executeCMD(listOf("git", "push", "origin", "main"), File(repoPath))
        }

        private fun executeCMD(command: List<String>, workingDir: File): String {
            val process = ProcessBuilder(command)
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()
            val result = StringBuilder()

            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    result.append(line).append("\n")
                    line = reader.readLine()
                }
            }

            process.waitFor()
            return result.toString()
        }
}