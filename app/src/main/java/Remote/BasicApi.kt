package Remote

import Model.*
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface BasicApi {

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


    @GET("api/GenerateSerials")
    fun InitGenerateSerials(@Query("FoldingStationCode") FoldingStationCode:String): Observable<FoldingItem>
    @POST("api/GenerateSerials")
    fun PostGenerateSerials(@Body model: FoldingItemModel): Observable<ResponseBody>

    @GET("api/ItemSerialMissing")
    fun InitItemSerialMissing(@Query("ItemCode") ItemCode:String): Observable<String>
    @POST("api/ItemSerialMissing")
    fun PostItemSerialMissing(@Query("UserID") UserID:Int,@Query("Letter") Letter:String,@Query("ItemSerialNo") ItemSerialNo:String): Observable<ResponseBody>



    @GET("api/ItemPricing")
    fun InitItemPricing(@Query("ItemCode") ItemCode:String): Observable<String>
    @POST("api/ItemPricing")
    fun PostItemPricing(@Query("UserID") UserID:Int,@Query("PricingLineCode") PricingLineCode:String,@Query("ItemSerials") ItemSerials:String): Observable<ResponseBody>


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


    @POST("api/ShipmentReceivingCartonCount")
    fun ShipmentReceivingCartonCount(
        @Query("UserID") UserID:Int,
        @Query("BolNumber") BolNumber: String,
        @Query("PalleteNb") PalleteNb:Int,
        @Query("NbOfCartons") NbOfCartons:Int,
        @Query("ForceInsert") ForceInsert:Int
    ): Observable<ResponseBody>

}