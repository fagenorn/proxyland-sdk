package com.betroix.proxyland.websocket

import com.betroix.proxyland.exceptions.ResponseException
import com.betroix.proxyland.models.json.BaseResponse
import com.beust.klaxon.Klaxon
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

internal class WebSocketListenerImpl(socket: com.betroix.proxyland.websocket.WebSocket) : WebSocketListener() {
    private val socket = socket;

    override fun onOpen(webSocket: WebSocket, response: Response) {
        socket.reconnectionAttempts = 0;
        socket.changeState(SocketState.OPEN)
        socket.postEvent(SocketEvents.OpenStatusEvent(response))
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        socket.postEvent(SocketEvents.BaseMessageEvent(text))

        try {
            val response = Klaxon().parse<BaseResponse>(text)

            when(response?.action) {
                "auth" -> socket.postEvent(SocketEvents.AuthMessageEvent(response))
                "heartbeat" -> socket.postEvent(SocketEvents.HeartbeatEvent(response))
                "https" -> socket.postEvent(SocketEvents.HttpsEvent(response.toTyped()))
                "http" -> socket.postEvent(SocketEvents.HttpEvent(response.toTyped()))
                "status" -> socket.postEvent(SocketEvents.StatusEvent(response.toTyped()))
                else -> throw ResponseException("Unknown action type.")
            }
        } catch (e: Exception) {
            println(e)
            // FIXME: 11/2/2020 Log this probably
        }

    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        // TODO: 11/2/2020 If binary data is transferred
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        socket.changeState(SocketState.CLOSING)
        webSocket.close(1000,reason);
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        socket.changeState(SocketState.CLOSED)
        socket.postEvent(SocketEvents.CloseStatusEvent(code, reason))
    }

    /**
     * This method will be called if:
     * - Fail to verify websocket GET request  => Throwable {@link ProtocolException}
     * - Can't establish websocket connection after upgrade GET request => response null, Throwable {@link Exception}
     * - First GET request had been failed => response null, Throwable {@link java.io.IOException}
     * - Fail to send Ping => response null, Throwable {@link java.io.IOException}
     * - Fail to send data frame => response null, Throwable {@link java.io.IOException}
     * - Fail to read data frame => response null, Throwable {@link java.io.IOException}
     */
    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        if(!socket.isForceTermination) {
            socket.isForceTermination = false;
            socket.changeState(SocketState.CONNECT_ERROR)
            socket.reconnect();
        }

        socket.postEvent(SocketEvents.FailureStatusEvent(t))
    }
}