package Model

data class SystemControlModel (

    val floorID : Int,
    val userID : Int,
    val settings : List<SystemControlModelItem>
)
data class SystemControlModelItem (

    val code : String,
    val value : String
)