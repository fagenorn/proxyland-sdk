package com.betroix.proxyland.models.json

data class AuthData (
        val secret : String,
        val version : String,
        val remoteId : String,
        val partner : String
)