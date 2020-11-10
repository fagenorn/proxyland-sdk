package com.betroix.proxyland.models.json

import com.beust.klaxon.TypeFor

@Suppress("UNCHECKED_CAST")
data class BaseResponse (
        val id: String,
        @TypeFor(field = "data", adapter = DataTypeAdapter::class) val action: String,
        val version: String,
        val data: Any? = null
) {
    fun <T> toTyped() : TypedBaseResponse<T> { return TypedBaseResponse(id, action, version, data as T)
    }
}