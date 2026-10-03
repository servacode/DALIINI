package com.servacode.directory.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import com.servacode.directory.core.network.OwnerUploadPayload
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

/** As Android's reader: nothing larger than this is read into memory to be sent. */
private const val UPLOAD_LIMIT = 10 * 1024 * 1024

/**
 * The system's photo picker (PHPicker), which needs no permission: the app sees only the picture
 * chosen. It is sent as a JPEG, whatever the library keeps it as (HEIC on most iPhones), so the
 * backend receives what Android sends.
 */
@Composable
internal actual fun rememberImagePicker(onPicked: (OwnerUploadPayload) -> Unit): () -> Unit {
    val screen = LocalUIViewController.current
    val picked by rememberUpdatedState(onPicked)
    // The picker holds its delegate weakly; the screen holds it for as long as it is shown.
    val delegate = remember { PickerDelegate { picked(it) } }
    return remember(screen, delegate) {
        {
            val configuration = PHPickerConfiguration().apply {
                filter = PHPickerFilter.imagesFilter
                selectionLimit = 1
            }
            val picker = PHPickerViewController(configuration)
            picker.delegate = delegate
            screen.presentViewController(picker, animated = true, completion = null)
        }
    }
}

private class PickerDelegate(
    private val onPicked: (OwnerUploadPayload) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val provider = (didFinishPicking.firstOrNull() as? PHPickerResult)?.itemProvider ?: return
        provider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
            val payload = data?.let(::jpeg) ?: return@loadDataRepresentationForTypeIdentifier
            dispatch_async(dispatch_get_main_queue()) { onPicked(payload) }
        }
    }
}

/** The picture as a JPEG, or null when it cannot be read or is too large to send. */
internal fun jpeg(data: NSData): OwnerUploadPayload? {
    val image = UIImage.imageWithData(data) ?: return null
    val bytes = UIImageJPEGRepresentation(image, 0.85)?.toByteArray() ?: return null
    if (bytes.isEmpty() || bytes.size > UPLOAD_LIMIT) return null
    return OwnerUploadPayload("upload.jpg", "image/jpeg", bytes)
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}
