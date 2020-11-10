package com.betroix.proxyland.http

import android.util.Log
import com.betroix.proxyland.models.protobuf.Model
import com.betroix.proxyland.websocket.IWebSocket
import com.betroix.proxyland.websocket.SocketEvents
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Pipe
import okio.buffer

class HttpRequestBody(websocket: IWebSocket, message: Model.ServerMessage) :
    RequestBody() {
    companion object {
        private val TAG = "Proxyland Http Request"
    }

    private val pipe: Pipe = Pipe(8192)
    private val bufferedSink: BufferedSink = pipe.sink.buffer()

    init {
        // Start the sink writer and listen for any extra data.
        websocket.observe(SocketEvents.HttpEvent::class.java)
            .subscribeOn(Schedulers.io())
            .filter { it.response.id == message.id }
            .takeUntil { it.response.http.end }
            .doOnError { Log.e(TAG, "HTTP WRITE REQUEST", it) }
            .doOnComplete { bufferedSink.close() }
            .subscribe ({ bufferedSink.write(it.response.http.data.toByteArray()) }, {})
    }

    override fun contentType(): MediaType? {
        // Setting this to null seems to cause the okhttp3 client to use the included "Content-Type" header. Best so we don't modify the user request even further.
        return null
    }

    override fun writeTo(sink: BufferedSink) {
        // Start writing the buffered sink to the request asynchronously.
        sink.writeAll(pipe.source);
    }
}