package Model.BosApp.Transfer

import retrofit2.http.POST
import retrofit2.http.Query

data class PutAwayModel (
    val rackBarcode : String,
    val userID : Int,
    val binBarcodes : List<PutAwayItemModel>,
)

data class PutAwayItemModel (
    val itemCode : String
)

