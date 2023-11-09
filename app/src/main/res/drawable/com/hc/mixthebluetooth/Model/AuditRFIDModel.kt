package Model
import com.google.gson.annotations.SerializedName

data class AuditRFIDModel (

    val boxBarcode : String,
    val boxRFID : String,
    val auditID : Int,
    val upcItems : List<UpcItems>,
    val rfidItems : List<RfidItems>
)

data class UpcItems (

    val id : Int,
    val auditId : Int,
    val barcode : String,
    val status : Int,
    val qty : Int,
    val dateAdded : String
)
data class RfidItems (

    val id : Int,
    val auditId : Int,
    val barcode : String,
    val lotBondId:Int,
    val rfid : String,
    val status : Int,
    val qty : Int,
    val dateAdded : String
)

public  class AuditRFIDModelPost {

    var auditID : Int;
    var boxBarcode : String;
    var boxRFID : String;
    var stationCode : String;
    var userID : Int;
    var items : List<AuditRFIDModelPostItems>;
    var assignLocation : Boolean;

    constructor(AuditID:Int,BoxBarcode:String,BoxRFID:String,StationCode:String,UserID:Int,Items : List<AuditRFIDModelPostItems>,AssignLocation:Boolean)
    {
        auditID=AuditID;
        boxBarcode= BoxBarcode;
        boxRFID=BoxRFID;
        userID=UserID;
        items=Items;
        stationCode=StationCode;
        assignLocation=AssignLocation;
    }
}


public  class PostListItems {
    var userID : Int;
    var items : List<String>;

    constructor(UserID:Int,Items: List<String>)
    {
        userID=UserID;
        items=Items;
    }
}


public  class AuditRFIDModelPostItems {

    var upc: String;
    var rfid: String;
    var status: String;
    var statusID: Int;
    var id: Int;
    val lotBondID:Int;
    constructor(ID:Int,LotBondID:Int,UPC: String,RFID: String,StatusID: Int,Status: String)
    {
        id=ID;
        upc=UPC;
        lotBondID=LotBondID;
        rfid=RFID;
        statusID=StatusID;
        status=Status;
    }
}