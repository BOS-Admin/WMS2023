package Model.BosApp

import Model.BosApp.Packing.FillBinDCModelItem

public data class BinModelItem2 (
    val id:Int,
    val binBarcode:String,
    val locationId:Int,
    val location:String,
    val currentTransferCode:String,
    val binCount:Int,
    val transferStatus:Int,
    val receivingStatus:Int,
    val currentReceivingCode:String,
    val isReceiving:Boolean,
    val receivingNextStatus:String,
    val transferNextStatus:String,


    val receivingPaletteBins : List<ReceivingPaletteBin>?,

    )

data class ReceivingPaletteBin (
    val binBarcode : String,
    val message : String
)
