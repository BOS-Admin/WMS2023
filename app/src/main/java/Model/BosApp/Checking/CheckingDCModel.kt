package Model.BosApp.Checking

import com.google.android.datatransport.runtime.Destination


data class CheckingDCModel (
    val binBarcode : String,
    val userID : Int,
    val itemSerials : List<CheckingDCModelItem>,
    val packingReasonType : Int,
    val locationId : Int,
)

data class CheckingDCModelItem (
    val itemCode : String
)
