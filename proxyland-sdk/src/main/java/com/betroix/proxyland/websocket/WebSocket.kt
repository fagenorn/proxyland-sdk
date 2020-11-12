package com.betroix.proxyland.websocket

import android.util.Log
import com.betroix.proxyland.websocket.SocketEvents.CloseStatusEvent
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketFactory
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.math.roundToLong

internal class WebSocket(private val webSocketFactory: WebSocketFactory, private val url: String) :
    IWebSocket {
    companion object {
        private val TAG = "Proxyland WebSocket"
        private const val CLOSE_REASON = "End of session"
        private const val MAX_COLLISION = 7
    }

    private var mState: SocketState
    private var mRealWebSocket: WebSocket? = null

    internal var reconnectionAttempts = 0
    internal var isForceTermination = false

    private val eventBus: PublishSubject<SocketEvents.Event> = PublishSubject.create()

    init {
        mState = SocketState.CLOSED
        isForceTermination = false
    }

    override fun connect() {
        if (mRealWebSocket != null && mState !== SocketState.CLOSED) return

        mRealWebSocket = webSocketFactory.createSocket(url)
        mRealWebSocket!!.addListener(WebSocketListenerImpl(this))
        mRealWebSocket!!.frameQueueSize = 10
        mRealWebSocket!!.connectAsynchronously()
    }

    fun changeState(newState: SocketState) {
        mState = newState
        postEvent(SocketEvents.ChangeStatusEvent(newState))
    }

    fun postEvent(event: SocketEvents.Event) {
        if (!eventBus.hasObservers()) return
        eventBus.onNext(event)
    }

    fun reconnect() {
        if (mState !== SocketState.CLOSED && mState !== SocketState.CONNECT_ERROR) {
            return
        }

        changeState(SocketState.RECONNECT_ATTEMPT)

        if (mRealWebSocket != null) {
            mRealWebSocket!!.disconnect()
            mRealWebSocket = null
        }

        // Calculate delay time: https://en.wikipedia.org/wiki/Exponential_backoff
        val collision =
            if (reconnectionAttempts > MAX_COLLISION) MAX_COLLISION else reconnectionAttempts
        val delayTime = ((2.0.pow(collision.toDouble()) - 1) / 2).roundToLong() * 1000

        postEvent(SocketEvents.ReconnectStatusEvent(reconnectionAttempts + 1, delayTime))

        Observable.timer(delayTime, TimeUnit.MILLISECONDS)
            .doOnError { Log.w(TAG, "AUTO RECONNECT", it) }
            .subscribe({
                changeState(SocketState.RECONNECTING);
                reconnectionAttempts++;
                connect();
            }, {})
    }

    override fun close() {
        close(1000, CLOSE_REASON)
    }

    fun close(code: Int, reason: String) {
        if (mRealWebSocket == null) return
        mRealWebSocket!!.disconnect(code, reason)
    }

    override fun terminate() {
        isForceTermination = true // skip onFailure auto reconnect
        mRealWebSocket!!.disconnect()
        mRealWebSocket = null

        postEvent(CloseStatusEvent(1006, ""))
        eventBus.onComplete()
    }

    override fun send(data: String) {
        if (mState === SocketState.OPEN) {
            mRealWebSocket?.sendText(data)
        }
    }

    override fun send(data: ByteArray) {
        if (mState === SocketState.OPEN) {
            mRealWebSocket?.sendBinary(data)
        }
    }

    override fun <T : SocketEvents.Event> observe(eventClass: Class<T>): Observable<T> {
        return eventBus.filter(eventClass::isInstance)
            .cast(eventClass)
            .subscribeOn(Schedulers.io())
    }
}