package com.servacode.directory.core.designsystem

import androidx.compose.runtime.Composable

/** A picture the reader picked, read into memory as it will be sent. */
class PickedImage(val fileName: String, val mediaType: String, val bytes: ByteArray)

/** As the backend's own limit: nothing larger is read into memory to be sent. */
const val PICKED_IMAGE_LIMIT = 10 * 1024 * 1024

/**
 * The phone's own photo picker, one picture at a time (DECISIONS 098 and 099). What the reader
 * picks reaches [onPicked]; a picture that cannot be read, or is larger than
 * [PICKED_IMAGE_LIMIT], reaches it as null, so the screen can say so; nothing picked reaches
 * nothing. The account's picture and an owner's evidence are picked with it.
 */
@Composable
expect fun rememberImagePicker(onPicked: (PickedImage?) -> Unit): () -> Unit
