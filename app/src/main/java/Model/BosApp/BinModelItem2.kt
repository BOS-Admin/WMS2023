package Model.BosApp

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
val transferNextStatus:String

    )