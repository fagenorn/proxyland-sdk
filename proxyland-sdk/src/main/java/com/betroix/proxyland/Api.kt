package com.betroix.proxyland

import android.util.Log
import com.betroix.proxyland.constants.Endpoint
import com.betroix.proxyland.constants.Version
import com.betroix.proxyland.exceptions.ResponseException
import com.betroix.proxyland.http.Http
import com.betroix.proxyland.http.Https
import com.betroix.proxyland.http.HttpsSelector
import com.betroix.proxyland.models.json.*
import com.betroix.proxyland.websocket.IWebSocket
import com.betroix.proxyland.websocket.SocketBuilder
import com.betroix.proxyland.websocket.SocketEvents
import com.betroix.proxyland.websocket.WebSocketEmpty
import com.beust.klaxon.Klaxon
import io.reactivex.rxjava3.core.Maybe
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

internal class Api(private val partnerId: String, private val remoteId: String) : IApi {
    companion object {
        private val TAG = "Proxyland Api"
    }

    override var websocket: IWebSocket = WebSocketEmpty()
    private val client = OkHttpClient()
    private val remoteVersion = Version.REMOTE
    private var secret = ""
    private val selectorLoop = HttpsSelector(this)

    private fun getServerInfo(): WebsocketServerResponse {
        val request = Request.Builder()
            .url(Endpoint.WS_INFO)
            .build()

        client.newCall(request).execute().use {
            val json = it.body?.string()
                ?: throw ResponseException("Invalid body when retrieving websocket server info.")

            return Klaxon().parse<WebsocketServerResponse>(json)
                ?: throw ResponseException("Invalid body when retrieving websocket server info.")
        }
    }

    override fun <T> sendToSocket(response: BaseResponse, data: T) {
        val s = Klaxon().toJsonString(
            BaseRequest(
                response.id,
                response.action,
                response.version,
                remoteId,
                remoteVersion,
                data
            )
        )
        websocket.send(s)
    }

    override fun <T, P> sendToSocket(response: TypedBaseResponse<P>, data: T) {
        val s = Klaxon().toJsonString(
            BaseRequest(
                response.id,
                response.action,
                response.version,
                remoteId,
                remoteVersion,
                data
            )
        )
        websocket.send(s)
    }

    override fun createSocket() {
        // Get websocket endpoint and secret from server.
        // val serverInfo = getServerInfo()
        secret = "aaf2289f-ef5c-4c1f-ba07-f6b860c8dc69" ; // serverInfo.secret
        // this.websocket = SocketBuilder(serverInfo.urls[0]).build()
        this.websocket = SocketBuilder("ws://54.165.176.195:4343").build()
    }

    override fun startSocket(timeout: Long): Maybe<SocketEvents.StatusEvent> {
//        websocket.observe(SocketEvents.BaseMessageEvent::class.java)
//                .subscribe {
//                    println("MESSAGE: ${it.message}")
//                }

        // Monitor any CONNECT requests to create TCP tunnel.
        websocket.observe(SocketEvents.HttpsEvent::class.java)
            .filter { it.response.data.method.equals("connect", true) }
            .doOnError { Log.e(TAG, "HTTPS CONNECT REQUEST", it) }
            .subscribe({ selectorLoop.register(Https(this, it.response)) }, {})

        // Remote/Server heartbeat.
        websocket.observe(SocketEvents.HeartbeatEvent::class.java)
            .doOnError { Log.e(TAG, "HEARTBEAT", it) }
            .subscribe({ sendToSocket(it.response, "pong") }, {})

        // Authenticate remote.
        websocket.observe(SocketEvents.AuthMessageEvent::class.java)
            .doOnError { Log.e(TAG, "AUTH REQUEST", it) }
            .subscribe({
                val data = AuthData(secret, it.response.version, remoteId, partnerId);
                sendToSocket(it.response, data)
            }, {})

        // Simple HTTP request.
        websocket.observe(SocketEvents.HttpEvent::class.java)
            .filter { it.response.data.body?.method != null }
            .doOnError { Log.e(TAG, "HTTP REQUEST", it) }
            .subscribe({ Http(this, it.response).request() }, {})

        // Wait for authenticate success status before socket is ready for use.
        val authObs = websocket.observe(SocketEvents.StatusEvent::class.java)
            .filter { it.response.data.authenticate }
            .firstElement()
            .timeout(timeout, TimeUnit.MILLISECONDS)
            .doOnSubscribe { websocket.connect() }

        return authObs ?: throw IllegalArgumentException("Unable to start socket")
    }
}