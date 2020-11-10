package com.betroix.proxyland.http

import android.util.Log
import com.betroix.proxyland.IApi
import com.betroix.proxyland.exceptions.RequestException
import com.betroix.proxyland.exceptions.ResponseException
import com.betroix.proxyland.models.json.HttpBodyResponse
import com.betroix.proxyland.models.json.HttpData
import com.betroix.proxyland.models.json.HttpDataResponse
import com.betroix.proxyland.models.json.TypedBaseResponse
import okhttp3.*
import java.io.BufferedInputStream
import java.io.IOException
import java.nio.ByteBuffer

internal class Http(private val api: IApi, private val typedResponse: TypedBaseResponse<HttpData>) {
    companion object {
        private val TAG = "Proxyland Api"
    }

    private val client = OkHttpClient()

    fun request() {
        val body = typedResponse.data.body ?: throw RequestException("No body provided.");
        if (body.url == null) throw RequestException("No url provided.")
        if (body.method == null) throw RequestException("No method provided.")
        if (body.headers == null) throw RequestException("No headers provided.")

        val builder = Request.Builder()
            .url(body.url)
            .method(
                body.method,
                if (body.method == "GET" || body.method == "HEAD") null else HttpRequestBody(
                    api.websocket,
                    typedResponse
                )
            );

        for ((name, key) in body.headers) {
            builder.addHeader(name, key)
        }

        // Asynchronously wait for response
        client.newCall(builder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "HTTP REQUEST", e)
                val data = HttpDataResponse(
                    HttpBodyResponse(listOf(), "Buffer"),
                    mapOf("statusCode" to "400"),
                    false
                );
                api.sendToSocket(typedResponse, data)
                api.sendToSocket(typedResponse, HttpData(end = true))
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
                                val httpBody = HttpBodyResponse(
                                    buffer.sliceArray(IntRange(0, read - 1)).map { i -> i.toInt() },
                                    "Buffer"
                                )

                                Log.d(TAG, "Slice buffer: ${System.nanoTime() - requestStart}")
                                requestStart = System.nanoTime()

                                var headers: Map<String, String>? = null
                                if(!headersSent) {
                                    headersSent = true
                                    headers = response.headers.toMap().plus("statusCode" to response.code.toString())
                                }

                                val data = HttpDataResponse(
                                    httpBody,
                                    headers,
                                    false
                                )

                                api.sendToSocket(typedResponse, data)

                                Log.d(TAG, "Write: ${System.nanoTime() - requestStart}")
                                requestStart = System.nanoTime()

                                read = bufferedInputStream.read(buffer)

                                Log.d(TAG, "Read: ${System.nanoTime() - requestStart}")
                                requestStart = System.nanoTime()
                            }
                        }
                    }

                    api.sendToSocket(typedResponse, HttpData(end = true))
                }
            }
        })
    }
}