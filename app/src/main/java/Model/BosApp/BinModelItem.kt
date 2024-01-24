package Model.BosApp

public data class BinModelItem (

    val id:Int,
    val binBarcode:String,
    val binRfidepc:String,
    val binRfidText:String,
    val binTypeId:Int,

    )

public data class PaletteModelItem (

    val paletteBarcode:String?,
    )