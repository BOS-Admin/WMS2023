package Model.Pricing

import retrofit2.http.POST
import retrofit2.http.Query

data class QuatroPricingItemModel (
    val UserId : Int,
    val ItemSerial : String,
    val ProductGroup : String,
    val Season : String,
    val PricingStandId : Int,

)

