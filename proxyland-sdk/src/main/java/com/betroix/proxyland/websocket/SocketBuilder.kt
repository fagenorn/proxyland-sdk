package com.betroix.proxyland.websocket

import okhttp3.OkHttpClient
import okhttp3.Request

internal class SocketBuilder {
    private val request: Request.Builder
    private val httpClient: OkHttpClient.Builder

    constructor(url: String) {
        require(!(!url.regionMatches(0, "ws:", 0, 3, ignoreCase = true) && !url.regionMatches(0, "wss:", 0, 4, ignoreCase = true))) { "web socket url must start with ws or wss, passed url is $url" }

        this.request = Request.Builder().url(url)
        this.httpClient= OkHttpClient.Builder()
    }

    fun build(): IWebSocket {
        return WebSocket(httpClient, request.build())
    }
}