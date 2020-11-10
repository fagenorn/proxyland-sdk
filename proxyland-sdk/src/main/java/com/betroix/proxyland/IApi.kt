package com.betroix.proxyland

import com.betroix.proxyland.models.json.BaseResponse
import com.betroix.proxyland.models.json.TypedBaseResponse
import com.betroix.proxyland.websocket.IWebSocket
import com.betroix.proxyland.websocket.SocketEvents
import io.reactivex.rxjava3.core.Maybe

internal interface IApi {

    /**
     * {@code timeout} is specified in milliseconds
     */
    fun startSocket(timeout: Long = 2500): Maybe<SocketEvents.StatusEvent>

    fun createSocket()

    fun <T> sendToSocket(response: BaseResponse, data: T)

    fun <T, P> sendToSocket(response: TypedBaseResponse<P>, data: T)

    val websocket: IWebSocket
}