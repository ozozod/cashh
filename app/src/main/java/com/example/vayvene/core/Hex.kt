package com.example.vayvene.core

object Hex {
    fun toHex(bytes: ByteArray?): String {
        if (bytes == null) return ""
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }
}