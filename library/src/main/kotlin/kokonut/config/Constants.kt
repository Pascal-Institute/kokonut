package kokonut.config

/**
 * Global constants for the Kokonut blockchain
 */
object Constants {
    const val TICKER = "KNT"
    const val BLOCK_TIME_MS = 5000L // 5 seconds
    const val HEARTBEAT_INTERVAL_MS = 600_000L // 10 minutes
    const val HEALTH_CHECK_INTERVAL_MS = 300_000L // 5 minutes
    const val DEFAULT_PORT = 80
}
