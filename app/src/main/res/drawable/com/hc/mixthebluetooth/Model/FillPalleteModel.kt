package Model
data class FillPalleteModel (

    val palleteCode : String,
    val binCodes : String,
    val userID : Int,
    val binTypeID : Int,
    val bins : List<FillPalleteModelBin>
)

data class FillPalleteModelBin (

    val binCode : String
)

data class FillPalleteModelBinValidation (

    val palleteCode : String,
    val binCode : String,
    val userID : Int,
    val binTypeID : Int,
    val nbOfBins : Int
)

data class FillPalleteModelBinValidationResponse (
    val status : String,
    val palleteCode : String,
    val binCode : String,
    val statusID : Int,
    val binTypeID : Int,
    val minPalleteBins : Int,
    val maxPalleteBins : Int
)