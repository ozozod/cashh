package com.example.vayvene.data

import android.content.Context
import retrofit2.Response
import com.example.vayvene.data.ApiClient

class Repository(context: Context) {

    private val api: ApiService = ApiClient.create()  // <- tipo explícito evita “Not enough information to infer T”

    suspend fun loginByCard(cardUid: String): Response<LoginResponse> =
        api.login(LoginRequest(cardUid))

    suspend fun me(bearer: String): Response<MeResponse> =
        api.me(bearer)
}
