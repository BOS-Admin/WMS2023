package com.hc.mixthebluetooth.Model.RfidLotBond
data class RfidLotBondSessionModel (
    var id: Int?=null,
    var stationCode: String?="",
    var itemSerial: String?="",
    var sessionStartDate: String?=null,
    var checkItemOnConveyorSentDate: String?=null,
    var checkItemOnConveyorRecDate: String?=null,
    var checkItemOnConveyorMsgSent: String?="",
    var checkItemOnConveyorMsgRec: String?="",
    var tatexMsgDate: String?=null,
    var tatexMsgRecDate: String?=null,
    var tatexMsgSent: String?="",
    var tatexMsgRec: String?="",
    var rfidReadStart: String?=null,
    var rfidReadStop: String?=null,
    var rfid: String?="",
    var isOverride: Boolean=false,
    var rfidLotBondStart: String?=null,
    var rfidLotBondStop: String?=null,
    var rfidLotBondProcedureTime: Int?=null,
    var rfidLotBondMessage: String?="",
    var rfidMsgSentDate: String?=null,
    var rfidMsgRecDate: String?=null,
    var rfidMsgSent: String?="",
    var rfidMsgRec: String?="",
    var userId: Int?=-1,
    var bol:String?="",
    var cartonNo:String?="",
    var activity:String?=""

)



