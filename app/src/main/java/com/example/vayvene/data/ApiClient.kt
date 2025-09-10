package com.example.vayvene.data

import com.example.vayvene.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    @Volatile
    private var jwtToken: String? = null

    fun setToken(token: String?) {
        jwtToken = token
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
        jwtToken?.let { builder.addHeader("Authorization", "Bearer $it") }
        chain.proceed(builder.build())
    }

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @PublishedApi
    internal val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL) // http://192.168.1.28:3000 en tu flavor dev
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    /** Forma 1: sin argumentos (usá tipo explícito o genérico reified) */
    inline fun <reified T> create(): T = retrofit.create(T::class.java)

    /** Forma 2: con argumento de clase (compat con llamadas existentes) */
    fun <T> create(service: Class<T>): T = retrofit.create(service)
}
