package Model

class ItemOCRModel {
    var ItemNo: String? =null
    var UserID: Int? =null
    lateinit var OCRs: Array<String?>
    lateinit var OCRFileNames: Array<String?>

    constructor()
    constructor(ItemNo: String?, UserID: Int?,OCRs: Array<String?>,OCRFileNames: Array<String?>) {
        this.ItemNo = ItemNo
        this.UserID = UserID
        this.OCRs=OCRs
        this.OCRFileNames=OCRFileNames
    }
}
