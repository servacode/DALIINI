package com.servacode.directory.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryChipRow
import com.servacode.directory.core.designsystem.DirectoryFilterChip
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryPasswordField
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectoryTextField
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.SectionHeader
import com.servacode.directory.core.designsystem.Space

/**
 * Editing the account.
 *
 * Two fields, because two fields are what the backend stores and accepts. The phone number is
 * the account's identity and is not edited here; changing it would be a different operation
 * with its own verification, and the API has none.
 */
@Composable
fun ProfileEditScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileEditViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val account by accountViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    DirectoryPage(
        topBar = { DirectoryTopBar(title = SecurityCopy.EDIT_PROFILE, onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Space.screen)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            DirectoryTextField(
                value = state.displayName,
                onValueChange = viewModel::updateName,
                label = SecurityCopy.NAME,
                leadingIcon = DirectoryIcons.person,
                modifier = Modifier.padding(top = Space.base),
            )
            val provinces = (account as? AccountUiState.Content)?.provinces.orEmpty()
            if (provinces.isNotEmpty()) {
                SectionHeader(SecurityCopy.PROVINCE)
                DirectoryChipRow {
                    provinces.forEach { province ->
                        DirectoryFilterChip(
                            text = province.nameAr,
                            selected = province.id == state.provinceId,
                            onClick = { viewModel.chooseProvince(province.id) },
                        )
                    }
                }
            }
            state.error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            DirectoryPrimaryButton(
                text = SecurityCopy.SAVE,
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.displayName.isNotBlank(),
                loading = state.saving,
            )
            Text(
                text = SecurityCopy.PHONE_NOTE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Space.xxl),
            )
        }
    }
}

/**
 * Changing the password.
 *
 * A success ends every session, this device's included, so the screen says so plainly and the
 * app returns to signing in rather than pretending the user is still where they were.
 */
@Composable
fun PasswordChangeScreen(
    onChanged: () -> Unit,
    onBack: () -> Unit,
    viewModel: PasswordChangeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.changed) { if (state.changed) onChanged() }

    DirectoryPage(
        topBar = { DirectoryTopBar(title = SecurityCopy.CHANGE_PASSWORD, onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Space.screen)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Text(
                text = SecurityCopy.SESSIONS_NOTE,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.base),
            )
            DirectoryPasswordField(
                value = state.current,
                onValueChange = viewModel::updateCurrent,
                label = SecurityCopy.CURRENT_PASSWORD,
            )
            DirectoryPasswordField(
                value = state.next,
                onValueChange = viewModel::updateNext,
                label = SecurityCopy.NEW_PASSWORD,
            )
            DirectoryPasswordField(
                value = state.confirmation,
                onValueChange = viewModel::updateConfirmation,
                label = SecurityCopy.CONFIRM_PASSWORD,
                error = if (state.mismatch) SecurityCopy.MISMATCH else null,
            )
            state.error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            DirectoryPrimaryButton(
                text = SecurityCopy.CHANGE_PASSWORD,
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth().padding(bottom = Space.xxl),
                enabled = state.canSubmit,
                loading = state.saving,
            )
        }
    }
}

/** The words of the account's own screens, provisional until product copy is approved. */
object SecurityCopy {
    const val EDIT_PROFILE = "المعلومات الشخصية"
    const val NAME = "الاسم"
    const val PROVINCE = "المحافظة"
    const val SAVE = "حفظ"
    const val PHONE_NOTE = "رقم الهاتف هو معرّف حسابك ولا يُعدَّل من هنا."
    const val CHANGE_PASSWORD = "تغيير كلمة المرور"
    const val CURRENT_PASSWORD = "كلمة المرور الحالية"
    const val NEW_PASSWORD = "كلمة المرور الجديدة"
    const val CONFIRM_PASSWORD = "تأكيد كلمة المرور"
    const val MISMATCH = "كلمتا المرور غير متطابقتين"
    const val SESSIONS_NOTE = "بعد التغيير ستُنهى جميع الجلسات، بما فيها هذا الجهاز، وستحتاج " +
        "إلى تسجيل الدخول بكلمة المرور الجديدة."
}
