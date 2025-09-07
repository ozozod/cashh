package com.example.vayvene.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AdminApi {
    @POST("api/admin/register-card")
    suspend fun registerCard(@Body body: RegisterCardRequest): Response<GenericOkResponse>
}
