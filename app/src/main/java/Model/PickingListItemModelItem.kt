package Model
public data class PickingListItemModelItem (
    val id : Int,
    val binID : Int,
    val binCode : String,
    val itemNo : String,
    val itemSerialID : Int,
    val itemSerial : String,
    val rackNb : Int,
    val rackCode : String,
    var DonePicking:Boolean=false
)