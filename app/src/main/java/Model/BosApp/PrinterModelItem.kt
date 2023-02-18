package Model.BosApp

public data class PrinterModelItem (

    val id:Int,
    val stationCode:String,
    val isActive:Boolean,
    val locationId:String,
    val location:String,
    val printerName:String,
    val headerTemplateId:String,
    val itemTemplateId:String
    )