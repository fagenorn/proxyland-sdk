package com.betroix.proxyland.models.json

data class BaseRequest<T> (
    val id: String,
    val action: String,
    val version: String,
    val remoteId: String,
    val remoteVersion: String,
    val data: T
)