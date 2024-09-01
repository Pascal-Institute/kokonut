package kokonut.util

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class GitHub {
    companion object {

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
}