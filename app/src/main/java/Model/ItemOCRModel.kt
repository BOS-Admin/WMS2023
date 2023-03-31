package Model

class ItemOCRModel {
    var ItemNo: String? =null
    var UPC: String? =null
    var UserID: Int? =null
    lateinit var OCRs: Array<String?>
    lateinit var OCRFileNames: Array<String?>

    constructor()
    constructor(ItemNo: String?, UPC: String?, UserID: Int?,OCRs: Array<String?>,OCRFileNames: Array<String?>) {
        this.ItemNo = ItemNo
        this.UPC = UPC
        this.UserID = UserID
        this.OCRs=OCRs
        this.OCRFileNames=OCRFileNames
    }
}
