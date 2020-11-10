package com.betroix.proxyland.http

import android.util.Log
import com.betroix.proxyland.Api
import com.betroix.proxyland.models.json.HttpData
import com.betroix.proxyland.models.json.TypedBaseResponse
import com.betroix.proxyland.websocket.IWebSocket
import com.betroix.proxyland.websocket.SocketEvents
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Pipe
import okio.buffer

class HttpRequestBody(websocket: IWebSocket, response: TypedBaseResponse<HttpData>) :
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
            .takeUntil { it.response.data.end }
            .filter { it.response.id == response.id && it.response.data.body?.type == "Buffer" }
            .doOnError { Log.e(TAG, "HTTP WRITE REQUEST", it) }
            .doOnComplete {
                bufferedSink.close()
            }
            .subscribe ({ i ->
                val bytes = i.response.data.body!!.data!!.map { j -> j.toByte() }.toByteArray()
                bufferedSink.write(bytes)
            }, {})
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