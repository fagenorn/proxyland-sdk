package com.betroix.proxyland.websocket

import com.neovisionaries.ws.client.WebSocketFactory
import okhttp3.OkHttpClient
import okhttp3.Request

internal class SocketBuilder(private val url: String) {
    private val factory: WebSocketFactory

    init {
        require(!(!url.regionMatches(0, "ws:", 0, 3, ignoreCase = true) && !url.regionMatches(0, "wss:", 0, 4, ignoreCase = true))) { "web socket url must start with ws or wss, passed url is $url" }
        this.factory= WebSocketFactory()
    }

    fun build(): IWebSocket {
        return WebSocket(factory, url)
    }
}