package com.hc.mixthebluetooth.Model

public data class RepairTypeModel (
    val id : Int,
    val name : String,
    val checkCount : Boolean,
    val minRequiredImages : Int,
    val binPrefix : String
)