package com.example.vayvene.util

import org.json.JSONObject

/** Solo agrega si value != null (y devuelve el mismo JSONObject para encadenar). */
fun JSONObject.putSafe(key: String, value: Any?): JSONObject {
    if (value != null) this.put(key, value)
    return this
}
