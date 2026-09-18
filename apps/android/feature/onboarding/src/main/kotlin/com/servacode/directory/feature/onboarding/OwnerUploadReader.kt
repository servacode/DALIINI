package com.servacode.directory.feature.onboarding

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.servacode.directory.core.network.OwnerUploadPayload
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val CLIENT_UPLOAD_LIMIT = 10 * 1024 * 1024

class OwnerUploadReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun read(uri: Uri): Result<OwnerUploadPayload> = runCatching {
        val resolver = context.contentResolver
        val mediaType = resolver.getType(uri) ?: "application/octet-stream"
        val bytes = resolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= CLIENT_UPLOAD_LIMIT) { "File exceeds client upload limit." }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        } ?: error("Unable to read selected file.")
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mediaType)
        OwnerUploadPayload(if (extension == null) "upload" else "upload.$extension", mediaType, bytes)
    }
}
