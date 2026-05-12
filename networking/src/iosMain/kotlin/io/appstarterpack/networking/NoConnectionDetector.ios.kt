package io.appstarterpack.networking

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import platform.Foundation.NSURLErrorNetworkConnectionLost
import platform.Foundation.NSURLErrorNotConnectedToInternet

internal actual fun isNoConnectionException(e: Exception): Boolean {
    if (e is DarwinHttpRequestException) {
        val code = e.origin.code
        return code == NSURLErrorNotConnectedToInternet || code == NSURLErrorNetworkConnectionLost
    }
    return false
}
