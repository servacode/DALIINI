package com.servacode.directory.core.transport

import com.servacode.directory.core.network.OwnerUploadPayload
import io.ktor.client.request.forms.FormPart
import io.ktor.client.request.forms.InputProvider
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.io.Buffer

/**
 * A picked file as the multipart part the upload operations expect: named `file`, carrying the
 * payload's file name and, when it is a well-formed media type, its type — the part Android
 * builds with OkHttp's `MultipartBody.Part.createFormData("file", fileName, body)`, for a
 * facility's photographs and evidence, a claim's evidence and the account picture alike.
 *
 * Two OkHttp rules are kept so the backend sees what it sees from Android:
 *  - the file name is quoted with `"`, CR and LF written as `%22`, `%0D` and `%0A`;
 *  - a media type OkHttp cannot parse is not sent at all, rather than sent as it is.
 *
 * Ktor writes the part's `Content-Disposition` itself, as `form-data; name=file` with the file
 * name appended, and adds its `Content-Length`, as OkHttp does.
 */
internal fun OwnerUploadPayload.toFilePart(): FormPart<InputProvider> {
    val headers = Headers.build {
        append(HttpHeaders.ContentDisposition, "filename=${quotedForMultipart(fileName)}")
        if (isOkHttpMediaType(mediaType)) append(HttpHeaders.ContentType, mediaType)
    }
    val content = bytes
    return FormPart(
        key = FILE_PART,
        value = InputProvider(content.size.toLong()) { Buffer().apply { write(content) } },
        headers = headers,
    )
}

private const val FILE_PART = "file"

/** OkHttp's `appendQuotedString`, which quotes multipart names and file names. */
internal fun quotedForMultipart(value: String): String = buildString {
    append('"')
    for (c in value) {
        when (c) {
            '\n' -> append("%0A")
            '\r' -> append("%0D")
            '"' -> append("%22")
            else -> append(c)
        }
    }
    append('"')
}

/**
 * Whether OkHttp's `toMediaTypeOrNull` reads [value] as a media type: `type/subtype` made of
 * token characters, then any number of `;` parameters whose values are tokens or quoted.
 */
internal fun isOkHttpMediaType(value: String): Boolean {
    val typeSubtype = TYPE_SUBTYPE.matchStartingAt(value, 0) ?: return false
    var next = typeSubtype.range.last + 1
    while (next < value.length) {
        val parameter = PARAMETER.matchStartingAt(value, next) ?: return false
        next = parameter.range.last + 1
    }
    return true
}

/** The first match from [index], only when it begins there; OkHttp's `matchAtPolyfill`. */
private fun Regex.matchStartingAt(input: String, index: Int): MatchResult? =
    find(input, index)?.takeIf { it.range.first == index }

private const val TOKEN = "([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)"
private const val QUOTED = "\"([^\"]*)\""
private val TYPE_SUBTYPE = Regex("$TOKEN/$TOKEN")
private val PARAMETER = Regex(";\\s*(?:$TOKEN=(?:$TOKEN|$QUOTED))?")
