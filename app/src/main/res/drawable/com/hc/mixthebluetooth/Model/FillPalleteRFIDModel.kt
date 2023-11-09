package Model

class FillPalleteRFIDModel {
    var BinRFID: String? =null
    var WeightMac: String? =null
    var RFIDMac: String? =null
    var DeviceMac: String? =null
    var UserID: Int? =null
    var Weight: Float=0f
    lateinit var RFIDItems: Array<String?>


    constructor(BinRFID: String?, UserID: Int?,RFIDItems: Array<String?>,Weight: Float,WeightMac: String?,RFIDMac: String?,DeviceMac: String?) {
        this.BinRFID = BinRFID
        this.UserID = UserID
        this.RFIDItems=RFIDItems
        this.Weight=Weight
        this.WeightMac=WeightMac
        this.RFIDMac=RFIDMac
        this.DeviceMac=DeviceMac
    }
}
