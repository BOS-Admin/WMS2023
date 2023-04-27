package Model.BosApp.Transfer

import retrofit2.http.POST
import retrofit2.http.Query

data class ItemPricingModel (
    val UserID : Int,
    val PricingLineCode : String,
    val Items : List<String>,
)

