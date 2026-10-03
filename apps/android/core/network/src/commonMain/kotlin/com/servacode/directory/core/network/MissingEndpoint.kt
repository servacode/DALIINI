package com.servacode.directory.core.network

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException

/**
 * A 404 (or 405) from an endpoint this build knows an older backend may not have yet: the
 * feature is simply not offered, rather than shown as broken.
 */
fun Throwable.isMissingEndpoint(): Boolean =
    (this as? AppException)?.error?.let {
        it.kind == AppError.Kind.NOT_FOUND || it.status == 404 || it.status == 405
    } == true
