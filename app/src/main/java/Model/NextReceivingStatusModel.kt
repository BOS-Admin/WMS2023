package Model

class NextReceivingStatusModel {
    var ItemUPC: String? =null
    var UserID: Int? =null


    constructor()
    constructor(ItemUPC: String?, UserID: Int?) {
        this.ItemUPC = ItemUPC
        this.UserID = UserID
    }
}
