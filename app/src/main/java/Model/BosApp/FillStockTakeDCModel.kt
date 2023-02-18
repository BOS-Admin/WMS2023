package Model.BosApp

import retrofit2.http.POST
import retrofit2.http.Query

data class FillStockTakeDCModel (
    val binBarcode : String,
    val userID : Int,
    val itemSerials : List<FillStockTakeDCModelItem>,
    val packingReasonType : Int,
    val locationId : Int,
)

data class FillStockTakeDCModelItem (
    val itemCode : String
)

