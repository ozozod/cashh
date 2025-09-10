package com.example.vayvene.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class LoginRequest(val cardUid: String)

data class EventDto(
    val id: String?,
    val name: String?
)

data class UserDto(
    val id: String?,
    val name: String?,
    val role: String?,
    val eventId: String?
)

data class LoginResponse(
    val token: String,
    val user: UserDto?,
    val event: EventDto?
)

data class MeResponse(
    val user: UserDto
)

interface ApiService {
    @POST("login") suspend fun login(@Body body: LoginRequest): Response<LoginResponse>
    @GET("me") suspend fun me(@Header("Authorization") bearer: String): Response<MeResponse>
}
