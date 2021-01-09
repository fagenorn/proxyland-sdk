package com.betroix.proxyland.http

import android.util.Log
import com.betroix.proxyland.IApi
import com.betroix.proxyland.models.protobuf.Model
import com.google.protobuf.ByteString
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.TimeUnit

internal class HttpsSelector(private val api: IApi) {
    companion object {
        private val TAG = "Proxyland Https Selector"
    }

    private val selector: Selector = Selector.open()
    private var running = false
    private var terminate = false

    init {
        // Start socket reader loop
        Observable.timer(100, TimeUnit.MILLISECONDS)
            .repeatUntil { terminate }
            .filter { !running }
            .doOnError { Log.e(TAG, "HTTPS LOOP", it) }
            .subscribe({ loop() }, {})
    }

    fun terminate() {
        terminate = true;
    }

    fun register(https: Https) {
        https.connect()
            .subscribe({
                it.register(
                    selector,
                    SelectionKey.OP_READ,
                    Pair(https.message, https.writerDispose)
                )
            }, {})
    }

    @Suppress("UNCHECKED_CAST")
    private fun loop() {
        running = true
        try {
            val noOfKeys = selector.selectNow()
            if (noOfKeys == 0) return
            val selectedKeys = selector.selectedKeys()
            val iter = selectedKeys.iterator()

            while (iter.hasNext()) {
                val key = iter.next() as SelectionKey
                val (message, disposable) = key.attachment() as Pair<Model.ServerMessage, Disposable?>

                iter.remove()

                // Start reading from socket and send to client.
                if (key.isReadable) {
                    val client = key.channel() as SocketChannel
                    val buffer = ByteBuffer.allocate(4096)
                    var read: Int

                    do {
                        read = client.read(buffer)
                        buffer.flip()

                        if (read > 0) {
                            val result = ByteArray(read)
                            buffer.get(result, 0, read)
                            api.sendToSocket(
                                message,
                                Model.RemoteMessage.newBuilder().setHttps(
                                    Model.HttpsRemoteMessage.newBuilder().setData(
                                        ByteString.copyFrom(result)
                                    )
                                )
                            )
                        }

                        buffer.clear()
                    } while (read > 0)

                } else {
                    // Dispose the writer observable.
                    disposable?.dispose()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTTPS READ SOCKET", e)
        } finally {
            running = false
        }
    }
}