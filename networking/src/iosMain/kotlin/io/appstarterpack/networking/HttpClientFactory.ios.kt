package io.appstarterpack.networking

import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSClassFromString
import platform.Foundation.NSProcessInfo

actual fun createHttpClient(config: HttpClientConfig): HttpClient {
    return KtorHttpClient(config, Darwin.create {
        if (NSProcessInfo.processInfo.arguments.any { it == "--uitesting" }) {
            configureSession {
                val testClass = NSClassFromString("UITestURLProtocol") ?: return@configureSession
                @Suppress("UNCHECKED_CAST")
                protocolClasses = (listOf(testClass) + (protocolClasses?.filterNotNull() ?: emptyList())) as List<Any>
            }
        }
    })
}
