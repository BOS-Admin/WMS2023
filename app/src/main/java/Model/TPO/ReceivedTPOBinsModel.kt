package Model.TPO

class ReceivedTPOBinsModel
{
    var ValidTPOS: List<Int>? = null
    var BinsIDs: List<Int>? = null
    var CurrentLocation : String? = null
    var UserID : Int? = null

    constructor()

    constructor(ValidTPOS: List<Int>?, BinsIDs: List<Int>?, CurrentLocation : String?, UserID : Int?) {
        this.ValidTPOS = ValidTPOS
        this.BinsIDs = BinsIDs
        this.CurrentLocation = CurrentLocation
        this.UserID = UserID
    }


}