package Model.BosApp

public data class BinModel (

    val id:Int,
    val binTrxCode:String,
    val binBarcode:String,
    val binTypeId:Int,
    val binRfidepc:String,
    val binRfidText:String,
    val statusId:Int,
    val status:String,
    val locationId:Int,
    val location:String,
    val rackId:Int,
    val rackCode:String,
    val dateAdded:String,
    val userId:Int,
    val lastEdited:String,
    val lastEditedBy:Int,
    val isActive:String,
    val actualWeight:Double,
    val locationTypeId:Int,
    val depthValue:Int,
    val lastLocationId:Int,
    val lastLocationIndex:Int,
    val lastLocationDate:String,
    val lastCycleCountStatusId:Int,
    val lastCycleCountStatus:String,
    val lastCycleCountDate:String,
    val palleteId:Int,
    val palleteCode:String,
    val fillBinId:Int,
    val nextStatus:String,
    val currentTransferCode:String,
    val currentPackingTypeId:Int

    )