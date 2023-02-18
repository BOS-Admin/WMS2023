package Model.BosApp.Transfer

import retrofit2.http.POST
import retrofit2.http.Query

data class TransferDCModel (
    val binBarcode : String,
    val userID : Int,
    val itemSerials : List<TransferDCModelItem>,
    val locationId : Int,
)

data class TransferDCModelItem (
    val itemCode : String
)

