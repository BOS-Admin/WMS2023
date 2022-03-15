package Model

public data class FoldingItem (
    val id : Int,
    val stationCode : String,
    val isActive : Boolean,
    val locationId : Int,
    val location : String
)

public data class FoldingItemModel (
    val userID : Int,
    val mainLocation : String,
    val mainLocationID : Int,
    val barcode : String,
    val stationID : Int
)