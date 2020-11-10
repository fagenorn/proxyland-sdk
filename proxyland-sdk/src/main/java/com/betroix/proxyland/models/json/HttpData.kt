package com.betroix.proxyland.models.json

data class HttpData (
        val body: HttpBody? = null,
        val end: Boolean,
)

data class HttpBody (
        val url: String? = null,
        val method: String? = null,
        val host: String? = null,
        val port: String? = null,
        val path: String? = null,
        val headers: Map<String, String>? = null,
        val data: List<Int>? = null,
        val type: String? = null,
)

data class HttpDataResponse (
        val body: HttpBodyResponse,
        val headers: Map<String, String>? = null,
        val end: Boolean,
)

data class HttpBodyResponse(
        val data: List<Int>,
        val type: String,
)