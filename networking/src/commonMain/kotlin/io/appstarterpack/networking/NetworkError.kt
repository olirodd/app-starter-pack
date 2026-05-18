package io.appstarterpack.networking

sealed class NetworkError(cause: Throwable? = null) : Exception(cause) {
    data object NoConnection : NetworkError() {
        override val message = "No internet connection"
    }
    data class HttpError(val statusCode: Int) : NetworkError() {
        override val message = "HTTP $statusCode"
    }
    data class DecodingFailed(val error: Throwable) : NetworkError(error) {
        override val message = "Decoding failed: ${error.message}"
    }
    data class RequestFailed(val error: Throwable) : NetworkError(error) {
        override val message = "Request failed: ${error.message}"
    }
}
