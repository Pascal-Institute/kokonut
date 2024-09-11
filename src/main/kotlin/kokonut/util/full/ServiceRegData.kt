package kokonut.util.full

import kotlinx.serialization.Serializable

@Serializable
data class ServiceRegData(
   val Name : String,
   val ID : String,
   val Address : String,
   val Port : Int,
   val Check: HealthCheck? = null
)
