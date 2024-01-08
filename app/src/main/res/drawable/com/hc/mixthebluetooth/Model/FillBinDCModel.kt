package com.hc.mixthebluetooth.Model

import com.google.android.datatransport.runtime.Destination


data class FillBinDCModel (
    val binBarcode : String,
    val userID : Int,
    val itemSerials : List<FillBinDCModelItem>,
    val packingReasonType : Int,
    val destination : String,
    val locationId : Int,
)

data class FillBinDCModelItem (
    val itemCode : String
)

