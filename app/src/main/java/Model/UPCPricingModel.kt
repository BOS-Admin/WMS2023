package Model.BosApp.Transfer

import retrofit2.http.POST
import retrofit2.http.Query

data class UPCPricingModel (
    val UserID : Int,
    val ItemSerials : List<String>,
    val ItemUPCs : List<String>,
    val PricingLineCode : String,
)

