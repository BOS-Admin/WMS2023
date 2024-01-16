package com.hc.mixthebluetooth.Remote.Routes
import Model.*
import com.hc.mixthebluetooth.Model.*
import com.hc.mixthebluetooth.Model.RfidBulkTypeUpdateSessionModel
import com.hc.mixthebluetooth.Model.RfidLotBond.RfidLotBondSessionModel
import com.hc.mixthebluetooth.Model.RfidSessionModel
//import com.hc.mixthebluetooth.activity.UndoBoxLocationAssignmentActivity
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
interface BasicApi {


    @POST("api/replenishment/distribution/assignBinLocation")
    fun AssignBinLocation( @Query("UserId") UserId: Int,@Query("BoxBarcode") BoxBarcode:String,@Query("StationCode") StationCode:String): Observable<String>
    @POST("api/replenishment/distribution/assignBinLocation/re-assign")
    fun ReAssignBinLocation( @Query("UserId") UserId: Int,
                             @Query("BoxBarcode") BoxBarcode:String,
                             @Query("StationCode") StationCode:String,
                             @Query("Print") Print:Boolean
    ): Observable<AssignBoxLocationResponse>


    @POST("api/replenishment/distribution/assignBinLocation/undo")
    fun Reprice( @Query("UserId") UserId: Int,
                 @Query("ItemCode") BoxBarcode:String,
                 @Query("Store") Store:String
    ): Observable<ResponseBody>





    @GET("api/audit/upcs/get")
    fun GetAuditUPCsToRemove(@Query("UserID") UserID:Int,@Query("BoxBarcode") BoxBarcode:String): Observable<AuditRemoveUPCsResponse>

    @POST("api/audit/upcs/remove")
    fun RemoveAuditUPC(@Query("UserID") UserID:Int,@Query("BoxBarcode") BoxBarcode:String,
                       @Query("ItemCode") ItemCode:String,@Query("BoxLocationScoreId") BoxLocationScoreId:Int): Observable<AuditRemoveUPCsResponse>


    @GET("api/ItemSerial/GetRFIDItemSerial")
    fun GetRFIDItemSerial(@Query("rfid") rfid: String): Observable<String>

    /* RFID Packing */

    @GET("api/RfidPacking/GetRFIDItemNumber")
    fun GetRFIDItemNumber(@Query("rfid") rfid: String): Observable<String>

    @GET("api/RfidPacking/ValidateFillBinItem")
    fun ValidateFillBinRFIDItem(@Query("ItemCode") ItemCode: String, @Query("RFID") RFID: String, @Query("BinBarcode") BinBarcode: String, @Query("Location") Location: String, @Query("UserId") UserId: Int, @Query("PackReasonId") PackReasonId: Int): Observable<ValidatePackItemModel>

    @POST("api/RfidPacking/GetRFIDPackingInfo")
    fun GetRFIDPackingInfo(@Body info: RFIDPackingInfoModel): Observable<List<RFIDPackingInfoReceivedModel>>

    @POST("api/RfidPacking/GetRFIDAuditInfo")
    fun GetRFIDAuditInfo(@Body info: RFIDAuditInfoModel): Observable<List<RFIDAuditInfoReceivedModel>>

    @GET("api/RfidPacking/GetPackReasons")
    fun GetPackReasons(): Observable<List<PackReasonModelItem>>

    @POST("api/FillBinBarcodes/FillBinDC")
    fun FillBinDC(@Body model: FillBinDCModel): Observable<ResponseBody>

    @GET("/api/RfidPacking/ValidateBin")
    fun ValidateBin(
            @Query("binBarcode") binBarcode: String,
            @Query("rfid") rfid: String,
            @Query("packingTypeId") packingTypeId: Int,
            @Query("userId") userId: Int,
            @Query("locationId") locationId: Int
    ): Observable<BinModelItem>

    @GET("api/FillBinBarcodes/FillBinCount")
    fun FillBinCount(
            @Query("UserID") UserID: Int,
            @Query("Count") ItemSerials: Int,
            @Query("BinBarcode") BinBarcode: String,
            @Query("LocationId") LocationId:Int
    ): Observable<ResponseBody>



    @GET("api/rfid/RfidLotBond/Bol/Valid")
    fun ValidateBol(@Query("Bol") Bol: String): Observable<Boolean>
    @GET("api/rfid/RfidLotBond/Carton/Valid")
    fun ValidateCarton(@Query("Carton") Carton: String, @Query("Bol") Bol: String): Observable<Boolean>

    @POST("/api/rfid/RfidLotBondSession/")
    fun postRidLotBondSession(@Body session: RfidLotBondSessionModel): Observable<ResponseBody>
    @POST("/api/rfid/RfidLotBondSession/")
    fun postRidLotBondSessionJava(@Body session: RfidLotBondSessionModel): Call<ResponseBody>

    @POST("api/rfid/rfidLotBondEnhanced")
    fun RFIDLotBondEnhanced(
        @Body SessionModel: RfidLotBondSessionModel
    ): Observable<RfidLotBondSessionModel>

    @POST("api/rfid/rfidlotbondgenerateItemSerial")
    fun rfidLotBondGenerateItemSerial(
        @Body SessionModel: RfidLotBondSessionModel
    ): Observable<RfidLotBondSessionModel>

    @GET("api/rfid/types")
    fun GetRfidTypes(): Observable<List<RfidType>>

    @GET("api/Repair/GetRepairTypes")
    fun GetRepairTypes(): Observable<List<RepairTypeModel>>
    @GET("api/ZPLPrint/CheckPrinterName")
    fun CheckPrinterName(@Query("stationCode") stationCode: String): Observable<PrinterInfoModel>

    @POST("api/ZPLPrint/PrintZplData")
    fun PrintZplData(@Query("RFID") RFID: String,@Query("StationCode") StationCode: String,@Query("UserId") UserId: Int): Observable<ResponseBody>

    @GET("api/ZPLPrint/GetZPLData")
    fun GetZPLData(@Query("rfid") RFID: String,@Query("StationCode") StationCode: String,@Query("UserId") UserId: Int): Observable<ResponseBody>

    @POST("api/Repair/ValidateRFIds")
    fun ValidateRFIds(@Body model: ValidateRFIdsModel): Observable<ItemSerialResponse>

    @POST("api/Repair/RepairTransfer")
    fun RepairTransfer(@Body model: TransferRepairModel): Observable<RepairTransferResponse>

    @POST("api/Repair/ReceiveInRepair")
    fun ReceiveInRepair(@Query("transferNavNo") transferNavNo: String, @Query("userId") userId: Int, @Query("locationCode") locationCode: String): Observable<String>

    @POST("api/Repair/UpdateRepairOutputType")
    fun UpdateRepairOutputType(@Query("itemRFId") itemRFId: String, @Query("repairOutputType") repairOutputType: Int): Observable<String>

    @GET("api/SystemControl/GetValue")
    fun GetSystemControlValue(
        @Query("code") code: String,
    ): Observable<String>

    @GET("api/UserPermissions/GetPermissions")
    fun GetPermissions(@Query("appName") appName:String, @Query("appVersion") appVersion:String, @Query("userID") userID:Int): Observable<List<String>>
    @GET("api/rfid/RfidBulkTypeUpdate/InitRfidBulkTypeUpdate")
    fun InitRfidBulkTypeUpdate(@Query("UserID") UserID:Int,@Query("StationCode") StationCode: String,@Query("TypeBarcode") TypeBarcode: String): Observable<RfidBulkTypeUpdateSessionModel>


    @POST("api/rfid/RfidBulkTypeUpdate")
    fun RfidBulkTypeUpdate(
        @Body SessionModel: RfidBulkTypeUpdateSessionModel): Observable<RfidBulkTypeUpdateSessionModel>



    @POST("api/RfidLotBonding/UpdateSession")
    fun updateRfidLotBondSession(@Body sessionModel: RfidSessionModel): Observable<ResponseBody>

    @POST("api/RFIDLotBonding/PostBulkV3")
    fun RFIDLotBondingAutoBulkV3(
        @Body SessionModel: RfidLotBondSessionModel): Observable<RfidLotBondSessionModel>



    @GET("api/RfidLotBonding/SendStationMessage")
    fun SendStationMessage(
        @Query("StationAddress") StationAddress: String,
        @Query("PortNo") PortNo: Int,
        @Query("Message") Message: String,
        @Query("TimeOut") TimeOut: Int
    ): Observable<ResponseBody>


    @GET("api/RfidLotBonding/InitRfidLotBond")
    fun InitRfidLotBond(@Query("UserId") UserId:Int,
         @Query("UPC") UPC:String,
         @Query("StationAddress") StationAddress: String,
         @Query("PortNo") PortNo: Int,
         @Query("Message") Message: String,
         @Query("TimeOut") TimeOut: Int
    ): Observable<RfidSessionModel>

    @GET("api/RfidLotBondStation/{code}")
    fun GetRfidLotBondStation(@Path("code") code:String): Observable<RfidStationModel>

    @GET("api/RFIDLotBonding")
    fun InitRFIDLotBonding1(@Query("UserID") UserID:Int,@Query("LotNumber") LotNumber: String): Observable<ResponseBody>


    @GET("api/RFIDLotBonding")
    fun InitRFIDLotBonding(@Query("UserID") UserID:Int,@Query("LotNumber") LotNumber: String): Call<ResponseBody>
    @GET("api/RFIDLotBonding/MoveClient")
    fun MoveConveyor(@Query("StationCode") LotBondStation:String ): Call<ResponseBody>

    @POST("api/RFIDLotBonding/PostBulk")
    fun RFIDLotBondingAutoBulk(@Query("UserID") UserID:Int,@Query("LotNumber") LotNumber:String, @Query("RFID") RFID: String, @Query("UPC") UPC: String,@Query("CartonNumber") CARTOON_NUMBER: String,@Query("StationCode") StationCode: String): Observable<RfidSessionModel>

    @POST("api/Login")
    fun Login(@Body usr: UserLoginModel): Observable<UserLoginResultModel>

    @GET("api/ItemOCR/{id}")
    fun CheckBarcodeOCRDone( @Path("id") id: String): Observable<Boolean>

    @POST("api/FillBin")
    fun ProceedFillBin(@Body bin: FillBinModel):  Call<ResponseBody>

    @POST("api/FillPalleteRFID")
    fun ProceedFillPalleteRFID(@Body bin: FillPalleteRFIDModel):  Call<ResponseBody>

    @GET("api/Location")
    fun GetLocation(@Query("IPAddress") IPAddress:String,@Query("LocationTypeID") LocationTypeID: Int, @Query("UserID") UserID: Int): Observable<LocationModel>

    @POST("api/AssignCycleCount")
    fun AssignCycleCount(@Query("UserID") UserID:Int, @Query("FloorID") FloorID: Int): Observable<ResponseBody>

    @GET("api/CycleCount")
    fun CycleCount(@Query("UserID") UserID:Int,@Query("FloorID") FloorID: Int): Call<CycleCountModel>
    @GET("api/SystemControl")
    fun GetSystemControls(@Query("UserID") UserID:Int,@Query("FloorID") FloorID: Int): Observable<SystemControlModel>

    @POST("api/CycleCount")
    fun CycleCount(@Body Model: ValidateCycleCountModel): Call<ResponseBody>
    @POST("api/RFIDUPCMatch")
    fun RFIDUPCMatch(@Query("UserID") UserID:Int, @Query("RFID") RFID: String): Call<ResponseBody>



    @POST("api/RFIDLotBonding")
    fun RFIDLotBonding(@Query("UserID") UserID:Int,@Query("LotNumber") LotNumber:String, @Query("RFID") RFID: String, @Query("UPC") UPC: String): Call<ResponseBody>

    @POST("api/RFIDLotBonding")
    fun RFIDLotBondingAuto(@Query("UserID") UserID:Int,@Query("LotNumber") LotNumber:String, @Query("RFID") RFID: String, @Query("UPC") UPC: String,@Query("CartonNumber") CARTOON_NUMBER: String,@Query("StationCode") StationCode: String): Call<ResponseBody>


    @GET("api/ManualDWSApi")
    fun InitManualDWSApi(@Query("UserID") UserID:Int,@Query("ItemSerial") ItemSerial: String): Call<ResponseBody>

    @POST("api/ManualDWSApi")
    fun ManualDWSApi(@Query("UserID") UserID:Int,@Query("ItemSerial") ItemSerial:String, @Query("RFID") RFID: String, @Query("length") length: Float, @Query("height") height: Float, @Query("width") width: Float, @Query("weight") weight: Float, @Query("BoxCode") BoxCode: String ): Call<ResponseBody>

    @PUT("api/ManualDWSApi")
    fun ValidateManualDWSApi(@Query("UserID") UserID:Int,@Query("ItemSerial") ItemSerial:String, @Query("RFID") RFID: String): Call<ResponseBody>

    @POST("api/FillPallete")
    fun FillPallete(@Body model: FillPalleteModel): Observable<ResponseBody>

    @POST("api/RfidLotBondingItem")
    fun RFIDLotBondingItem(@Body Items:PostListItems): Call<PostListItems>


    @GET("api/NonBBBAudit")
    fun InitNonBBBAudit(@Query("UserID") UserID:Int,@Query("BoxBarcode") BoxBarcode:String, @Query("BoxRFID") BoxRFID: String): Call<AuditRFIDModel>

    @POST("api/NonBBBAudit")
    fun PostNonBBBAudit(@Body Model:AuditRFIDModelPost): Observable<RfidAuditResponse>
}