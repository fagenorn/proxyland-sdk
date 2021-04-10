package com.betroix.proxyland.websocket

import android.util.Log
import com.betroix.proxyland.exceptions.ResponseException
import com.betroix.proxyland.models.protobuf.Model
import com.neovisionaries.ws.client.ThreadType
import com.neovisionaries.ws.client.WebSocketException
import com.neovisionaries.ws.client.WebSocketFrame
import com.neovisionaries.ws.client.WebSocketState

internal class WebSocketListenerImpl(private val socket: com.betroix.proxyland.websocket.WebSocket) :
    com.neovisionaries.ws.client.WebSocketListener {
    companion object {
        private val TAG = "Proxyland Web Socket Listener"
    }

    override fun onStateChanged(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        newState: WebSocketState?
    ) {
        when (newState) {
            WebSocketState.CREATED -> socket.changeState(SocketState.CLOSED)
            WebSocketState.CONNECTING -> socket.changeState(SocketState.OPENING)
            WebSocketState.OPEN -> socket.changeState(SocketState.OPEN)
            WebSocketState.CLOSING -> socket.changeState(SocketState.CLOSING)
            WebSocketState.CLOSED -> socket.changeState(SocketState.CLOSED)
            null -> throw ResponseException("Unknown web socket state.")
        }
    }

    override fun onConnected(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        headers: MutableMap<String, MutableList<String>>?
    ) {
        socket.postEvent(SocketEvents.OpenStatusEvent(headers))
    }

    override fun onConnectError(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        cause: WebSocketException?
    ) {
        socket.changeState(SocketState.CONNECT_ERROR)

        if (!socket.isForceTermination) {
            socket.reconnect()
        }
    }

    override fun onDisconnected(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        serverCloseFrame: WebSocketFrame?,
        clientCloseFrame: WebSocketFrame?,
        closedByServer: Boolean
    ) {
        if (closedByServer) {
            socket.postEvent(
                SocketEvents.CloseStatusEvent(
                    serverCloseFrame!!.closeCode,
                    serverCloseFrame.closeReason,
                )
            )
        }

        if (!socket.isForceTermination) {
            socket.reconnect()
        }
    }

    override fun onFrame(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        frame: WebSocketFrame?
    ) {
    }

    override fun onContinuationFrame(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        frame: WebSocketFrame?
    ) {
    }

    override fun onTextFrame(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        frame: WebSocketFrame?
    ) {
    }

    override fun onBinaryFrame(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        frame: WebSocketFrame?
    ) {
    }

    override fun onCloseFrame(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        frame: WebSocketFrame?
    ) {
    }

    override fun onPingFrame(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        frame: WebSocketFrame?
    ) {
    }

    override fun onPongFrame(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        frame: WebSocketFrame?
    ) {
    }

    override fun onTextMessage(websocket: com.neovisionaries.ws.client.WebSocket?, text: String?) {
        socket.postEvent(SocketEvents.BaseMessageEvent(text!!))
    }

    override fun onTextMessage(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        data: ByteArray?
    ) {
    }

    override fun onBinaryMessage(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        binary: ByteArray?
    ) {
//        socket.postEvent(
//            SocketEvents.BaseMessageEvent(
//                binary!!.joinToString(",", "[", "]") { it.toInt().toString() })
//        )

        try {
            val response = Model.ServerMessage.parseFrom(binary)

            when (response.action) {
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

    override fun onSendingFrame(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        frame: WebSocketFrame?
    ) {
    }

    override fun onFrameSent(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        frame: WebSocketFrame?
    ) {
    }

    override fun onFrameUnsent(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        frame: WebSocketFrame?
    ) {
    }

    override fun onThreadCreated(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        threadType: ThreadType?,
        thread: Thread?
    ) {
    }

    override fun onThreadStarted(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        threadType: ThreadType?,
        thread: Thread?
    ) {
    }

    override fun onThreadStopping(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        threadType: ThreadType?,
        thread: Thread?
    ) {
    }

    override fun onError(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        cause: WebSocketException?
    ) {
        socket.postEvent(SocketEvents.FailureStatusEvent(cause))
    }

    override fun onFrameError(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        cause: WebSocketException?,
        frame: WebSocketFrame?
    ) {
    }

    override fun onMessageError(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        cause: WebSocketException?,
        frames: MutableList<WebSocketFrame>?
    ) {
    }

    override fun onMessageDecompressionError(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        cause: WebSocketException?,
        compressed: ByteArray?
    ) {
    }

    override fun onTextMessageError(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        cause: WebSocketException?,
        data: ByteArray?
    ) {
    }

    override fun onSendError(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        cause: WebSocketException?,
        frame: WebSocketFrame?
    ) {
    }

    override fun onUnexpectedError(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        cause: WebSocketException?
    ) {
    }

    override fun handleCallbackError(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        cause: Throwable?
    ) {
        socket.postEvent(SocketEvents.FailureStatusEvent(cause))
    }

    override fun onSendingHandshake(
        websocket: com.neovisionaries.ws.client.WebSocket?,
        requestLine: String?,
        headers: MutableList<Array<String>>?
    ) {
    }
}