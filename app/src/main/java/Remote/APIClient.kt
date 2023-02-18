package Remote

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory


object APIClient {
    private var instance: Retrofit?=null
    private var isFake: Boolean =false
    fun getInstance(IPAddress:String,ForceInit:Boolean,fake:Boolean):Retrofit {
        isFake=fake;
        if (instance == null || ForceInit) {
            //instance = Retrofit.Builder().baseUrl("http://10.188.30.110/BOS_WMS_API/")
            val gson = GsonBuilder()
                .setLenient()
                .create()
            var IP:String=IPAddress;
            if(!IP.endsWith("/"))
                IP=IP+"/";
            if(isFake){
                instance = Retrofit.Builder().baseUrl("https://bosapp.free.beeceptor.com/")

                    //instance = Retrofit.Builder().baseUrl("http://192.168.10.82:5000/")
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
            }
            else{
                instance = Retrofit.Builder().baseUrl("http://" + IP)

                    //instance = Retrofit.Builder().baseUrl("http://192.168.10.82:5000/")
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
            }


        }
        return instance!!
    }

    fun getInstance(IPAddress:String,ForceInit:Boolean):Retrofit {
        isFake=false;
        if (instance == null || ForceInit) {
            //instance = Retrofit.Builder().baseUrl("http://10.188.30.110/BOS_WMS_API/")
            val gson = GsonBuilder()
                .setLenient()
                .create()
            var IP:String=IPAddress;
            if(!IP.endsWith("/"))
                IP=IP+"/";
                instance = Retrofit.Builder().baseUrl("http://" + IP)
                    //instance = Retrofit.Builder().baseUrl("http://192.168.10.82:5000/")
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
        }
        return instance!!
    }


}