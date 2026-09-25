package com.servacode.directory.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectorySecondaryButton
import com.servacode.directory.core.designsystem.DirectoryTextField
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.Space

/**
 * Changing the number the account signs in with.
 *
 * One screen in two states: asking for the new number, then asking for the code that was sent to
 * it. Nothing about the account changes until the code is proved, and proving it ends every
 * session — so the screen says that before the person begins, not after it has happened to them.
 */
@Composable
fun PhoneChangeScreen(
    onChanged: () -> Unit,
    onBack: () -> Unit,
    viewModel: PhoneChangeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.changed) { if (state.changed) onChanged() }

    DirectoryPage(
        topBar = { DirectoryTopBar(title = PhoneChangeCopy.TITLE, onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Space.screen)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            if (state.challengeId == null) {
                DirectoryTextField(
                    value = state.phone,
                    onValueChange = viewModel::updatePhone,
                    label = PhoneChangeCopy.NEW_PHONE,
                    leadingIcon = DirectoryIcons.phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.padding(top = Space.base),
                )
                Text(
                    text = PhoneChangeCopy.SESSIONS_NOTE,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.error?.let { ErrorLine(it) }
                DirectoryPrimaryButton(
                    text = PhoneChangeCopy.SEND,
                    onClick = viewModel::send,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canSend,
                    loading = state.working,
                )
            } else {
                Text(
                    text = PhoneChangeCopy.sentTo(state.phone),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = Space.base),
                )
                DirectoryTextField(
                    value = state.code,
                    onValueChange = viewModel::updateCode,
                    label = PhoneChangeCopy.CODE,
                    leadingIcon = DirectoryIcons.verified,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                state.error?.let { ErrorLine(it) }
                DirectoryPrimaryButton(
                    text = PhoneChangeCopy.CONFIRM,
                    onClick = viewModel::confirm,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canConfirm,
                    loading = state.working,
                )
                DirectorySecondaryButton(
                    text = PhoneChangeCopy.ANOTHER_NUMBER,
                    onClick = viewModel::startOver,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ErrorLine(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}

/** The words of the phone change, provisional until product copy is approved. */
object PhoneChangeCopy {
    val TITLE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.phone_change_title)
    val NEW_PHONE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.phone_change_new_phone)
    val CODE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.phone_change_code)
    val SEND: String @Composable @ReadOnlyComposable get() = stringResource(R.string.phone_change_send)
    val CONFIRM: String @Composable @ReadOnlyComposable get() = stringResource(R.string.phone_change_confirm)
    val ANOTHER_NUMBER: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.phone_change_another_number)
    val SESSIONS_NOTE: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.phone_change_sessions_note)


    @Composable
    @ReadOnlyComposable
    fun sentTo(phone: String): String = stringResource(R.string.phone_change_sent_to, phone)
}
