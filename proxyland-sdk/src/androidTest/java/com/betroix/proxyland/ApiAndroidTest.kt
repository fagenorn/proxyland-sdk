package com.betroix.proxyland

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.*
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class ApiAndroidTest {
    @Test
    fun send_https_get_request_ok() {
       ProxylandSdk(InstrumentationRegistry.getInstrumentation().targetContext, "partner-id")
               .initialize()

        val request: Request = Request.Builder()
                .url("https://httpbin.org/get")
                .build()

        send_proxy_request(request)
    }

    @Test
    fun send_http_get_request_ok() {
        ProxylandSdk(InstrumentationRegistry.getInstrumentation().targetContext,"partner-id")
                .initialize()

        val request: Request = Request.Builder()
                .url("http://httpbin.org/get")
                .build()

        send_proxy_request(request)
    }

    @Test
    fun send_https_post_request_ok() {
        ProxylandSdk(InstrumentationRegistry.getInstrumentation().targetContext,"partner-id")
                .initialize()

        val formBody = FormBody.Builder()
                .add("message", "test")
                .build()

        val request: Request = Request.Builder()
                .url("https://httpbin.org/post")
                .post(formBody)
                .build()

        send_proxy_request(request)
    }

    @Test
    fun send_http_post_request_ok() {
        ProxylandSdk(InstrumentationRegistry.getInstrumentation().targetContext,"partner-id")
                .initialize()

        val formBody = FormBody.Builder()
                .add("message", "test")
                .build()

        val request: Request = Request.Builder()
                .url("http://httpbin.org/post")
                .post(formBody)
                .build()

        send_proxy_request(request)
    }

    private fun send_proxy_request(request: Request) {
        val port = 9090
        val username = "Tg9tVvepoEDqp9X"
        val password = "xGD4r6o7GzlMWrI"

        val proxyAuthenticator = object: Authenticator {
            override fun authenticate(route: Route?, response: Response): Request? {
                val credential: String = Credentials.basic(username, password)
                return response.request
                    .newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build()
            }
        }

        val client = OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(InetAddress.getByName("localhost"), port)))
                .proxyAuthenticator(proxyAuthenticator)
                .build()

        client.newCall(request)
                .execute()
                .use {
                    it.body?.string()
                    Assert.assertTrue(it.isSuccessful)
                }
    }
}