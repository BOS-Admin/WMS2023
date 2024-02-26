package Model.BosApp.Checking

import com.google.android.datatransport.runtime.Destination
import retrofit2.http.Query


data class CheckingDCModel (
    val binBarcode : String,
    val userID : Int,
    val itemSerials : List<CheckingDCModelItem>,
    val packingReasonType : Int,
    val locationId : Int,
    val isReceiving: Boolean
)

data class CheckingDCModelItem (
    val itemCode : String
)

