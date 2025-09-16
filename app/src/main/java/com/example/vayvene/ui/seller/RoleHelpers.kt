package com.example.vayvene.ui.seller

import java.util.Locale

fun isManagerOrAdmin(role: String?): Boolean =
    when (role?.trim()?.uppercase(Locale.ROOT)) {
        "ADMINISTRADOR", "ENCARGADO", "ADMIN", "MANAGER" -> true
        else -> false
    }
