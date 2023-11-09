package Remote

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory


object APIClient {
    private var instance: Retrofit?=null
    fun getInstance(IPAddress:String,ForceInit:Boolean):Retrofit {
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

    @JvmStatic
    fun getInstanceStatic(IPAddress:String,ForceInit:Boolean):Retrofit {
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

    @JvmStatic
    fun getNewInstanceStatic(IPAddress:String):Retrofit {
        //instance = Retrofit.Builder().baseUrl("http://10.188.30.110/BOS_WMS_API/")
        val gson = GsonBuilder()
            .setLenient()
            .create()
        var IP:String=IPAddress;
        if(!IP.endsWith("/"))
            IP=IP+"/";
        var instanceResult = Retrofit.Builder().baseUrl("http://" + IP)
            //instance = Retrofit.Builder().baseUrl("http://192.168.10.82:5000/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addConverterFactory(ScalarsConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
        return instanceResult!!
    }

}


/*
package Remote

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory


object APIClient {
    private var instance: Retrofit?=null
    fun getInstance(IPAddress:String,ForceInit:Boolean):Retrofit {
        if (instance == null || ForceInit) {
            //instance = Retrofit.Builder().baseUrl("http://10.188.30.110/BOS_WMS_API/")
            val gson = GsonBuilder()
                .setLenient()
                .create()
            instance = Retrofit.Builder().baseUrl("http://" + IPAddress)

                //instance = Retrofit.Builder().baseUrl("http://192.168.10.82:5000/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addConverterFactory(ScalarsConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()

        }
        return instance!!
    }
}*/
