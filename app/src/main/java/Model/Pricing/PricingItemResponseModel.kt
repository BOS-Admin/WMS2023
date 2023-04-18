package Model.Pricing

import retrofit2.http.POST
import retrofit2.http.Query

data class PricingItemResponseModel (
    val ItemSerial : String,
    val Price:Float


)

