package io.appstarterpack.networking

/**
 * A `multipart/form-data` request body: named text fields plus optional files.
 *
 * Set it as [RequestDefinition.multipartBody] to upload a file alongside form
 * fields. A request carries either a JSON body ([RequestDefinition.bodyJson]) or
 * a multipart body, never both; [KtorHttpClient] refuses one that sets both.
 */
class MultipartBody(val parts: List<Part>) {

    sealed interface Part {
        /** The form field name the server reads the part under. */
        val name: String
    }

    /** A plain form field. */
    class TextPart(override val name: String, val value: String) : Part

    /**
     * A file. [contentType] is a MIME type such as `image/jpeg`; the server
     * should not trust it, so sniff the bytes there if it matters.
     */
    class FilePart(
        override val name: String,
        val bytes: ByteArray,
        val filename: String,
        val contentType: String
    ) : Part
}
