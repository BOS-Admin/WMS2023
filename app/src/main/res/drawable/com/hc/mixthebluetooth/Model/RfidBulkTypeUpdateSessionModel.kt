package com.hc.mixthebluetooth.Model
data class RfidBulkTypeUpdateSessionModel (
    var id: Int?=null,
    var stationCode: String?="",
    var conveyorMode:Boolean=false,
    var type: String?="",
    var rfidReadTime: Int=-1,
    var rfidReadStart: String?=null,
    var rfidReadStop: String?=null,
    var rfids: String?="",
    var rfidCount: Int=-1,
    var bulkTypeUpdateMessage: String?="",
    var stationMsgSent: String?="",
    var stationMsgToReceive: String?="",
    var stationMsgReceived: String?="",
    var userId: Int?=-1,
)



