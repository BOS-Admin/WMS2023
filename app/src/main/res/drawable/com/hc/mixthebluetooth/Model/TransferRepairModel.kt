package com.hc.mixthebluetooth.Model

public class TransferRepairModel {

    val StationCode : String;
    val FromLocation : String;
    val ToLocation : String;
    val UserID : Int;
    val Count : Int;
    val RepairTypeId : Int;
    val RFIds : List<String>;

    constructor(StationCode: String, FromLocation : String, ToLocation : String, UserID : Int, Count : Int, RepairTypeId: Int, RFIds: List<String>) {
        this.StationCode = StationCode
        this.FromLocation = FromLocation;
        this.ToLocation = ToLocation;
        this.UserID = UserID;
        this.Count = Count;
        this.RepairTypeId = RepairTypeId
        this.RFIds = RFIds
    }
}