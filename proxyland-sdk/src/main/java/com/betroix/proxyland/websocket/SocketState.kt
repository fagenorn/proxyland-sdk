package com.betroix.proxyland.websocket

enum class SocketState {
    CLOSING,
    CLOSED,
    OPENING,
    OPEN,
    CONNECT_ERROR,
    RECONNECT_ATTEMPT,
    RECONNECTING,
}
