package io.appstarterpack.networking

import io.ktor.client.engine.darwin.Darwin

actual fun createHttpClient(config: HttpClientConfig): HttpClient =
    KtorHttpClient(config, Darwin.create())
