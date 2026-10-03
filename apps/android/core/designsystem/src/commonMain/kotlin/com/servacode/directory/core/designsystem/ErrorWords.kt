package com.servacode.directory.core.designsystem

import androidx.compose.runtime.Composable
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppErrorMessage
import com.servacode.directory.core.model.AppErrorMessages
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A refusal in words.
 *
 * The view models keep the error itself in their state rather than a sentence, and the screens
 * read it through this. That is what makes a second language a second `strings.xml`: a sentence
 * built when the request failed would be frozen in the language the phone had at that moment, and
 * a view model has no composition to read a resource in anyway.
 */
@Composable
fun appErrorText(error: AppError): String = stringResource(AppErrorMessages.of(error).resource())

private fun AppErrorMessage.resource(): StringResource = when (this) {
    AppErrorMessage.AUTHENTICATION_FAILED -> Res.string.ds_error_authentication_failed
    AppErrorMessage.AUTHENTICATION_REQUIRED -> Res.string.ds_error_authentication_required
    AppErrorMessage.PERMISSION_DENIED -> Res.string.ds_error_permission_denied
    AppErrorMessage.NOT_FOUND -> Res.string.ds_error_not_found
    AppErrorMessage.THROTTLED -> Res.string.ds_error_throttled
    AppErrorMessage.VALIDATION_ERROR -> Res.string.ds_error_validation
    AppErrorMessage.PROVINCE_REQUIRED -> Res.string.ds_error_province_required
    AppErrorMessage.PHONE_ALREADY_REGISTERED -> Res.string.ds_error_phone_already_registered
    AppErrorMessage.DUTY_NOT_SUPPORTED -> Res.string.ds_error_duty_not_supported
    AppErrorMessage.DUTY_OVERLAP_OR_INVALID -> Res.string.ds_error_duty_overlap
    AppErrorMessage.DUTY_DURING_CLOSURE -> Res.string.ds_error_duty_during_closure
    AppErrorMessage.HOURS_NOT_SUPPORTED -> Res.string.ds_error_hours_not_supported
    AppErrorMessage.INVALID_HOURS -> Res.string.ds_error_invalid_hours
    AppErrorMessage.PHOTOS_NOT_SUPPORTED -> Res.string.ds_error_photos_not_supported
    AppErrorMessage.TEMPORARY_CLOSURE_NOT_SUPPORTED -> Res.string.ds_error_closure_not_supported
    AppErrorMessage.EVIDENCE_MAX_FILES -> Res.string.ds_error_evidence_max_files
    AppErrorMessage.EVIDENCE_LOCKED_DURING_REVIEW -> Res.string.ds_error_evidence_locked
    AppErrorMessage.FACILITY_LOCKED_DURING_REVIEW -> Res.string.ds_error_facility_locked
    AppErrorMessage.OTP_RECIPIENT_INVALID -> Res.string.ds_error_otp_recipient_invalid
    AppErrorMessage.OTP_DELIVERY_UNAVAILABLE -> Res.string.ds_error_otp_unavailable
    AppErrorMessage.LAST_OWNER_PROTECTED -> Res.string.ds_error_last_owner
    AppErrorMessage.MAINTENANCE -> Res.string.ds_error_maintenance
    AppErrorMessage.OFFLINE -> Res.string.ds_error_offline
    AppErrorMessage.SESSION_EXPIRED -> Res.string.ds_error_session_expired
    AppErrorMessage.CONFLICT -> Res.string.ds_error_conflict
    AppErrorMessage.SERVER -> Res.string.ds_error_server
    AppErrorMessage.UNEXPECTED -> Res.string.ds_error_unexpected
}
