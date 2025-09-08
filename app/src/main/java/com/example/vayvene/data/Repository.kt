package com.example.vayvene.data

import android.content.Context

class Repository(
    private val api: ApiService,
    context: Context
) {
    private val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)

    private fun saveToken(token: String) {
        prefs.edit().putString("jwt", token).apply()
    }

    fun getToken(): String? = prefs.getString("jwt", null)

    fun clearToken() {
        prefs.edit().remove("jwt").apply()
    }

    /** ==== LOGIN ==== */
    suspend fun loginWithCard(cardUid: String): Result<LoginResponse> {
        return try {
            val resp = api.loginWithCard(mapOf("cardUid" to cardUid))
            if (resp.isSuccessful) {
                val body = resp.body()
                if (body != null) {
                    saveToken(body.token)
                    Result.success(body)
                } else {
                    Result.failure(IllegalStateException("Respuesta vacía del servidor"))
                }
            } else {
                Result.failure(IllegalStateException("HTTP ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** ==== YO ==== */
    suspend fun me(): Result<MeResp> {
        val token = getToken() ?: return Result.failure(IllegalStateException("Sin token"))
        return try {
            val resp = api.me("Bearer $token")
            if (resp.isSuccessful) {
                Result.success(resp.body() ?: MeResp(ok = true))
            } else {
                Result.failure(IllegalStateException("HTTP ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** ==== VENDEDOR: BALANCE ==== */
    suspend fun balance(): Result<BalanceResp> {
        val token = getToken() ?: return Result.failure(IllegalStateException("Sin token"))
        return try {
            val resp = api.balance("Bearer $token")
            if (resp.isSuccessful) {
                Result.success(resp.body() ?: BalanceResp(null))
            } else {
                Result.failure(IllegalStateException("HTTP ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** ==== VENDEDOR: VENTA ==== */
    suspend fun sale(body: SaleBody): Result<SaleResp> {
        val token = getToken() ?: return Result.failure(IllegalStateException("Sin token"))
        return try {
            val resp = api.sale("Bearer $token", body)
            if (resp.isSuccessful) {
                Result.success(resp.body() ?: SaleResp(false, null))
            } else {
                Result.failure(IllegalStateException("HTTP ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
