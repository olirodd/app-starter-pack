package io.appstarterpack.networking

data class HttpClientConfig(
    val baseUrl: String,
    val tokenProvider: (() -> String?)? = null,
    val defaultHeaders: Map<String, String> = emptyMap()
)
