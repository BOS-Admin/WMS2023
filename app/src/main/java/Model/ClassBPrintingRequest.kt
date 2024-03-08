package Model

import Model.TPO.OverrideBinModel

class ClassBPrintingRequest {
    var ScannedItemSerials: List<String>? = null
    var Store : String? = null
    var Printer : String? = null
    var UserID: Int? = null

    constructor()

    constructor(Store: String?, Printer: String?, ScannedItemSerials : List<String>?, UserID: Int?) {
        this.ScannedItemSerials = ScannedItemSerials
        this.Store = Store
        this.Printer = Printer
        this.UserID = UserID
    }
}
