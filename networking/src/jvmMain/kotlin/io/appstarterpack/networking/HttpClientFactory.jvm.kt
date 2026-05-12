package io.appstarterpack.networking

import io.ktor.client.engine.java.Java

actual fun createHttpClient(config: HttpClientConfig): HttpClient =
    KtorHttpClient(config, Java.create())
