package Model.Pricing

import retrofit2.http.POST
import retrofit2.http.Query

data class UPCPricingItemModel (
    val UserId : Int,
    val ItemSerial : String,
    val UPC : String,
    val PricingStandId : Int,

)

