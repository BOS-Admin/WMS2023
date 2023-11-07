package Remote

import Model.*
import Model.BosApp.*
import Model.BosApp.BinModelItem
import Model.BosApp.Checking.CheckingDCModel
import Model.BosApp.Packing.FillBinDCModel
import Model.BosApp.StockTake.FillRackStockTakeDCModel
import Model.BosApp.Transfer.*
import Model.Pricing.PricingItemResponseModel
import Model.Pricing.PricingStandModel
import Model.Pricing.QuatroPricingItemModel
import Model.Pricing.UPCPricingItemModel
import Model.TPO.ReceivedTPOBinsModel
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface BasicApi {

    /* Transfer Preparation Order */

    @POST("api/reprice/store/")
    fun Reprice( @Query("UserId") UserId: Int,
                 @Query("ItemCode") BoxBarcode:String,
                 @Query("Store") Store:String
    ): Observable<ClassBRepriceResponseModel>

    @GET("api/reprice/store/getStoreRepriceCount")
    fun GetRepriceCount(  @Query("Store") Store:String
    ): Observable<ClassBRepriceCountResponse>





    @GET("api/TPO/GetAllTransferLocations")
    fun GetAllTransferLocations(@Query("CurrentLocation") CurrentLocation: String): Observable<ResponseBody>

    @GET("api/TPO/GetAllAvailableSendingTPOS")
    fun GetAllAvailableSendingTPOS(@Query("CurrentLocation") CurrentLocation: String): Observable<ResponseBody>

    @GET("api/TPO/GetAllTruckSendingTPOS")
    fun GetAllTruckSendingTPOS(@Query("CurrentLocation") CurrentLocation: String): Observable<ResponseBody>

    @GET("api/TPO/GetAvailableTPOBins")
    fun GetAvailableTPOBins(@Query("TPOID") TPOID: Int): Observable<ResponseBody>

    @GET("api/TPO/VerifyTPOBinAdd")
    fun VerifyTPOBinAdd(@Query("TPOID") TPOID: Int, @Query("BinBarcode") BinBarcode: String, @Query("UserID") UserID: Int): Observable<ResponseBody>

    @GET("api/TPO/RemoveBinFromTPO")
    fun RemoveBinFromTPO(@Query("TPOID") TPOID: Int, @Query("BinBarcode") BinBarcode: String): Observable<ResponseBody>

    @POST("api/TPO/ReadyTPOBins")
    fun ReadyTPOBins(@Query("TPOID") TPOID: Int, @Query("UserID") UserID: Int): Observable<ResponseBody>

    @GET("api/TPO/CreateTPO")
    fun CreateTPO(@Query("FromLocation") FromLocation: String, @Query("ToLocation") ToLocation: String, @Query("UserID") UserID: Int): Observable<ResponseBody>

    @GET("api/OverridePasswords/ValidateAuthToken")
    fun ValidateAuthToken(): Observable<ResponseBody>

    @GET("api/TPO/VerifyTPOTruckForShipment")//Checks If We Can Add Boxes To The Truck Or Count The Trucks Boxes
    fun VerifyTPOTruckForShipment(@Query("TruckBarcode") TruckBarcode: String, @Query("Count") Count: Boolean): Observable<ResponseBody>

    @GET("api/TPO/StartTruckBinCount")//Sets The Truck In Box Counting Mode
    fun StartTruckBinCount(@Query("TruckBarcode") TruckBarcode: String): Observable<ResponseBody>

    @GET("api/TPO/ResetTruckBinCount")//Reset The Trucks Count Mode, Either Stop The Count Or Stop The Adding
    fun ResetTruckBinCount(@Query("TruckBarcode") TruckBarcode: String, @Query("Count") Count: Boolean): Observable<ResponseBody>

    @GET("api/TPO/VerifyTPOBinShipment")//Adds The Box To A Truck And Sets The Truck In Adding Mode
    fun VerifyTPOBinShipment(@Query("TPOID") TPOID: Int, @Query("TruckBarcode") TruckBarcode: String,@Query("BinBarcode") BinBarcode: String, @Query("UserID") UserID: Int): Observable<ResponseBody>

    @GET("api/TPO/VerifyTPOBinShipmentCount")//Submits The Truck Count Number
    fun VerifyTPOBinShipmentCount(@Query("TPOID") TPOID: Int, @Query("TruckBarcode") TruckBarcode: String,@Query("Count") Count: Int, @Query("UserID") UserID: Int): Observable<ResponseBody>

    @GET("api/TPO/AttemptTPOShipmentPrepared")//Submits The Truck Bins And Checks If The Count Matches
    fun AttemptTPOShipmentPrepared(@Query("TPOID") TPOID: Int, @Query("TruckBarcode") TruckBarcode: String, @Query("UserID") UserID: Int): Observable<ResponseBody>

    @GET("api/TPO/GetAllAvailableReceivingTPOSData")//Gets All The Bins That Are Supposed To Be Received
    fun GetAllAvailableReceivingTPOSData(@Query("CurrentLocation") CurrentLocation: String): Observable<ResponseBody>

    @POST("api/TPO/AddReceivedTPOBins")
    fun AddReceivedTPOBins(@Body model: ReceivedTPOBinsModel): Observable<ResponseBody>

    @GET("api/OverridePasswords/GetAvailablePasswords")
    fun GetAllOverridePasswords(@Query("PasswordCategory") PasswordCategory: String): Observable<List<String>>

    @GET("api/TPO/ResetTruckBinCountMode")
    fun ResetTruckBinCountMode(@Query("TruckBarcode") TruckBarcode: String): Observable<ResponseBody>

    @GET("api/TPO/GetReceivingTransfers")
    fun GetReceivingTransfers(@Query("CurrentLocation") CurrentLocation: String): Observable<ResponseBody>




    @POST("api/wms/pricing/upc/overrideItem")
    fun OverrideItemPrice(@Body model: UPCPricingItemModel): Observable<PricingItemResponseModel>
    @POST("api/wms/pricing/itempricing/item")
    fun PriceItem(@Body model: UPCPricingItemModel): Observable<PricingItemResponseModel>

    @POST("api/wms/pricing/quatro/item")
    fun PriceQuatroItem(@Body model: QuatroPricingItemModel): Observable<PricingItemResponseModel>
    @POST("api/wms/pricing/upc/item")
    fun PriceUPCItem(@Body model: UPCPricingItemModel): Observable<PricingItemResponseModel>
    @POST("api/wms/pricing/stand/create")
    fun CreatePricingStand(@Query("UserId") UserId: Int, @Query("PricingLineCode") PricingLineCode: String): Observable<PricingStandModel>
    @POST("api/wms/pricing/printing/order/stand")
    fun SendPrintingStand(@Body model: PricingStandModel): Observable<ResponseBody>

    @GET("api/ItemSerial/GetRFIDItemSerial")
    fun GetRFIDItemSerial(@Query("rfid") rfid: String): Observable<String>

    @GET("api/UserPermissions/GetPermissions")
    fun GetPermissions(@Query("appName") appName:String, @Query("appVersion") appVersion:String, @Query("userID") userID:Int): Observable<List<String>>

    @GET("api/SystemControl/GetValue")
    fun GetSystemControlValue(
        @Query("code") code: String,
    ): Observable<Int>

    @POST("api/EmptyBin")
    fun EmptyBin(@Query("UserID") UserID: Int, @Query("binBarcode") binBarcode: String): Observable<String>

    @GET("api/EmptyBin/CheckBin")
    fun CheckBinEmpty(@Query("UserID") UserID: Int, @Query("binBarcode") binBarcode: String): Observable<EmptyBinModel>

    @POST("api/BolRecognition")
    fun ProceedBolFailedUpload(@Body ocr: BolRecognitionModel): Observable<String>

    @POST("api/LotLines/ValidateBol")
    fun ValidateBol(@Query("userId") userId:Int, @Body model: List<String>): Observable<ResponseBody>

    @POST("api/LotLines/GetBolByUPCs")
    fun GetBolByUPCs(@Query("UserID") UserID:Int, @Body model: List<String>): Observable<ResponseBody>

    @GET("api/Login/GetRole")
    fun GetUserRole(
        @Query("Id") UserId: Int): Observable<ResponseBody>
    @POST("api/Transfer/TransferShipment")
    fun TransferShipment(
      @Body model: TransferShipmentModel
    ): Observable<ResponseBody>

    @GET("api/StockTake/ValidateRackPutAway")
    fun ValidateRackPutAway(
        @Query("rackCode") ItemCode: String): Observable<RackModelItem>

    @GET("api/StockTake/ValidateBinForPutAway")
    fun ValidateBinForPutAway(
        @Query("binBarcode") ItemCode: String
    ): Observable<ResponseBody>


    @POST("api/StockTake/RackPutAway")
    fun RackPutAway(@Body model:PutAwayModel):Observable<ResponseBody>


    @POST("api/Transfer/EndProcess")
    fun EndTransferReceivingProcess(
        @Query("NavNo") NavNo: String,
        @Query("LocationCode") LocationCode: String,
    ): Observable<ResponseBody>
    @POST("api/StockTake/FillRackStockTakeDC")
    fun FillRackStockTake(@Body model: FillRackStockTakeDCModel): Observable<ResponseBody>
    @POST("api/FillBinBarcodes/ValidateCheckingDC")
    fun CheckingDC(@Body model: CheckingDCModel): Observable<ResponseBody>
    @POST("api/FillBinBarcodes/FillBinDC")
    fun FillBinDC(@Body model: FillBinDCModel): Observable<ResponseBody>
    @POST("api/StockTake/FillBinStockTake1")
    fun FillBinStockTake1(@Body model: FillStockTakeDCModel): Observable<ResponseBody>
    @POST("api/Transfer/TransferDC")
    fun TransferDC(@Body model: TransferDCModel
    ): Observable<ResponseBody>



    @GET("api/Transfer/ValidateTransferItem")
    fun ValidateTransferItem(
        @Query("ItemCode") ItemCode: String,
        @Query("Location") Location:String,
        @Query("UserID") UserID: Int
    ): Observable<ResponseBody>

    @GET("api/Transfer/CheckTransferBinCount")
    fun CheckTransferBinCount(
        @Query("UserID") UserID: Int,
        @Query("Count") ItemSerials: Int,
        @Query("BinBarcode") BinBarcode: String,
        @Query("LocationId") LocationId:Int,
        @Query("TransferNavNo") transferNavNo:String,
    ): Observable<ResponseBody>

    @GET("/api/Transfer/GetBinInfo")
    fun GetBinTransferInfo(
        @Query("binBarcode") binBarcode: String,
    ): Observable<BinModelItem2>

    @GET("api/Transfer/GetTransferBins")
    fun GetTransferBins(@Query("transferNavNo") transferNavNo:String): Observable<List<BinModelItem1>>


    @GET("api/Transfer/ValidateBinForTransfer")
    fun ValidateBinForTransfer(
        @Query("BinBarcode") ItemCode: String,
        @Query("FromLocationBarcode") FromLocation: String,
        @Query("ToLocationBarcode") ToLocation: String,
    ): Observable<ResponseBody>



    @GET("/api/Transfer/ValidateReceiving")
    fun ValidateReceiving(
        @Query("locationCode") binBarcode: String,
        @Query("navNo") navNo: String
    ): Observable<ResponseBody>



    @GET("api/FillBin/GetPrintSections")
    fun GetPrintSections(@Query("locationId") locationId:String): Observable<List<PrinterModelItem>>

    @GET("api/FillBin/GetPackReasons")
    fun GetPackReasons(): Observable<List<PackReasonModelItem>>

    @GET("/api/FillBin/ValidateBin")
    fun ValidateBin(
        @Query("binBarcode") binBarcode: String,
        @Query("packingTypeId") packingTypeId: Int,
        @Query("userId") userId: Int,
        @Query("locationId") locationId: Int
    ): Observable<BinModelItem>

    @GET("/api/StockTake/ValidateBin")
    fun ValidateBinForStockTake(
        @Query("binBarcode") binBarcode: String,
        @Query("transactionType") transactionType: Int,
        @Query("userId") userId: Int,
        @Query("locationId") locationId: Int
    ): Observable<BinModelItem>


    @GET("api/FillBinBarcodes/ValidateBinForCount")
    fun ValidateBinForCount(
        @Query("binBarcode") binBarcode: String,
        @Query("packingTypeId") packingTypeId: Int,
        @Query("userId") userId: Int,
        @Query("locationId") locationId: Int
    ): Observable<ResponseBody>




    @GET("api/FillBinBarcodes/FillBinCount")
    fun FillBinCount(
        @Query("UserID") UserID: Int,
        @Query("Count") ItemSerials: Int,
        @Query("BinBarcode") BinBarcode: String,
        @Query("LocationId") LocationId:Int
    ): Observable<ResponseBody>


    @GET("api/FillBinBarcodes/ValidateFillBinItem")
    fun ValidateFillBinItem(
        @Query("ItemCode") ItemCode: String,
        @Query("Location") Location: String,
        @Query("UserId") UserId: Int=-1,
        @Query("PackReasonId") PackReasonId: Int): Observable<ResponseBody>


    @GET("api/StockTake/ValidateItem")
    fun ValidateStockTakeItem(@Query("ItemCode") ItemCode: String): Observable<ResponseBody>



    @GET("api/StockTake/ValidateReleasedBin")
    fun ValidateStockTakeItemForRack(@Query("binBarcode") ItemCode: String): Observable<ResponseBody>

    @GET("api/StockTake/ValidateRack")
    fun ValidateRack(
        @Query("transactionType") transactionType: Int,
        @Query("rackCode") ItemCode: String): Observable<RackModelItem>

    @GET("api/FillBinBarcodes/ValidateBinForChecking")
    fun ValidateBinForChecking(
        @Query("BinBarcode") BinBarcode: String,
        @Query("UserID") UserID: Int,
        @Query("LocationId") LocationId: Int
    ): Observable<ResponseBody>

    @GET("api/FillBinBarcodes/ValidateFillBinCheckItem")
    fun ValidateFillBinCheckingItem(
        @Query("ItemCode") ItemCode: String,
        @Query("Location") Location: String,
        @Query("UserId") UserId: Int): Observable<ResponseBody>

    @GET("api/FillBinBarcodes/ValidateChecking")
    fun ValidateChecking(
        @Query("UserID") UserID: Int,
        @Query("ItemSerials") ItemSerials: String,
        @Query("BinBarcode") BinBarcode: String,
        @Query("PackingReasonType") PackingReasonType :Int,
        @Query("LocationID") LocationId:Int
    ): Observable<ResponseBody>

    @POST("api/StockTake/FillBinStockTake")
    fun FillBinStockTake(
        @Query("UserID") UserID: Int,
        @Query("ItemSerials") ItemSerials: String,
        @Query("BinBarcode") BinBarcode: String,
        @Query("PackingReasonType") PackingReasonType :Int,
        @Query("LocationId") LocationId:Int
    ): Observable<ResponseBody>






    @POST("api/FillBinBarcodes")
    fun PostPackingItems(
        @Query("UserID") UserID: Int,
        @Query("ItemSerials") ItemSerials: String,
        @Query("BinBarcode") BinBarcode: String,
        @Query("PackingReasonType") PackingReasonType :Int,
        @Query("Destination") Destination:String,
        @Query("LocationId") LocationId:Int
    ): Observable<ResponseBody>


    @POST("api/StockTake/FillRackStockTake")
    fun FillRackStockTake(
        @Query("UserID") UserID: Int,
        @Query("Items") ItemSerials: String,
        @Query("RackBarcode") BinBarcode: String,
        @Query("LocationId") LocationId:Int
    ): Observable<ResponseBody>


    @GET("api/StockTake/ValidateRackCount")
    fun ValidateRackCountStockTake(
        @Query("UserID") UserID: Int,
        @Query("RackBarcode") RackBarcode: String,
        @Query("LocationId") LocationId:Int,
        @Query("Count") ItemSerials: Int
    ): Observable<ResponseBody>

    @GET("api/StockTake/ValidateBinCount")
    fun ValidatBinCountStockTake(
        @Query("UserID") UserID: Int,
        @Query("Count") ItemSerials: Int,
        @Query("BinBarcode") RackBarcode: String,
        @Query("LocationId") LocationId:Int
    ): Observable<ResponseBody>



    @POST("api/Login")
    fun Login(@Body usr: UserLoginModel): Observable<UserLoginResultModel>

    @GET("api/ItemOCR/{id}")
    fun CheckBarcodeOCRDone( @Path("id") id: String): Observable<Boolean>

    @GET("api/ShipmentAppointment")
    fun ValidateShipmentAppointment( @Query("AppointmentNo") AppointmentNo:Int): Observable<Boolean>

    @GET("api/ShipmentBol")
    fun ValidateShipmentBol( @Query("AppointmentNo") AppointmentNo:Int, @Query("BolNumber") BolNumber: Int): Observable<Boolean>

    @POST("api/ItemOCR")
    fun ProceedItemOCR(@Body ocr: ItemOCRModel): Observable<ResponseBody>

    @POST("api/AssignPickingList")
    fun AssignPickingList(@Body UserID: Int): Observable<Boolean>
    @POST("api/PickItemSerial")
    fun PickItemSerial(@Body Model: PickItemSerialModel): Observable<Boolean>
    @POST("api/GetPickingListItems")
    fun GetPickingListItems(@Body UserID: Int): Observable<PickingListItemModel>

    @POST("api/NextReceivingStatus")
    fun NextReceivingStatus(@Body Model: NextReceivingStatusModel): Observable<Int>

    @GET("api/Location")
    fun GetLocation(@Query("IPAddress") IPAddress:String,@Query("LocationTypeID") LocationTypeID: Int, @Query("UserID") UserID: Int): Observable<LocationModel>

    @POST("api/AssignLocationCheck")
    fun AssignLocationCheck(@Query("UserID") UserID:Int, @Query("FloorID") FloorID: Int): Observable<ResponseBody>

    @GET("api/LocationCheck")
    fun LocationCheck(@Query("UserID") UserID:Int,@Query("FloorID") FloorID: Int): Observable<LocationCheckModel>

    @GET("api/SystemControl")
    fun GetSystemControls(@Query("UserID") UserID:Int,@Query("FloorID") FloorID: Int): Observable<SystemControlModel>

    @POST("api/LocationCheck")
    fun LocationCheck(@Body Model: LocationCheckModelShelf): Observable<ResponseBody>


    @POST("api/FillPalleteBinValidation")
    fun FillPalleteBinValidation(@Body model: FillPalleteModelBinValidation): Observable<FillPalleteModelBinValidationResponse>

    @POST("api/FillPallete")
    fun FillPallete(@Body model: FillPalleteModel): Observable<ResponseBody>

    @POST("api/PutPalleteNextShelf")
    fun PutPalleteNextShelf(@Body model: PutPalleteNextShelfModel): Observable<PutPalleteNextShelfModelResponse>

    @POST("api/PutPalleteOnShelf")
    fun PutPalleteOnShelf(@Body model: PutPalleteOnShelfModel): Observable<ResponseBody>

    @GET("api/ManualFoldingItem")
    fun FoldingItem(@Query("FoldingStationCode") FoldingStationCode:String): Observable<FoldingItem>
    @POST("api/ManualFoldingItem")
    fun FoldingItem(@Body model: FoldingItemModel): Observable<ResponseBody>


    @GET("api/UPCPricing")
    fun ValidateUPCPricing(@Query("ItemSerial") ItemSerial:String,@Query("ItemUPC") ItemUPC:String): Observable<String>
    @POST("api/UPCPricing")
    fun PostUPCPricing(@Body model:UPCPricingModel): Observable<ResponseBody>


    @GET("api/PGPricing")
    fun ValidatePGPricing(@Query("ItemCode") ItemCode:String,@Query("PG") PG:String,@Query("Season") Season:String): Observable<String>
    @POST("api/PGPricing")
    fun PostPGPricing(@Query("UserID") UserID:Int,@Query("PricingLineCode") PricingLineCode:String,@Query("ItemSerials") ItemSerials:String,@Query("PG") PG:String,@Query("Season") Season:String): Observable<ResponseBody>

    @GET("api/GenerateSerials")
    fun InitGenerateSerials(@Query("FoldingStationCode") FoldingStationCode:String): Observable<FoldingItem>
    @POST("api/GenerateSerials")
    fun PostGenerateSerials(@Body model: GenerateItemSerialModel): Observable<ResponseBody>

    @GET("api/ItemSerialMissing")
    fun InitItemSerialMissing(@Query("UserID") UserID:Int,@Query("ItemSerialNo") ItemSerialNo:String): Observable<String>
    @POST("api/ItemSerialMissing")
    fun PostItemSerialMissing(@Query("UserID") UserID:Int,@Query("upc") upc:String,@Query("ItemSerialNo") ItemSerialNo:String,@Query("Perc") Perc:Float): Observable<String>



    @GET("api/ItemPricing")
    fun InitItemPricing(@Query("ItemCode") ItemCode:String): Observable<String>
    @POST("api/ItemPricing")
    fun PostItemPricing(@Body model: ItemPricingModel): Observable<ResponseBody>


    @GET("api/ShipmentReceivingPallete")
    fun InitShipmentReceivingPallete(
        @Query("UserID") UserID:Int,
        @Query("AppointmentNo") AppointmentNo:Int,
        @Query("BolNumber") BolNumber: String,
        @Query("PalleteNb") PalleteNb:Int,
        @Query("NbOfCartons") NbOfCartons:Int,
        @Query("FieldValidate") FieldValidate:Int
    ): Observable<ResponseBody>

    @POST("api/ShipmentReceivingPallete")
    fun ShipmentReceivingPallete(
        @Query("UserID") UserID:Int,
        @Query("AppointmentNo") AppointmentNo:Int,
        @Query("BolNumber") BolNumber: String,
        @Query("PalleteNb") PalleteNb:Int,
        @Query("NbOfCartons") NbOfCartons:Int
    ): Observable<ResponseBody>

    @POST("api/ShipmentReceivingCarton")
    fun ShipmentReceivingCarton(
        @Query("UserID") UserID:Int,
        @Query("BolNumber") BolNumber: String,
        @Query("PalleteNb") PalleteNb:Int,
        @Query("CartonCode") CartonCode:String
    ): Observable<ResponseBody>

    @POST("api/ShipmentReceivingCarton/PostV2")
    fun ShipmentReceivingCartonV2(
        @Query("UserID") UserID:Int,
        @Query("BolNumber") BolNumber: String,
        @Query("PalleteNb") PalleteNb:Int,
        @Query("CartonCode") CartonCode:String
    ): Observable<ResponseBody>

    @POST("api/ShipmentReceivingCartonCount")
    fun ShipmentReceivingCartonCount(
        @Query("UserID") UserID:Int,
        @Query("BolNumber") BolNumber: String,
        @Query("PalleteNb") PalleteNb:Int,
        @Query("NbOfCartons") NbOfCartons:Int,
        @Query("ForceInsert") ForceInsert:Int
    ): Observable<ResponseBody>

}