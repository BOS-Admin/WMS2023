package Model.TPO

class ReceivedTPOBinsModel
{
    var BinIDS: List<Int>? = null
    var OverrideBins: List<OverrideBinModel>? = null
    var UsedOverridePasswords: List<String>? = null
    var CurrentLocation : String? = null
    var UserID : Int? = null

    constructor()

    constructor(BinIDS: List<Int>?, OverrideBins: List<OverrideBinModel>?, UsedOverridePasswords : List<String>?, CurrentLocation : String?, UserID : Int?) {
        this.BinIDS = BinIDS
        this.OverrideBins = OverrideBins
        this.UsedOverridePasswords = UsedOverridePasswords
        this.CurrentLocation = CurrentLocation
        this.UserID = UserID
    }


}

class OverrideBinModel
{
    var Barcode: String? = null
    var TruckBarcode: String? = null
    var PasswordUsed: String? = null

    constructor()
    constructor(Barcode: String?, TruckBarcode: String?, PasswordUsed: String?) {
        this.Barcode = Barcode
        this.TruckBarcode = TruckBarcode
        this.PasswordUsed = PasswordUsed
    }


}