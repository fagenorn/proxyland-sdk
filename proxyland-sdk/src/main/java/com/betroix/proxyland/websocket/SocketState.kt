package com.betroix.proxyland.websocket

enum class SocketState {
    CLOSING,
    CLOSED,
    OPENING,
    OPEN,
    RECONNECT_ATTEMPT,
    RECONNECTING,
    CONNECT_ERROR,
}
