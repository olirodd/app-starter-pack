package io.appstarterpack.networking

data class HttpClientConfig(
    val baseUrl: String,
    val tokenProvider: (() -> String?)? = null,
    val defaultHeaders: Map<String, String> = emptyMap(),
    /**
     * Invoked when a request comes back with HTTP 401 Unauthorized, before the
     * [NetworkError.HttpError] is thrown. Lets the host app react to an expired
     * session (e.g. clear the token and log the user out) in one place for every request.
     */
    val onUnauthorized: (() -> Unit)? = null
)
