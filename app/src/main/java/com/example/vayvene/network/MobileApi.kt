package com.example.vayvene.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class LoginReq(val cardUid: String)
data class UserDTO(val id: Long, val name: String?, val role: String, val eventId: Long?)
data class EventDTO(val id: Long, val name: String?)
data class LoginRes(val token: String, val user: UserDTO, val event: EventDTO?)

interface MobileApi {
    @POST("api/mobile/login")
    suspend fun login(@Body body: LoginReq): LoginRes

    @GET("api/mobile/me")
    suspend fun me(): Map<String, Any>

    @POST("api/mobile/heartbeat")
    suspend fun heartbeat(@Body b: Map<String, Any?>): Map<String, Any>

    @GET("api/mobile/products")
    suspend fun products(@Query("eventId") eventId: Long?): Any
}
