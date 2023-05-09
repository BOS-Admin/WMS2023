package Model.TPO

class ReceivedTPOBinsModel
{
    var BinIDS: List<Int>? = null
    var OverrideBinBarcodes: List<String>? = null
    var UsedOverridePasswords: List<String>? = null
    var CurrentLocation : String? = null
    var UserID : Int? = null

    constructor()

    constructor(BinIDS: List<Int>?, OverrideBinBarcodes: List<String>?, UsedOverridePasswords : List<String>?, CurrentLocation : String?, UserID : Int?) {
        this.BinIDS = BinIDS
        this.OverrideBinBarcodes = OverrideBinBarcodes
        this.UsedOverridePasswords = UsedOverridePasswords
        this.CurrentLocation = CurrentLocation
        this.UserID = UserID
    }


}