package com.hc.mixthebluetooth.Model
data class RfidSessionModel (
    var id: Int?=null,
    var stationCode: String?="",
    var upc: String?="",
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
    var rfidLotBondStart: String?=null,
    var rfidLotBondStop: String?=null,
    var rfidLotBondMessage: String?="",
    var rfidMsgSentDate: String?=null,
    var rfidMsgRecDate: String?=null,
    var rfidMsgSent: String?="",
    var rfidMsgRec: String?="",
    var userId: Int?=-1,
    var lotNo:String?="",
    var cartonNo:String?=""


)



