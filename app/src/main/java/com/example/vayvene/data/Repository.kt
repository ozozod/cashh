package com.example.vayvene.data

import retrofit2.Response

class Repository(private val api: ApiService) {

    private fun <T> Response<T>.toResult(): Result<T> =
        if (isSuccessful && body() != null) Result.success(body()!!)
        else Result.failure(RuntimeException("HTTP ${code()}"))

    suspend fun me(): Result<MeResp> = api.me().toResult()

    suspend fun products(): Result<List<Product>> =
        api.products().toResult().map { it.products }

    suspend fun heartbeat(deviceId: String, battery: Int?, signal: Int?): Result<SimpleResp> =
        api.heartbeat(HeartbeatBody(deviceId, battery, signal)).toResult()
}
