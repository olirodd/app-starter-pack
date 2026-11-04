package io.appstarterpack.networking

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.Headers
import io.ktor.http.HttpMethod as KtorHttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KtorHttpClientTest {

    @Serializable
    private data class TestResponse(val id: Int, val name: String)

    private fun mockClient(
        responseBody: String = """{"id":1,"name":"test"}""",
        statusCode: HttpStatusCode = HttpStatusCode.OK,
        captureRequest: ((HttpRequestData) -> Unit)? = null,
        config: HttpClientConfig = HttpClientConfig(baseUrl = "https://api.test.com")
    ): KtorHttpClient {
        val engine = MockEngine { request ->
            captureRequest?.invoke(request)
            respond(
                content = ByteReadChannel(responseBody),
                status = statusCode,
                headers = headersOf("Content-Type", "application/json")
            )
        }
        return KtorHttpClient(config, engine)
    }

    private fun testRequest(
        path: String = "/test",
        method: HttpMethod = HttpMethod.GET,
        queryParameters: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        bodyJson: String? = null
    ) = object : RequestDefinition<TestResponse> {
        override val path = path
        override val method = method
        override val queryParameters = queryParameters
        override val headers = headers
        override val bodyJson = bodyJson
        override val responseSerializer = serializer<TestResponse>()
    }

    // --- Response parsing ---

    @Test
    fun `GET request returns parsed response`() = runTest {
        val result = mockClient().send(testRequest())
        assertEquals(1, result.id)
        assertEquals("test", result.name)
    }

    @Test
    fun `invalid JSON throws NetworkError DecodingFailed`() = runTest {
        assertFailsWith<NetworkError.DecodingFailed> {
            mockClient(responseBody = "not json").send(testRequest())
        }
    }

    // --- HTTP methods ---

    @Test
    fun `POST request sends body and uses correct method`() = runTest {
        var capturedBody: String? = null
        var capturedMethod: KtorHttpMethod? = null
        mockClient(captureRequest = {
            capturedMethod = it.method
            capturedBody = (it.body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString()
        }).send(testRequest(method = HttpMethod.POST, bodyJson = """{"name":"test"}"""))
        assertEquals(KtorHttpMethod.Post, capturedMethod)
        assertEquals("""{"name":"test"}""", capturedBody)
    }

    @Test
    fun `PUT request uses correct method`() = runTest {
        var capturedMethod: KtorHttpMethod? = null
        mockClient(captureRequest = { capturedMethod = it.method })
            .send(testRequest(method = HttpMethod.PUT))
        assertEquals(KtorHttpMethod.Put, capturedMethod)
    }

    @Test
    fun `DELETE request uses correct method`() = runTest {
        var capturedMethod: KtorHttpMethod? = null
        mockClient(captureRequest = { capturedMethod = it.method })
            .send(testRequest(method = HttpMethod.DELETE))
        assertEquals(KtorHttpMethod.Delete, capturedMethod)
    }

    @Test
    fun `PATCH request uses correct method`() = runTest {
        var capturedMethod: KtorHttpMethod? = null
        mockClient(captureRequest = { capturedMethod = it.method })
            .send(testRequest(method = HttpMethod.PATCH))
        assertEquals(KtorHttpMethod.Patch, capturedMethod)
    }

    // --- HTTP errors ---

    @Test
    fun `HTTP 4xx throws NetworkError HttpError with correct status code`() = runTest {
        val error = assertFailsWith<NetworkError.HttpError> {
            mockClient(statusCode = HttpStatusCode.NotFound).send(testRequest())
        }
        assertEquals(404, error.statusCode)
    }

    @Test
    fun `HTTP 401 throws NetworkError HttpError with status 401`() = runTest {
        val error = assertFailsWith<NetworkError.HttpError> {
            mockClient(statusCode = HttpStatusCode.Unauthorized).send(testRequest())
        }
        assertEquals(401, error.statusCode)
    }

    @Test
    fun `HTTP 401 invokes onUnauthorized callback before throwing`() = runTest {
        var unauthorizedCalls = 0
        assertFailsWith<NetworkError.HttpError> {
            mockClient(
                statusCode = HttpStatusCode.Unauthorized,
                config = HttpClientConfig(
                    baseUrl = "https://api.test.com",
                    onUnauthorized = { unauthorizedCalls++ }
                )
            ).send(testRequest())
        }
        assertEquals(1, unauthorizedCalls)
    }

    @Test
    fun `non-401 errors do not invoke onUnauthorized callback`() = runTest {
        var unauthorizedCalls = 0
        val config = HttpClientConfig(
            baseUrl = "https://api.test.com",
            onUnauthorized = { unauthorizedCalls++ }
        )
        assertFailsWith<NetworkError.HttpError> {
            mockClient(statusCode = HttpStatusCode.Forbidden, config = config).send(testRequest())
        }
        assertFailsWith<NetworkError.HttpError> {
            mockClient(statusCode = HttpStatusCode.InternalServerError, config = config).send(testRequest())
        }
        assertEquals(0, unauthorizedCalls)
    }

    @Test
    fun `HTTP 500 throws NetworkError HttpError with status 500`() = runTest {
        val error = assertFailsWith<NetworkError.HttpError> {
            mockClient(statusCode = HttpStatusCode.InternalServerError).send(testRequest())
        }
        assertEquals(500, error.statusCode)
    }

    // --- Auth token ---

    @Test
    fun `token injected into X-AUTH-TOKEN header`() = runTest {
        var capturedHeaders: Headers? = null
        mockClient(
            captureRequest = { capturedHeaders = it.headers },
            config = HttpClientConfig(
                baseUrl = "https://api.test.com",
                tokenProvider = { "test-token-123" }
            )
        ).send(testRequest())
        assertEquals("test-token-123", capturedHeaders?.get("X-AUTH-TOKEN"))
    }

    @Test
    fun `no auth header when token provider returns null`() = runTest {
        var capturedHeaders: Headers? = null
        mockClient(
            captureRequest = { capturedHeaders = it.headers },
            config = HttpClientConfig(
                baseUrl = "https://api.test.com",
                tokenProvider = { null }
            )
        ).send(testRequest())
        assertNull(capturedHeaders?.get("X-AUTH-TOKEN"))
    }

    @Test
    fun `no auth header when no token provider set`() = runTest {
        var capturedHeaders: Headers? = null
        mockClient(captureRequest = { capturedHeaders = it.headers })
            .send(testRequest())
        assertNull(capturedHeaders?.get("X-AUTH-TOKEN"))
    }

    // --- Headers ---

    @Test
    fun `default headers included in request`() = runTest {
        var capturedHeaders: Headers? = null
        mockClient(
            captureRequest = { capturedHeaders = it.headers },
            config = HttpClientConfig(
                baseUrl = "https://api.test.com",
                defaultHeaders = mapOf("X-App-Version" to "1.0")
            )
        ).send(testRequest())
        assertEquals("1.0", capturedHeaders?.get("X-App-Version"))
    }

    @Test
    fun `per-request headers included in request`() = runTest {
        var capturedHeaders: Headers? = null
        mockClient(captureRequest = { capturedHeaders = it.headers })
            .send(testRequest(headers = mapOf("X-Request-ID" to "abc123")))
        assertEquals("abc123", capturedHeaders?.get("X-Request-ID"))
    }

    // --- URL construction ---

    @Test
    fun `query parameters appended to URL`() = runTest {
        var capturedUrl: Url? = null
        mockClient(captureRequest = { capturedUrl = it.url })
            .send(testRequest(queryParameters = mapOf("q" to "pokemon", "page" to "1")))
        assertEquals("pokemon", capturedUrl?.parameters?.get("q"))
        assertEquals("1", capturedUrl?.parameters?.get("page"))
    }

    @Test
    fun `trailing slash on baseUrl and leading slash on path produce correct URL`() = runTest {
        var capturedUrl: Url? = null
        mockClient(
            captureRequest = { capturedUrl = it.url },
            config = HttpClientConfig(baseUrl = "https://api.test.com/")
        ).send(testRequest(path = "/items"))
        assertEquals("/items", capturedUrl?.encodedPath)
    }

    // --- Network errors ---

    @Test
    fun `UnknownHostException throws NetworkError NoConnection`() = runTest {
        val engine = MockEngine { throw java.net.UnknownHostException("network unavailable") }
        val client = KtorHttpClient(HttpClientConfig(baseUrl = "https://api.test.com"), engine)
        assertFailsWith<NetworkError.NoConnection> { client.send(testRequest()) }
    }

    @Test
    fun `ConnectException throws NetworkError NoConnection`() = runTest {
        val engine = MockEngine { throw java.net.ConnectException("connection refused") }
        val client = KtorHttpClient(HttpClientConfig(baseUrl = "https://api.test.com"), engine)
        assertFailsWith<NetworkError.NoConnection> { client.send(testRequest()) }
    }

    @Test
    fun `NoRouteToHostException throws NetworkError NoConnection`() = runTest {
        val engine = MockEngine { throw java.net.NoRouteToHostException("no route") }
        val client = KtorHttpClient(HttpClientConfig(baseUrl = "https://api.test.com"), engine)
        assertFailsWith<NetworkError.NoConnection> { client.send(testRequest()) }
    }

    @Test
    fun `unknown engine exception wraps in NetworkError RequestFailed`() = runTest {
        val engine = MockEngine { throw RuntimeException("unexpected") }
        val client = KtorHttpClient(HttpClientConfig(baseUrl = "https://api.test.com"), engine)
        val error = assertFailsWith<NetworkError.RequestFailed> { client.send(testRequest()) }
        assertNotNull(error.error)
    }

    // --- RequestDefinition defaults ---

    @Test
    fun `RequestDefinition default properties return empty and null`() = runTest {
        val minimalRequest = object : RequestDefinition<TestResponse> {
            override val path = "/test"
            override val method = HttpMethod.GET
            override val responseSerializer = serializer<TestResponse>()
        }
        assertEquals(emptyMap(), minimalRequest.headers)
        assertEquals(emptyMap(), minimalRequest.queryParameters)
        assertNull(minimalRequest.bodyJson)
        assertNull(minimalRequest.multipartBody)
        mockClient().send(minimalRequest)
    }

    // --- Multipart bodies ---

    /** What a multipart request put on the wire, read inside the engine (the body is a stream, so the read is suspend). */
    private class Captured(val contentType: String?, val body: String, val bytes: ByteArray, val headers: Headers)

    private fun multipartClient(
        onRequest: (Captured) -> Unit,
        config: HttpClientConfig = HttpClientConfig(baseUrl = "https://api.test.com")
    ): KtorHttpClient = KtorHttpClient(config, MockEngine { request ->
        val bytes = request.body.toByteArray()
        onRequest(Captured(request.body.contentType?.toString(), bytes.decodeToString(), bytes, request.headers))
        respond(ByteReadChannel("""{"id":1,"name":"test"}"""), HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
    })

    private fun multipartRequest(body: MultipartBody, bodyJson: String? = null) = object : RequestDefinition<TestResponse> {
        override val path = "/upload"
        override val method = HttpMethod.POST
        override val bodyJson = bodyJson
        override val multipartBody = body
        override val responseSerializer = serializer<TestResponse>()
    }

    private val cover = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) // a PNG signature

    @Test
    fun `multipart request sends text and file parts as multipart form data`() = runTest {
        var captured: Captured? = null
        multipartClient({ captured = it }).send(multipartRequest(MultipartBody(listOf(
            MultipartBody.TextPart("title", "Antonblast"),
            MultipartBody.TextPart("description", "A game"),
            MultipartBody.FilePart("cover", cover, filename = "cover.png", contentType = "image/png"),
        ))))

        val c = assertNotNull(captured)
        assertTrue(c.contentType!!.startsWith("multipart/form-data; boundary="), c.contentType)
        // Text parts: name and value.
        assertTrue(c.body.contains("Content-Disposition: form-data; name=title"), c.body)
        assertTrue(c.body.contains("Antonblast"), c.body)
        assertTrue(c.body.contains("Content-Disposition: form-data; name=description"), c.body)
        // File part: field name, filename and content type, and the bytes themselves.
        assertTrue(c.body.contains("name=cover"), c.body)
        assertTrue(c.body.contains("filename=\"cover.png\""), c.body)
        assertTrue(c.body.contains("Content-Type: image/png"), c.body)
        assertTrue(c.bytes.toList().windowed(cover.size).any { it == cover.toList() }, "file bytes missing from the body")
    }

    @Test
    fun `multipart request still carries the auth token`() = runTest {
        var captured: Captured? = null
        multipartClient(
            { captured = it },
            HttpClientConfig(baseUrl = "https://api.test.com", tokenProvider = { "test-token-123" })
        ).send(multipartRequest(MultipartBody(listOf(MultipartBody.TextPart("title", "x")))))

        assertEquals("test-token-123", assertNotNull(captured).headers["X-AUTH-TOKEN"])
    }

    @Test
    fun `a JSON request is still sent as JSON`() = runTest {
        var contentType: String? = null
        KtorHttpClient(HttpClientConfig(baseUrl = "https://api.test.com"), MockEngine { request ->
            contentType = request.body.contentType?.toString()
            respond(ByteReadChannel("""{"id":1,"name":"test"}"""), HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
        }).send(testRequest(method = HttpMethod.POST, bodyJson = """{"name":"test"}"""))

        assertEquals("application/json", contentType)
    }

    @Test
    fun `a request with both a JSON and a multipart body is refused before anything is sent`() = runTest {
        var sent = false
        val client = multipartClient({ sent = true })

        assertFailsWith<IllegalArgumentException> {
            client.send(multipartRequest(MultipartBody(listOf(MultipartBody.TextPart("title", "x"))), bodyJson = "{}"))
        }
        assertEquals(false, sent)
    }

    // --- Factory and lifecycle ---

    @Test
    fun `createHttpClient returns a usable HttpClient`() {
        val client = createHttpClient(HttpClientConfig(baseUrl = "https://api.test.com"))
        assertNotNull(client)
    }
}
