package io.appstarterpack.networking

import io.ktor.client.HttpClient as KtorClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
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
        // A programmer error, not a network one, so it is raised before the
        // try below and is never wrapped as NetworkError.RequestFailed.
        require(request.bodyJson == null || request.multipartBody == null) {
            "${request.path}: a request sets bodyJson or multipartBody, not both"
        }
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
                // The content carries its own multipart/form-data Content-Type,
                // boundary included, so none is set here.
                request.multipartBody?.let { setBody(it.toFormData()) }
            }

            if (!response.status.isSuccess()) {
                if (response.status.value == 401) config.onUnauthorized?.invoke()
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

    private fun MultipartBody.toFormData() = MultiPartFormDataContent(
        formData {
            parts.forEach { part ->
                when (part) {
                    is MultipartBody.TextPart -> append(part.name, part.value)
                    is MultipartBody.FilePart -> append(
                        part.name,
                        part.bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, part.contentType)
                            append(HttpHeaders.ContentDisposition, "filename=\"${part.filename}\"")
                        }
                    )
                }
            }
        }
    )

    private fun HttpMethod.toKtor() = when (this) {
        HttpMethod.GET -> KtorMethod.Get
        HttpMethod.POST -> KtorMethod.Post
        HttpMethod.PUT -> KtorMethod.Put
        HttpMethod.DELETE -> KtorMethod.Delete
        HttpMethod.PATCH -> KtorMethod.Patch
    }
}
