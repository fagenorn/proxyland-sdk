package com.betroix.proxyland.models.json

import com.beust.klaxon.TypeAdapter
import kotlin.reflect.KClass

class DataTypeAdapter : TypeAdapter<Any> {
    override fun classFor(type: Any): KClass<out Any> = when (type as String) {
        "auth" -> Any::class
        "heartbeat" -> Any::class
        "https" -> HttpsData::class
        "http" -> HttpData::class
        "status" -> StatusData::class
        else -> throw IllegalArgumentException("Unknown action response type: $type")
    }
}