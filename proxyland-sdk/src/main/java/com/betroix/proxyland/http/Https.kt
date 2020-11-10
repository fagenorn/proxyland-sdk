package com.betroix.proxyland.http

import android.util.Log
import com.betroix.proxyland.IApi
import com.betroix.proxyland.exceptions.ProxylandException
import com.betroix.proxyland.models.json.HttpsData
import com.betroix.proxyland.models.json.TypedBaseResponse
import com.betroix.proxyland.websocket.SocketEvents
import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util.concurrent.TimeUnit

internal class Https(private val api: IApi, val response: TypedBaseResponse<HttpsData>) {
    companion object {
        private val TAG = "Proxyland Https Request"
    }

    var writerDispose: Disposable = Disposable.empty()

    fun connect(): Single<SocketChannel> {
        val socket = createSocket();

        // Monitor socket till connection is established
        return Observable.just(socket)
            .delay(50, TimeUnit.MILLISECONDS)
            .repeatUntil { socket.finishConnect() }
            .filter { socket.finishConnect() }
            .firstOrError()
            .doOnError { Log.w(TAG, "HTTPS CONNECT", it) }
            .doOnSuccess {
                // Tunnel websocket client data to socket input.
                writerDispose = api.websocket.observe(SocketEvents.HttpsEvent::class.java)
                    .filter { it.response.id == response.id && it.response.data.data != null }
                    .doOnError { Log.e(TAG, "HTTPS WRITE", it) }
                    .subscribe({
                        val buf: ByteBuffer =
                            ByteBuffer.wrap(it.response.data.data!!.map { s -> s.toByte() }
                                .toByteArray())
                        socket.write(buf)
                    }, {})

                // Send success response to Server
                api.sendToSocket(response, "HTTP/1.1 200 Connection established\r\n\r\n")
            }
    }

    private fun createSocket(): SocketChannel {
        val socket = SocketChannel.open()
        socket.configureBlocking(false);
        socket.connect(InetSocketAddress(getHost(), response.data.port ?: 443))

        return socket
    }

    private fun getHost(): String {
        // Since url contains port too, need only host
        return response.data.url?.split(':')?.get(0) ?: throw ProxylandException("Invalid URL")
    }
}