package Model.BosApp

public data class BinModelItem1 (
    val Id :Int,
    val TransferId :Int,
val BinId :Int,
val Count:Int,
val PackReasonID:Int,
val DateAdded:String,
val BinBarcode:String,
var TransferStatus:Int,
var TransferNextStatus:String,
val CurrentTransferCode:String,
var Received:Int

    )