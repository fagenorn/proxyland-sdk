package com.betroix.proxyland

import com.betroix.proxyland.models.json.BaseResponse
import com.betroix.proxyland.models.json.TypedBaseResponse
import com.betroix.proxyland.models.protobuf.Model
import com.betroix.proxyland.websocket.IWebSocket
import com.betroix.proxyland.websocket.SocketEvents
import io.reactivex.rxjava3.core.Maybe

internal interface IApi {

    /**
     * {@code timeout} is specified in milliseconds
     */
    fun startSocket(timeout: Long = 5000): Maybe<SocketEvents.StatusEvent>

    fun createSocket()

    fun sendToSocket(response: Model.ServerMessage, request: Model.RemoteMessage.Builder)

    val websocket: IWebSocket
}