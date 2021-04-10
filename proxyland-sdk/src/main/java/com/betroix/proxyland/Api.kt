package com.betroix.proxyland

import android.util.Log
import androidx.lifecycle.Observer
import com.betroix.proxyland.constants.Endpoint
import com.betroix.proxyland.constants.Version
import com.betroix.proxyland.exceptions.ResponseException
import com.betroix.proxyland.http.Http
import com.betroix.proxyland.http.Https
import com.betroix.proxyland.http.HttpsSelector
import com.betroix.proxyland.models.protobuf.Model
import com.betroix.proxyland.websocket.IWebSocket
import com.betroix.proxyland.websocket.SocketBuilder
import com.betroix.proxyland.websocket.SocketEvents
import com.betroix.proxyland.websocket.WebSocketEmpty
import io.reactivex.rxjava3.core.Maybe
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

internal class Api(private val partnerId: String, private val apiKey: String, private val remoteId: String) : IApi {
    companion object {
        private val TAG = "Proxyland Api"
    }

    override var websocket: IWebSocket = WebSocketEmpty()
    private val client = OkHttpClient()
    private val remoteVersion = Version.REMOTE
    private val selectorLoop = HttpsSelector(this)

    private fun getWsUrl(): String {
        val request = Request.Builder()
            .url(Endpoint.WS_INFO)
            .build()

        client.newCall(request).execute().use {
            val body = it.body?.string()
                ?: throw ResponseException("Invalid body when retrieving websocket server info.")

            return JSONObject(body).getString("url")
        }
    }

    override fun sendToSocket(response: Model.ServerMessage, request: Model.RemoteMessage.Builder) {
        request.id = response.id
        request.action = response.action
        request.version = response.version

        val message = request.build().toByteArray()
//        Log.d(TAG, "(S) Message - ${message.joinToString(",", "[", "]") { it.toInt().toString() }}")

        websocket.send(message)
    }

    override fun createSocket() {
//         ws://mars-protobuf.proxyland.io:4343
         this.websocket = SocketBuilder(getWsUrl()).build()
//        this.websocket = SocketBuilder("ws://192.168.1.72:4343").build()
    }

    override fun startSocket(timeout: Long): Maybe<SocketEvents.StatusEvent> {
//        websocket.observe(SocketEvents.BaseMessageEvent::class.java)
//            .subscribe { Log.i(TAG, "(R) Message - ${it.message}") }

        websocket.observe(SocketEvents.FailureStatusEvent::class.java)
            .subscribe { Log.e(TAG, "Web Socket Failure", it.throwable) }

        websocket.observe(SocketEvents.CloseStatusEvent::class.java)
            .subscribe { Log.e(TAG, "Web Socket Close - ${it.code}:${it.reason}") }

        websocket.observe(SocketEvents.ChangeStatusEvent::class.java)
            .subscribe { Log.d(TAG, "Web Socket Status - ${it.status}") }

        // Monitor any CONNECT requests to create TCP tunnel.
        websocket.observe(SocketEvents.HttpsEvent::class.java)
            .filter { it.response.https.method.equals("connect", true) }
            .doOnNext { Log.d(TAG, "HTTPS CONNECT message received") }
            .doOnError { Log.e(TAG, "HTTPS CONNECT REQUEST", it) }
            .subscribe({ selectorLoop.register(Https(this, it.response)) }, {})

        // Remote/Server heartbeat.
        websocket.observe(SocketEvents.HeartbeatEvent::class.java)
            .doOnError { Log.e(TAG, "HEARTBEAT", it) }
            .subscribe({ sendToSocket(it.response, Model.RemoteMessage.newBuilder()) }, {})

        // Authenticate remote.
        websocket.observe(SocketEvents.AuthMessageEvent::class.java)
            .doOnNext { Log.d(TAG, "Auth message received") }
            .doOnError { Log.e(TAG, "AUTH REQUEST", it) }
            .subscribe({
                val data =
                    Model.AuthMessage
                        .newBuilder()
                        .setSecret(apiKey)
                        .setRemoteVersion(remoteVersion)
                        .setRemoteId(remoteId)
                        .setPartnerId(partnerId)

                sendToSocket(it.response, Model.RemoteMessage.newBuilder().setAuth(data))
            }, {})

        // Simple HTTP request.
        websocket.observe(SocketEvents.HttpEvent::class.java)
            .filter { !it.response.http.url.isNullOrBlank() }
            .doOnNext { Log.d(TAG, "HTTP message received") }
            .doOnError { Log.e(TAG, "HTTP REQUEST", it) }
            .subscribe({ Http(this, it.response).request() }, {})

        // Wait for authenticate success status before socket is ready for use.
        val authObs = websocket.observe(SocketEvents.StatusEvent::class.java)
            .doOnNext { Log.d(TAG, "Status message received") }
            .filter { it.response.status.authenticated }
            .firstElement()
            .timeout(timeout, TimeUnit.MILLISECONDS)
            .doOnSubscribe { websocket.connect() }

        return authObs ?: throw IllegalArgumentException("Unable to start socket")
    }

    override fun stopSocket() {
        Log.d(TAG, "Terminating")

        websocket.terminate()
        selectorLoop.terminate()
    }
}