package com.example.vayvene.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Solo la interfaz (los modelos están en ModelsMobile.kt)
interface ApiService {
    @POST("api/mobile/login")
    suspend fun login(@Body body: Map<String, String>): Response<LoginResponse>

    @GET("api/mobile/me")
    suspend fun me(): Response<MeResp>

    @GET("api/mobile/products")
    suspend fun products(): Response<ProductsResp>

    @POST("api/mobile/heartbeat")
    suspend fun heartbeat(@Body body: HeartbeatBody): Response<SimpleResp>
}
