package com.servacode.directory.feature.account

import androidx.compose.runtime.Composable
import com.servacode.directory.core.network.OwnerUploadPayload

/**
 * The phone's own photo picker, one picture at a time. What the reader picks reaches [onPicked]
 * as it will be sent; nothing is picked, or a picture too large to send, reaches nothing
 * (DECISION-098).
 */
@Composable
internal expect fun rememberImagePicker(onPicked: (OwnerUploadPayload) -> Unit): () -> Unit
