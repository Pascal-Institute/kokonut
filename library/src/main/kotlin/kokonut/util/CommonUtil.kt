package kokonut.util

import kotlin.math.floor
import kotlin.math.pow

/**
 * Pure utility functions for common operations
 */
object MathUtil {
    
    /**
     * Truncate double to specified decimal places
     * Default: 6 places (micro-KNT precision)
     */
    fun truncate(value: Double, decimals: Int = 6): Double {
        val scale = 10.0.pow(decimals)
        return floor(value * scale) / scale
    }
    
    /**
     * Round to micro-KNT (6 decimal places)
     */
    fun roundToMicroKNT(value: Double): Double {
        return truncate(value, 6)
    }
}

/**
 * String normalization utilities
 */
object StringUtil {
    
    /**
     * Normalize blockchain address
     * Ensures consistent address format
     */
    fun normalizeAddress(address: String): String {
        return address.trim().lowercase()
    }
}

/**
 * Time-related utilities
 */
object TimeUtil {
    
    /**
     * Get current timestamp in milliseconds
     */
    fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
    
    /**
     * Get current timestamp in seconds
     */
    fun currentTimeSeconds(): Long {
        return System.currentTimeMillis() / 1000
    }
}
