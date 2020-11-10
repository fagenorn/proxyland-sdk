package com.betroix.proxyland.websocket

import android.util.Log
import com.betroix.proxyland.Api
import com.betroix.proxyland.websocket.SocketEvents.CloseStatusEvent
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.ws.RealWebSocket
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.math.roundToLong

internal class WebSocket(httpClient: OkHttpClient.Builder, request: Request) : IWebSocket {
    companion object {
        private val TAG = "Proxyland WebSocket"
        private  const val CLOSE_REASON = "End of session"
        private const val MAX_COLLISION = 7
    }

    private var mState: SocketState
    private val mRequest: Request = request
    private val mHttpClient: OkHttpClient.Builder = httpClient
    private var mRealWebSocket: RealWebSocket? = null

    internal var reconnectionAttempts = 0
    internal var isForceTermination = false

    private val eventBus: PublishSubject<SocketEvents.Event> = PublishSubject.create()

    init {
        mState = SocketState.CLOSED
        isForceTermination = false
    }

    override fun connect() {
        if (mRealWebSocket != null && mState !== SocketState.CLOSED) return

        changeState(SocketState.OPENING)
        mRealWebSocket = mHttpClient.build().newWebSocket(mRequest, WebSocketListenerImpl(this)) as RealWebSocket
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
        if (mState !== SocketState.CONNECT_ERROR) {
            return
        }

        changeState(SocketState.RECONNECT_ATTEMPT)

        if (mRealWebSocket != null) {
            mRealWebSocket!!.cancel()
            mRealWebSocket = null
        }

        // Calculate delay time: https://en.wikipedia.org/wiki/Exponential_backoff
        val collision = if (reconnectionAttempts > MAX_COLLISION) MAX_COLLISION else reconnectionAttempts
        val delayTime = ((2.0.pow(collision.toDouble()) - 1) / 2).roundToLong() * 1000

        postEvent(SocketEvents.ReconnectStatusEvent(reconnectionAttempts + 1, delayTime))

        Observable.timer(delayTime, TimeUnit.MILLISECONDS)
            .doOnError { Log.w(TAG, "AUTO RECONNECT", it)}
            .subscribe ({
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
        mRealWebSocket!!.close(code, reason)
    }

    override fun terminate() {
        isForceTermination = true // skip onFailure auto reconnect
        mRealWebSocket?.cancel()
        mRealWebSocket = null

        changeState(SocketState.CLOSED)
        postEvent(CloseStatusEvent(1006, ""))
        eventBus.onComplete()
    }

    override fun send(data: String): Boolean {
        if (mState === SocketState.OPEN) {
            return mRealWebSocket?.send(data) ?: false
        }

        return false
    }

    override fun send(data: ByteArray): Boolean {
        if (mState === SocketState.OPEN) {
            return mRealWebSocket?.send(data.toByteString()) ?: false
        }

        return false    }

    override fun <T : SocketEvents.Event> observe(eventClass: Class<T>) : Observable<T> {
        return eventBus.filter(eventClass::isInstance)
                .cast(eventClass)
                .subscribeOn(Schedulers.io())
    }
}