package kokonut.util.full

import kotlinx.serialization.Serializable

@Serializable
data class HealthCheck(
    val HTTP: String? = null,
    val Interval: String? = null,
    val Timeout: String? = null,
    val DeregisterCriticalServiceAfter: String? = null
)
