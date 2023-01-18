package com.bos.wms.mlkit.app.bosApp.models
import Model.UserLoginModel
import Model.UserLoginResultModel
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
interface QuotesAPI {

        @GET("/quotes")
         fun getQuotes(): Observable<QuoteList>


}