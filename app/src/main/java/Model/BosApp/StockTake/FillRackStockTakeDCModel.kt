package Model.BosApp.StockTake

import retrofit2.http.POST
import retrofit2.http.Query

data class FillRackStockTakeDCModel (
    val Barcode : String,
    val userID : Int,
    val bins : List<FillRackStockTakeDCModelItem>,
    val locationId : Int,
)

data class FillRackStockTakeDCModelItem (
    val itemCode : String
)

