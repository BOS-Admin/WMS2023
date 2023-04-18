package Model.Pricing

import retrofit2.http.POST
import retrofit2.http.Query

data class PricingStandModel (
    val id : Int,
    val UserId : Int,
    val dateAdded : String,
    val pricingLineCode : String,
    val printerName : String,
)

