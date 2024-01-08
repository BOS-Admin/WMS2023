package Model


data class CycleCountModel (

    val bins : List<CycleCountModelBin>,
    val cycleCountID : Int,
    val rackID : Int,
    val rackCode : String,
    val userID : Int
)

data class CycleCountModelBin (
    val binCode : String,
    val index : Int,
    val locationName : String,
    val locationID : Int,
    var NbFailures : Int=0,
    var IsCurrent : Boolean=false,
    var IsDone : Boolean=false
)

data class ValidateCycleCountModel (

    var CycleCountBinID: Int,
    var BinRFID: String,
    var WeightMac: String,
    var RFIDMac: String,
    var DeviceMac: String,
    var UserID: Int,
    var Weight: Float,
    var RFIDItems: Array<String>
)


