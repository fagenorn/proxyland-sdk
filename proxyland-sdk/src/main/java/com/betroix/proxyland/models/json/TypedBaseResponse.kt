package com.betroix.proxyland.models.json

data class TypedBaseResponse<T> (
        val id: String,
        val action: String,
        val version: String,
        val data: T
)

