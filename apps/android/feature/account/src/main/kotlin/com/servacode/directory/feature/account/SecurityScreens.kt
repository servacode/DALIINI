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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.servacode.directory.core.designsystem.DirectoryAvatar
import com.servacode.directory.core.designsystem.DirectoryChipRow
import com.servacode.directory.core.designsystem.DirectoryFilterChip
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryInlineLoading
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryPasswordField
import com.servacode.directory.core.designsystem.DirectorySecondaryButton
import com.servacode.directory.core.designsystem.DirectoryMenuGroup
import com.servacode.directory.core.designsystem.DirectoryMenuRow
import com.servacode.directory.core.designsystem.Sizes
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectoryTextField
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.SectionHeader
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.network.UploadReader

/**
 * Everything the app knows about the person, in the one place it is edited.
 *
 * Two fields, because two fields are what the backend stores and accepts: a display name and a
 * province. There is no address and no picture — `PATCH /api/v1/account/profile/` takes
 * `displayName` and `provinceId` and nothing else, so neither exists to show.
 *
 * The phone number is shown and not edited. It is the account's identity, and changing it is a
 * different operation with its own verification that the contract does not have; a row that
 * opened a screen which could not do it would be worse than saying so.
 */
@Composable
fun ProfileEditScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    onChangePhone: () -> Unit,
    viewModel: ProfileEditViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel(),
    uploadReader: UploadReader? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val account by accountViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    val context = LocalContext.current
    val reader = uploadReader ?: remember(context) { UploadReader(context.applicationContext) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { chosen -> reader.read(chosen).onSuccess(viewModel::chooseImage) }
    }

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
            ProfilePicture(
                imageUrl = state.imageUrl,
                working = state.savingImage,
                onChoose = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onRemove = viewModel::removeImage,
                modifier = Modifier.padding(top = Space.base),
            )
            DirectoryTextField(
                value = state.displayName,
                onValueChange = viewModel::updateName,
                label = SecurityCopy.NAME,
                leadingIcon = DirectoryIcons.person,
            )
            DirectoryTextField(
                value = state.address,
                onValueChange = viewModel::updateAddress,
                label = SecurityCopy.ADDRESS,
                placeholder = SecurityCopy.ADDRESS_HINT,
                leadingIcon = DirectoryIcons.pin,
                singleLine = false,
                minLines = 2,
            )
            // The one detail that is changed somewhere else, because changing it is not a
            // field: it is drawn as what it is, a way onwards, in the shape the menus use.
            DirectoryMenuGroup {
                DirectoryMenuRow(
                    title = SecurityCopy.PHONE,
                    subtitle = state.phone.ifBlank {
                        (account as? AccountUiState.Content)?.profile?.phone.orEmpty()
                    },
                    onClick = onChangePhone,
                    icon = DirectoryIcons.phone,
                )
            }
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

/** The picture, with the two things that can be done to it. */
@Composable
private fun ProfilePicture(
    imageUrl: String?,
    working: Boolean,
    onChoose: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        DirectoryAvatar(imageUrl = imageUrl, size = Sizes.avatar + Space.lg)
        if (working) {
            DirectoryInlineLoading(SecurityCopy.PICTURE)
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            DirectorySecondaryButton(text = SecurityCopy.CHOOSE_PICTURE, onClick = onChoose)
            if (imageUrl != null) {
                DirectorySecondaryButton(text = SecurityCopy.REMOVE_PICTURE, onClick = onRemove)
            }
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
    const val PHONE = "رقم الهاتف"
    const val ADDRESS = "العنوان"
    const val ADDRESS_HINT = "الحي، وأقرب معلم"
    const val PICTURE = "الصورة الشخصية"
    const val CHOOSE_PICTURE = "اختيار صورة"
    const val REMOVE_PICTURE = "إزالة الصورة"
    const val PHONE_NOTE = "رقم الهاتف هو معرّف حسابك، وتغييره يحتاج تحققًا جديدًا غير متاح بعد."
    const val CHANGE_PASSWORD = "تغيير كلمة المرور"
    const val CURRENT_PASSWORD = "كلمة المرور الحالية"
    const val NEW_PASSWORD = "كلمة المرور الجديدة"
    const val CONFIRM_PASSWORD = "تأكيد كلمة المرور"
    const val MISMATCH = "كلمتا المرور غير متطابقتين"
    const val SESSIONS_NOTE = "بعد التغيير ستُنهى جميع الجلسات، بما فيها هذا الجهاز، وستحتاج " +
        "إلى تسجيل الدخول بكلمة المرور الجديدة."
}
