package Model

import com.google.android.datatransport.runtime.Destination


data class UPCPricingModel (
    val userID : Int,
    val itemSerials : String,
    val itemUPCs : String,
    val pricingLineCode : String,
)


