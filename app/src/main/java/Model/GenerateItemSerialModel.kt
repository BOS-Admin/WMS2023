package Model

class GenerateItemSerialModel {

    var FoldingItemModel: FoldingItemModel? = null
    var PrintType: Int? = null
    constructor()
    constructor(
            FoldingItemModel: FoldingItemModel?,
            PrintType: Int?,
    ) {
        this.FoldingItemModel = FoldingItemModel
        this.PrintType = PrintType
    }

}