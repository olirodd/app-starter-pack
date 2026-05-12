package io.appstarterpack.networking

import io.ktor.client.HttpClient as KtorClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod as KtorMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal class KtorHttpClient(
    private val config: HttpClientConfig,
    engine: HttpClientEngine
) : HttpClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = KtorClient(engine) {
        install(ContentNegotiation) { json(json) }
        expectSuccess = false
    }

    override suspend fun <T : Any> send(request: RequestDefinition<T>): T {
        try {
            val response = client.request {
                url(config.baseUrl.trimEnd('/') + "/" + request.path.trimStart('/'))
                method = request.method.toKtor()
                config.defaultHeaders.forEach { (k, v) -> header(k, v) }
                config.tokenProvider?.invoke()?.let { header("X-AUTH-TOKEN", it) }
                request.headers.forEach { (k, v) -> header(k, v) }
                request.queryParameters.forEach { (k, v) ->
                    url { parameters.append(k, v) }
                }
                request.bodyJson?.let {
                    contentType(ContentType.Application.Json)
                    setBody(it)
                }
            }

            if (!response.status.isSuccess()) {
                throw NetworkError.HttpError(response.status.value)
            }

            return try {
                json.decodeFromString(request.responseSerializer, response.bodyAsText())
            } catch (e: Exception) {
                throw NetworkError.DecodingFailed(e)
            }
        } catch (e: NetworkError) {
            throw e
        } catch (e: Exception) {
            if (isNoConnectionException(e)) throw NetworkError.NoConnection
            throw NetworkError.RequestFailed(e)
        }
    }

    private fun HttpMethod.toKtor() = when (this) {
        HttpMethod.GET -> KtorMethod.Get
        HttpMethod.POST -> KtorMethod.Post
        HttpMethod.PUT -> KtorMethod.Put
        HttpMethod.DELETE -> KtorMethod.Delete
        HttpMethod.PATCH -> KtorMethod.Patch
    }
}
