package com.marsa.absen.data.remote

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlin.math.roundToInt

/** Nilai field "status" di respons server (null jika tidak ada). */
fun JsonElement.serverStatus(): Boolean? = (this as? JsonObject)?.bool("status")

/** Nilai field "message" di respons server (null jika tidak ada). */
fun JsonElement.serverMessage(): String? = (this as? JsonObject)?.str("message")

fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

fun JsonObject.bool(key: String): Boolean? {
    val p = this[key] as? JsonPrimitive ?: return null
    p.booleanOrNull?.let { return it }
    return when (p.contentOrNull?.lowercase()) {
        "1", "true", "ya" -> true
        "0", "false", "tidak" -> false
        else -> null
    }
}

fun JsonObject.int(key: String): Int? {
    val p = this[key] as? JsonPrimitive ?: return null
    return p.intOrNull ?: p.doubleOrNull?.roundToInt()
}
