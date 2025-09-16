package com.example.vayvene.data

import com.example.vayvene.BuildConfig

// Compat: algunas clases antiguas esperan API_BASE o apiBaseUrl
val API_BASE: String get() = BuildConfig.BASE_URL.trimEnd('/')
val apiBaseUrl: String get() = API_BASE

// Si más adelante necesitás lógica real del repositorio, podés extender este object.
object Repository
