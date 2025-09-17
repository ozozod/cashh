package com.example.vayvene.repo

import com.example.vayvene.network.LoginReq
import com.example.vayvene.network.LoginRes
import com.example.vayvene.network.MobileApi

class AuthRepository(private val api: MobileApi) {
    suspend fun mobileLogin(cardUid: String): LoginRes = api.login(LoginReq(cardUid))
}
