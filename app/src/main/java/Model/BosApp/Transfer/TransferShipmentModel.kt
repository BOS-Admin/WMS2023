package Model.BosApp.Transfer

import retrofit2.http.POST
import retrofit2.http.Query

data class TransferShipmentModel (
    val FromLocation : String,
    val ToLocation : String,
    val userID : Int,
    val BinBarcodes : List<TransferShipmentItemModel>,
)

data class TransferShipmentItemModel (
    val itemCode : String
)

