package com.example.vayvene.data

/**
 * Cuerpos y respuestas usados por los endpoints de AdminApi.
 * Si ya tenés estas clases en otro archivo, borrá el duplicado para evitar "Redeclaration".
 */

/**
 * Request para /api/mobile/register-card
 * Ajustá los campos a lo que espere tu backend si difieren.
 */
data class RegisterCardRequest(
    val cardUid: String,          // UID de la tarjeta en hex (ej. "04AABBCCDD...")
    val role: String,             // "ADMIN" | "SELLER" | "BUYER" (según tu backend)
    val fullName: String? = null, // opcional
    val phone: String? = null,    // opcional
    val email: String? = null     // opcional
)

/**
 * Respuesta simple tipo { ok: true, message: "..." }
 * para endpoints que solo confirman la operación.
 */
data class GenericOkResponse(
    val ok: Boolean,
    val message: String? = null
)
