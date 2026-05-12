package io.appstarterpack.networking

sealed class NetworkError(cause: Throwable? = null) : Exception(cause) {
    data object NoConnection : NetworkError()
    data class HttpError(val statusCode: Int) : NetworkError()
    data class DecodingFailed(val error: Throwable) : NetworkError(error)
    data class RequestFailed(val error: Throwable) : NetworkError(error)
}
