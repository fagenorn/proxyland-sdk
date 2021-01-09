package com.betroix.proxyland.http

import android.util.Log
import com.betroix.proxyland.IApi
import com.betroix.proxyland.models.protobuf.Model
import com.betroix.proxyland.websocket.SocketEvents
import com.google.protobuf.ByteString
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.concurrent.TimeUnit

internal class Https(private val api: IApi, val message: Model.ServerMessage) {
    companion object {
        private val TAG = "Proxyland Https Request"
    }

    var writerDispose: Disposable? = null

    fun connect(): Single<SocketChannel> {
        // Monitor socket till connection is established
        return Observable.just(createSocket())
            .delay(50, TimeUnit.MILLISECONDS)
            .repeat()
            .filter { it.finishConnect() }
            .firstOrError()
            .doOnError {
                Log.w(
                    TAG,
                    "HTTPS CONNECT (${message.https.host}:${message.https.port})",
                    it
                )
            }
            .doOnSuccess { socket ->
                // Tunnel websocket client data to socket input.
                writerDispose = api.websocket.observe(SocketEvents.HttpsEvent::class.java)
                    .filter { it.response.id == message.id }
                    .doOnError { Log.e(TAG, "HTTPS WRITE", it) }
                    .subscribe({
                        val buf: ByteBuffer =
                            ByteBuffer.wrap(it.response.https.data.toByteArray())
                        socket.write(buf)
                    }, {})

                // Send success response to Server
                api.sendToSocket(
                    message,
                    Model.RemoteMessage.newBuilder().setHttps(
                        Model.HttpsRemoteMessage.newBuilder()
                            .setData(ByteString.copyFrom("HTTP/1.1 200 Connection established\r\n\r\n".toByteArray()))
                    )
                )
            }
    }

    private fun createSocket(): SocketChannel {
        val socket = SocketChannel.open()
        socket.configureBlocking(false);

        try {
            socket.connect(InetSocketAddress(message.https.host, message.https.port))
        } catch (ex: Exception) {
        }

        return socket
    }
}