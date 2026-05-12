package io.appstarterpack.networking

internal actual fun isNoConnectionException(e: Exception): Boolean =
    e is java.net.UnknownHostException ||
    e is java.net.ConnectException ||
    e is java.net.NoRouteToHostException
