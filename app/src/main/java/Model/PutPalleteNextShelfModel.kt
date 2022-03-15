package Model
data class PutPalleteNextShelfModel (
    val palleteCode : String,
    val LastLocationID : Int,
    val userID : Int,
    val floorID : Int,
)
data class PutPalleteNextShelfModelResponse (
    val userDefinedBarcode : String,
    val locationBarCode : String,
    val rackCellID : Int
)