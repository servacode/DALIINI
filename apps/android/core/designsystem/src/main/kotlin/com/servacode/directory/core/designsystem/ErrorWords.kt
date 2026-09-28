package com.servacode.directory.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppErrorMessage
import com.servacode.directory.core.model.AppErrorMessages

/**
 * A refusal in words.
 *
 * The view models keep the error itself in their state rather than a sentence, and the screens
 * read it through this. That is what makes a second language a second `strings.xml`: a sentence
 * built when the request failed would be frozen in the language the phone had at that moment, and
 * a view model has no composition to read a resource in anyway.
 */
@Composable
@ReadOnlyComposable
fun appErrorText(error: AppError): String = stringResource(AppErrorMessages.of(error).resource())

private fun AppErrorMessage.resource(): Int = when (this) {
    AppErrorMessage.AUTHENTICATION_FAILED -> R.string.ds_error_authentication_failed
    AppErrorMessage.AUTHENTICATION_REQUIRED -> R.string.ds_error_authentication_required
    AppErrorMessage.PERMISSION_DENIED -> R.string.ds_error_permission_denied
    AppErrorMessage.NOT_FOUND -> R.string.ds_error_not_found
    AppErrorMessage.THROTTLED -> R.string.ds_error_throttled
    AppErrorMessage.VALIDATION_ERROR -> R.string.ds_error_validation
    AppErrorMessage.PROVINCE_REQUIRED -> R.string.ds_error_province_required
    AppErrorMessage.PHONE_ALREADY_REGISTERED -> R.string.ds_error_phone_already_registered
    AppErrorMessage.DUTY_NOT_SUPPORTED -> R.string.ds_error_duty_not_supported
    AppErrorMessage.DUTY_OVERLAP_OR_INVALID -> R.string.ds_error_duty_overlap
    AppErrorMessage.HOURS_NOT_SUPPORTED -> R.string.ds_error_hours_not_supported
    AppErrorMessage.INVALID_HOURS -> R.string.ds_error_invalid_hours
    AppErrorMessage.PHOTOS_NOT_SUPPORTED -> R.string.ds_error_photos_not_supported
    AppErrorMessage.TEMPORARY_CLOSURE_NOT_SUPPORTED -> R.string.ds_error_closure_not_supported
    AppErrorMessage.EVIDENCE_MAX_FILES -> R.string.ds_error_evidence_max_files
    AppErrorMessage.EVIDENCE_LOCKED_DURING_REVIEW -> R.string.ds_error_evidence_locked
    AppErrorMessage.LAST_OWNER_PROTECTED -> R.string.ds_error_last_owner
    AppErrorMessage.MAINTENANCE -> R.string.ds_error_maintenance
    AppErrorMessage.OFFLINE -> R.string.ds_error_offline
    AppErrorMessage.SESSION_EXPIRED -> R.string.ds_error_session_expired
    AppErrorMessage.CONFLICT -> R.string.ds_error_conflict
    AppErrorMessage.SERVER -> R.string.ds_error_server
    AppErrorMessage.UNEXPECTED -> R.string.ds_error_unexpected
}
