package com.hc.mixthebluetooth.Model

public data class RFIDPackingInfoReceivedModel (

    val status:Int,
    val message:String,
    val rfid:String,
    val itemSerial:String,
    val details: String,

    )