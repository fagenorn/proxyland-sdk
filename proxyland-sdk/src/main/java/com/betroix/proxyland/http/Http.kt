package com.betroix.proxyland.http

import android.util.Log
import com.betroix.proxyland.IApi
import com.betroix.proxyland.exceptions.RequestException
import com.betroix.proxyland.exceptions.ResponseException
import com.betroix.proxyland.models.protobuf.Model
import com.google.protobuf.ByteString
import okhttp3.*
import java.io.BufferedInputStream
import java.io.IOException

internal class Http(private val api: IApi, private val message: Model.ServerMessage) {
    companion object {
        private val TAG = "Proxyland Api"
    }

    private val client = OkHttpClient()

    fun request() {
        val body =
            if (message.bodyOneofCase == Model.ServerMessage.BodyOneofCase.HTTP) message.http else throw RequestException(
                "No body provided."
            );
        if (body.url == null) throw RequestException("No url provided.")
        if (body.method == null) throw RequestException("No method provided.")
        if (body.headersMap == null) throw RequestException("No headers provided.")

        val builder = Request.Builder()
            .url(body.url)
            .method(
                body.method,
                if (body.method == "GET" || body.method == "HEAD") null else HttpRequestBody(
                    api.websocket,
                    message
                )
            );

        for ((name, key) in body.headersMap) {
            builder.addHeader(name, key)
        }

        // Asynchronously wait for response
        client.newCall(builder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "HTTP REQUEST", e)


                api.sendToSocket(
                    message,
                    Model.RemoteMessage.newBuilder().setHttp(
                        Model.HttpRemoteMessage.newBuilder().setStatusCode(400).setEnd(false)
                    )
                )
                api.sendToSocket(
                    message,
                    Model.RemoteMessage.newBuilder()
                        .setHttp(Model.HttpRemoteMessage.newBuilder().setEnd(true))
                )
            }

            override fun onResponse(call: Call, response: Response) {
                var requestStart = System.nanoTime()

                response.body.use {
                    if (it == null) throw ResponseException("No body provided.");

                    it.byteStream().use { inputStream ->
                        BufferedInputStream(inputStream).use { bufferedInputStream ->
                            var headersSent = false
                            val buffer = ByteArray(8192)
                            var read = bufferedInputStream.read(buffer)

                            Log.d(TAG, "First read: ${System.nanoTime() - requestStart}")
                            requestStart = System.nanoTime()

                            while (read > -1) {
                                Log.d(TAG, "Slice buffer: ${System.nanoTime() - requestStart}")
                                requestStart = System.nanoTime()

                                val http = Model.HttpRemoteMessage.newBuilder()
                                if (!headersSent) {
                                    headersSent = true
                                    http.putAllHeaders(response.headers.toMap())
                                    http.statusCode = response.code
                                }

                                http.data = ByteString.copyFrom(buffer, 0, read)
                                http.end = false

                                api.sendToSocket(
                                    message,
                                    Model.RemoteMessage.newBuilder().setHttp(http)
                                )

                                Log.d(TAG, "Write: ${System.nanoTime() - requestStart}")
                                requestStart = System.nanoTime()

                                read = bufferedInputStream.read(buffer)

                                Log.d(TAG, "Read: ${System.nanoTime() - requestStart}")
                                requestStart = System.nanoTime()
                            }
                        }
                    }

                    api.sendToSocket(
                        message,
                        Model.RemoteMessage.newBuilder()
                            .setHttp(Model.HttpRemoteMessage.newBuilder().setEnd(true))
                    )
                }
            }
        })
    }
}