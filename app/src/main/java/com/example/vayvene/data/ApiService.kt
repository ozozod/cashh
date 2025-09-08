package com.example.vayvene.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// ¡OJO! Acá solo la interfaz. Nada de data classes.
interface ApiService {

    // Login por tarjeta NFC (se envía como Map, clave "cardUid")
    @POST("auth/login-card")
    suspend fun loginWithCard(@Body body: Map<String, String>): Response<LoginResponse>

    @GET("auth/me")
    suspend fun me(@Header("Authorization") bearer: String): Response<MeResp>

    @GET("seller/balance")
    suspend fun balance(@Header("Authorization") bearer: String): Response<BalanceResp>

    @POST("seller/sale")
    suspend fun sale(
        @Header("Authorization") bearer: String,
        @Body body: SaleBody
    ): Response<SaleResp>
}
