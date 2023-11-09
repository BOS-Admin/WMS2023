package com.hc.mixthebluetooth.Model

public class ValidateRFIdsModel {

    val StationCode : String;
    val RepairTypeId : Int;
    val RFIds : List<String>;

    constructor(StationCode: String, RepairTypeId: Int, RFIds: List<String>) {
        this.StationCode = StationCode
        this.RepairTypeId = RepairTypeId
        this.RFIds = RFIds
    }
}