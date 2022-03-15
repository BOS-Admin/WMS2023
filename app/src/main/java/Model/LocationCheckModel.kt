package Model
public data class LocationCheckModel (
    val shelfs : List<LocationCheckModelShelf>,
    val locationCheckID : Int,
    val rackID : Int,
    val rackCode : String,
    val userID : Int
)
data class LocationCheckModelShelf (
    var bins : List<LocationCheckModelBin>,
    val locationCheckShelfID : Int,
    val locationID : Int,
    val locationName : String,
    val locationBarcode : String,
    var DoneLocationCheck : Boolean,
    val userID : Int
)
data class LocationCheckModelBin (
    val binCode : String,
    val index : Int
)