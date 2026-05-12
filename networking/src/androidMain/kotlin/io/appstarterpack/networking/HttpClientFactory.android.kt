package io.appstarterpack.networking

import io.ktor.client.engine.okhttp.OkHttp

actual fun createHttpClient(config: HttpClientConfig): HttpClient =
    KtorHttpClient(config, OkHttp.create())
