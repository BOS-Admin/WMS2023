package Model.Pricing

data class PricingStandModel (
    val id : Int,
    val userId : Int,
    val dateAdded : String,
    val pricingLineCode : String,
    val printerName : String,
)

