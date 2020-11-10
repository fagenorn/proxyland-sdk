package com.betroix.proxyland.websocket

import com.betroix.proxyland.models.json.*
import okhttp3.Response

class SocketEvents {
    open class Event

    class BaseMessageEvent internal constructor(val message: String): Event()

    class OpenStatusEvent internal constructor(val response: Response) : Event()

    class CloseStatusEvent internal constructor(val code: Int, val reason: String) : Event()

    class FailureStatusEvent internal constructor(val throwable: Throwable) : Event()

    class ReconnectStatusEvent internal constructor(val attemptsCount: Int, val attemptDelay: Long) : Event()

    class ChangeStatusEvent internal constructor(val status: SocketState) : Event()

    class AuthMessageEvent internal constructor(val response: BaseResponse): Event()

    class HeartbeatEvent internal constructor(val response: BaseResponse): Event()

    class HttpsEvent internal constructor(val response: TypedBaseResponse<HttpsData>): Event()

    class HttpEvent internal constructor(val response: TypedBaseResponse<HttpData>): Event()

    class StatusEvent internal constructor(val response: TypedBaseResponse<StatusData>): Event()
}