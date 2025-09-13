package com.example.vayvene.data

import com.example.vayvene.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    @Volatile
    private var jwtToken: String? = null

    /** Guardar / limpiar token en memoria (para el header Authorization) */
    fun setToken(token: String?) { jwtToken = token }

    fun token(): String? = jwtToken

    private val logging by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }
    }

    /** Cliente OkHttp que agrega Authorization si hay token */
    val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder().apply {
                    jwtToken?.let { addHeader("Authorization", "Bearer $it") }
                }.build()
                chain.proceed(req)
            }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /** Retrofit listo por si luego querés usar interfaces */
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL) // ej: http://192.168.1.28:3000
            .client(http)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }
}
