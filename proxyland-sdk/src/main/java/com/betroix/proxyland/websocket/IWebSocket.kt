package com.betroix.proxyland.websocket

import io.reactivex.rxjava3.core.Observable
import okio.ByteString

interface IWebSocket {
    fun connect()
    fun close()
    fun terminate()
    fun send(data: String) : Boolean;
    fun send(data: ByteArray) : Boolean;
    fun <T : SocketEvents.Event> observe(eventClass: Class<T>) : Observable<T>
}

class WebSocketEmpty : IWebSocket {
    override fun connect() {
        throw NotImplementedError()
    }

    override fun close() {
        throw NotImplementedError()
    }

    override fun terminate() {
        throw NotImplementedError()
    }

    override fun send(data: String): Boolean {
        throw NotImplementedError()
    }

    override fun send(data: ByteArray): Boolean {
        throw NotImplementedError()
    }

    override fun <T : SocketEvents.Event> observe(eventClass: Class<T>): Observable<T> {
        throw NotImplementedError()
    }
}