package io.appstarterpack.networking

interface HttpClient {
    suspend fun <T : Any> send(request: RequestDefinition<T>): T
}
