package Model

class PickItemSerialModel {
    var PickingWaveID: Int? =null
    var ItemSerialID: Int? =null
    var BinID: Int? =null
    var UserID: Int? =null
    constructor()
    constructor( PickingWaveID: Int?,ItemSerialID: Int?,UserID: Int?,BinID: Int?,) {
        this.PickingWaveID = PickingWaveID
        this.UserID = UserID
        this.ItemSerialID=ItemSerialID
        this.BinID=BinID
    }
}
