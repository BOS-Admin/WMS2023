package Model
data class PutPalleteOnShelfModel (

    val palleteCode : String,
    val locationCode : String,
    val userID : Int
)

public data class BoxTypeModel (

    val id : Int,
    val type : String,
    val prefix : String,
    val description : String,
)