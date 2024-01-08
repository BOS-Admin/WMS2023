package Model

class RFIDPackingInfoModel {
    var BinBarcode: String? =null
    var RFIDS: ArrayList<String>? =null
    var Location: String? =null
    var UserID: Int? =null
    var PackReasonId: Int?=null

    constructor(BinBarcode: String?, RFIDS: ArrayList<String>?,Location: String?,UserID: Int?, PackReasonId: Int?) {
        this.BinBarcode = BinBarcode
        this.UserID = UserID
        this.RFIDS=RFIDS
        this.Location=Location
        this.UserID=UserID
        this.PackReasonId=PackReasonId
    }
}