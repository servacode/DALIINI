package com.servacode.directory.feature.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.servacode.directory.core.network.OwnerUploadPayload
import com.servacode.directory.core.network.UploadReader

/** Android's photo picker, and the reader every upload uses, as before. */
@Composable
internal actual fun rememberImagePicker(onPicked: (OwnerUploadPayload) -> Unit): () -> Unit {
    val context = LocalContext.current
    val picked by rememberUpdatedState(onPicked)
    val reader = remember(context) { UploadReader(context.applicationContext) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { chosen -> reader.read(chosen).onSuccess(picked) }
    }
    return remember(launcher) {
        { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    }
}
