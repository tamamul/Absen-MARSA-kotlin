package com.marsa.absen.data.remote

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/** Nilai field "status" di respons server (null jika tidak ada). */
fun JsonElement.serverStatus(): Boolean? =
    ((this as? JsonObject)?.get("status") as? JsonPrimitive)?.booleanOrNull

/** Nilai field "message" di respons server (null jika tidak ada). */
fun JsonElement.serverMessage(): String? =
    ((this as? JsonObject)?.get("message") as? JsonPrimitive)?.contentOrNull
