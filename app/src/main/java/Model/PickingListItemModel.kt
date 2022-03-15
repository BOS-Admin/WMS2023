package Model

public  class  PickingListItemModel (
    val items : List<PickingListItemModelItem>,
    val userID : Int,
    val pickingWaveID : Int,
    val waveNb : Int,
    val serialNb : Int
)