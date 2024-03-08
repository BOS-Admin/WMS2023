package Model

public data class ClassBRepriceResponseModel (

    val PrevSalesPrice :Double,
    val PrevLetter :String,
    val PrevUSDPrice: String,
    val NewSalesPrice: Double,
    val NewLetter: String,
    val NewUSDPrice: String,
    var ItemSerial: String?
)

public data class BrandInToIsResponseModel (

    val Company :String?,
    val PrevPrice :String?,
    val Letter :String?,
    val NewPrice: String?,
    val ItemSerial:String?,
    val ItemNo:String?
)