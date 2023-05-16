package Remote

import Remote.UserPermissions.UserPermissions
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit


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

                //This is used to add the authorization tokens in the header of every request
                var httpClient = OkHttpClient.Builder().apply {
                    addInterceptor(Interceptor { chain ->
                    val builder = chain.request().newBuilder();
                    builder.addHeader("AuthorizationToken", UserPermissions.AuthToken);
                    return@Interceptor chain.proceed(builder.build())
                })
                }.build();
                instance = Retrofit.Builder().baseUrl("http://" + IP)
                    //instance = Retrofit.Builder().baseUrl("http://192.168.10.82:5000/")
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .client(httpClient)
                    .build()
        }
        return instance!!
    }

    @JvmStatic
    fun getInstanceStatic(IPAddress:String,ForceInit:Boolean):Retrofit {
        isFake=false;
        if (instance == null || ForceInit) {
            //instance = Retrofit.Builder().baseUrl("http://10.188.30.110/BOS_WMS_API/")
            val gson = GsonBuilder()
                .setLenient()
                .create()
            var IP:String=IPAddress;
            if(!IP.endsWith("/"))
                IP=IP+"/";
            //This is used to add the authorization tokens in the header of every request
            var httpClient = OkHttpClient.Builder().apply {
                addInterceptor(Interceptor { chain ->
                    val builder = chain.request().newBuilder();
                    builder.addHeader("AuthorizationToken", UserPermissions.AuthToken);
                    return@Interceptor chain.proceed(builder.build())
                })
            }.build();
            instance = Retrofit.Builder().baseUrl("http://" + IP)
                //instance = Retrofit.Builder().baseUrl("http://192.168.10.82:5000/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addConverterFactory(ScalarsConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(httpClient)
                .build()
        }
        return instance!!
    }

    @JvmStatic
    fun getNewInstanceStatic(IPAddress:String):Retrofit {
        isFake=false
            //instance = Retrofit.Builder().baseUrl("http://10.188.30.110/BOS_WMS_API/")
        val gson = GsonBuilder()
            .setLenient()
            .create()
        var IP:String=IPAddress;
        if(!IP.endsWith("/"))
            IP=IP+"/";
        //This is used to add the authorization tokens in the header of every request
        var httpClient = OkHttpClient.Builder().apply {
            addInterceptor(Interceptor { chain ->
                val builder = chain.request().newBuilder();
                builder.addHeader("AuthorizationToken", UserPermissions.AuthToken);
                return@Interceptor chain.proceed(builder.build())
            })
        }.build();
        var instanceResult = Retrofit.Builder().baseUrl("http://" + IP)
            //instance = Retrofit.Builder().baseUrl("http://192.168.10.82:5000/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addConverterFactory(ScalarsConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(httpClient)
            .build()
        return instanceResult!!
    }

    @JvmStatic
    fun getNewInstanceStatic(IPAddress:String, Timeout:Long):Retrofit {
        isFake=false
        //instance = Retrofit.Builder().baseUrl("http://10.188.30.110/BOS_WMS_API/")
        val gson = GsonBuilder()
                .setLenient()
                .create()
        var IP:String=IPAddress;
        if(!IP.endsWith("/"))
            IP=IP+"/";
        //This is used to add the authorization tokens in the header of every request
        var httpClient = OkHttpClient.Builder().apply {
            addInterceptor(Interceptor { chain ->
                val builder = chain.request().newBuilder();
                builder.addHeader("AuthorizationToken", UserPermissions.AuthToken);
                return@Interceptor chain.proceed(builder.build())
            })
            writeTimeout(Timeout, TimeUnit.SECONDS)
            readTimeout(Timeout, TimeUnit.SECONDS)
        }.build();
        var instanceResult = Retrofit.Builder().baseUrl("http://" + IP)
                //instance = Retrofit.Builder().baseUrl("http://192.168.10.82:5000/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addConverterFactory(ScalarsConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(httpClient)
                .build()
        return instanceResult!!
    }

}