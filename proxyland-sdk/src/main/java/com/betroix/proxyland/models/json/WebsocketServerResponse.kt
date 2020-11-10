package com.betroix.proxyland.models.json

data class WebsocketServerResponse (
        val url : String,
        val urls : List<String>,
        val secret : String,
        val tls : Boolean
)