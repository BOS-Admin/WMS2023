package Model.BosApp.Transfer


 data class BinModelItem (
    val id:Int,
    val transferId:Int,
    val binId:Int,
    val Count:Int,
    val PackReasonID:Int,
    val DateAdded:String,
    val binBarcode:String,
    val transferStatus:Int,
    val transferNextStatus:String,
    val currentTransferCode:String,

)