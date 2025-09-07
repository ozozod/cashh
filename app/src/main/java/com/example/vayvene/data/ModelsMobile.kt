package com.example.vayvene.data

// ===== Modelos unificados =====

data class User(
    val id: String,
    val name: String,
    val role: String,
    val eventId: String?
)

data class MeResp(val user: User)

data class Product(
    val id: String,
    val name: String,
    val price: Int // en centavos
)

data class ProductsResp(val products: List<Product>)

data class SimpleResp(val ok: Boolean, val message: String? = null)

data class HeartbeatBody(
    val deviceId: String,
    val batteryLevel: Int? = null,
    val signalStrength: Int? = null
)

data class RegisterCardRequest(
    val cardUid: String,
    val personName: String?,
    val initialBalance: Int?
)

data class GenericOkResponse(val ok: Boolean)

data class SaleItem(val productId: String, val qty: Int)
data class SaleBody(val cardUid: String, val items: List<SaleItem>)
data class SaleResp(val ok: Boolean, val total: Int)

data class BalanceResp(val balance: Int)

data class LoginResponse(val token: String)
