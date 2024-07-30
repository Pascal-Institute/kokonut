package kokonut

import kotlinx.serialization.Serializable

@Serializable
data class GitHubFile(
    val name: String,
    val path: String,
    val type: String // "file" or "dir"
)