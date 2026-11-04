package io.appstarterpack.networking

import kotlinx.serialization.KSerializer

interface RequestDefinition<T : Any> {
    val path: String
    val method: HttpMethod
    val headers: Map<String, String> get() = emptyMap()
    val queryParameters: Map<String, String> get() = emptyMap()
    val bodyJson: String? get() = null
    /** A `multipart/form-data` body, e.g. a file upload. Set this or [bodyJson], never both. */
    val multipartBody: MultipartBody? get() = null
    val responseSerializer: KSerializer<T>
}
