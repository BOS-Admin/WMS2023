package com.hc.mixthebluetooth.Model

data class ValidatePackItemModel (
        val ItemClass : String,
        val Damaged : Boolean,
        val OffSeason : Boolean,
        val Rejected : Boolean,
        val ZeroPrice : Boolean,
){
    override fun toString(): String {
        return ("Class " + ItemClass + ", Damaged " + Damaged
                + ", OffSeason " + OffSeason + ", Rejected " + Rejected + ", ZeroPrice " + ZeroPrice)
    }

    fun getClassLetter(): String {
        return ItemClass.substring(0, 1)
    }
}