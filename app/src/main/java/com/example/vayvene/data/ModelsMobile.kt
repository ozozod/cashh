package com.example.vayvene.data

// Centralizá TODOS los modelos acá. No los declares de nuevo en ApiService.kt.

data class LoginResponse(
    val token: String,
    val role: String? = null,
    val name: String? = null
)

data class MeResp(
    val ok: Boolean = true
)

data class BalanceResp(
    val balance: Double? = null
)

data class SaleItem(
    val productId: String,
    val qty: Int
)

data class SaleBody(
    val items: List<SaleItem>
)

data class SaleResp(
    val ok: Boolean = false,
    val id: String? = null
)
