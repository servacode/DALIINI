package com.servacode.directory.core.designsystem

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream

/** Android's photo picker, the picture read as the uploads have always read theirs. */
@Composable
actual fun rememberImagePicker(onPicked: (PickedImage?) -> Unit): () -> Unit {
    val context = LocalContext.current.applicationContext
    val picked by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { chosen -> picked(runCatching { read(context, chosen) }.getOrNull()) }
    }
    return remember(launcher) {
        { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    }
}

private fun read(context: Context, uri: Uri): PickedImage {
    val resolver = context.contentResolver
    val mediaType = resolver.getType(uri) ?: "application/octet-stream"
    val bytes = resolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= PICKED_IMAGE_LIMIT) { "File exceeds client upload limit." }
            output.write(buffer, 0, read)
        }
        output.toByteArray()
    } ?: error("Unable to read selected file.")
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mediaType)
    return PickedImage(if (extension == null) "upload" else "upload.$extension", mediaType, bytes)
}
