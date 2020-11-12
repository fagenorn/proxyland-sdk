package com.betroix.proxyland.websocket

import android.util.Log
import com.betroix.proxyland.exceptions.ResponseException
import com.betroix.proxyland.models.protobuf.Model
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

internal class WebSocketListenerImpl(socket: com.betroix.proxyland.websocket.WebSocket) : WebSocketListener() {
    companion object {
        private val TAG = "Proxyland Web Socket Listener"
    }

    private val socket = socket;

    override fun onOpen(webSocket: WebSocket, response: Response) {
        socket.reconnectionAttempts = 0;
        socket.changeState(SocketState.OPEN)
        socket.postEvent(SocketEvents.OpenStatusEvent(response))
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        socket.postEvent(SocketEvents.BaseMessageEvent(text))
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        socket.postEvent(SocketEvents.BaseMessageEvent(bytes.toByteArray().joinToString(",", "[", "]") { it.toInt().toString() }))

        try {
            val response = Model.ServerMessage.parseFrom(bytes.toByteArray())


            when(response.action) {
                Model.Action.AUTH -> socket.postEvent(SocketEvents.AuthMessageEvent(response))
                Model.Action.HEARTBEAT -> socket.postEvent(SocketEvents.HeartbeatEvent(response))
                Model.Action.HTTPS -> socket.postEvent(SocketEvents.HttpsEvent(response))
                Model.Action.HTTP -> socket.postEvent(SocketEvents.HttpEvent(response))
                Model.Action.STATUS -> socket.postEvent(SocketEvents.StatusEvent(response))
                else -> throw ResponseException("Unknown action type.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse server message", e)
        }
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
            socket.isForceTermination = false
            socket.changeState(SocketState.CONNECT_ERROR)
            socket.reconnect()
        }

        socket.postEvent(SocketEvents.FailureStatusEvent(t))
    }
}