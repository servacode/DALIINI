package com.servacode.directory.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryAvatar
import com.servacode.directory.core.designsystem.DirectoryConfirmDialog
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryMessageState
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectorySettingRow
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.IconSize
import com.servacode.directory.core.designsystem.Radius
import com.servacode.directory.core.designsystem.Sizes
import com.servacode.directory.core.designsystem.Space

/**
 * Screen 15. Who the user is to this app, and the few things they can do about it.
 *
 * The menu is short on purpose, and each row appears in exactly one place in the app:
 *
 *  - **The province is not a row.** It is one of the person's own details, and it is edited
 *    where the rest of them are. Two places to change one field is two places to disagree.
 *  - **Ratings are not a row.** A rating is left on a facility and changed or removed on that
 *    same facility, which is where the person is looking when they think about it. A list of
 *    one's own ratings is a filing cabinet nobody opens.
 *  - **The password and the app's own switches are not rows.** They are settings, and settings
 *    is one screen. This one used to offer both, and so did that one.
 *  - **Owning is one row, not two.** Someone who has never joined is invited to; someone who
 *    has sees what they have. Never both.
 *
 * Nothing is offered that the account API does not have: the contract stores a display name and
 * a province and nothing else, so there is no address to edit and no picture to upload.
 */
@Composable
fun AccountScreen(
    onFacilities: () -> Unit,
    onJoinAsOwner: () -> Unit,
    onAccountDeleted: () -> Unit,
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onFavorites: () -> Unit,
    onNotifications: () -> Unit,
    onSettings: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deletion by viewModel.deletionState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }

    LaunchedEffect(deletion) {
        if (deletion == DeletionUiState.Deleted) onAccountDeleted()
    }

    DirectoryPage(
        topBar = { DirectoryTopBar(title = AccountCopy.TITLE, onBack = onBack) },
        bottomBar = bottomBar,
    ) { padding ->
        when (val value = state) {
            AccountUiState.Loading -> DirectoryLoading(Modifier.padding(padding))
            AccountUiState.SignedOut -> Column(Modifier.padding(padding)) {
                DirectoryMessageState(
                    icon = DirectoryIcons.person,
                    title = AccountCopy.SIGNED_OUT,
                    body = AccountCopy.SIGNED_OUT_BODY,
                    modifier = Modifier.weight(1f),
                    primaryAction = AccountCopy.SIGN_IN,
                    onPrimaryAction = onSignIn,
                    secondaryAction = AccountCopy.REGISTER,
                    onSecondaryAction = onRegister,
                )
                // The platform's own pages belong to everyone, signed in or not, and they
                // live in settings — so settings is what a signed-out reader is offered.
                DirectorySettingRow(
                    title = AccountCopy.SETTINGS,
                    onClick = onSettings,
                    icon = DirectoryIcons.grid,
                )
            }
            is AccountUiState.Error -> DirectoryErrorState(
                title = AccountCopy.ERROR,
                modifier = Modifier.padding(padding),
                body = value.message,
                onRetry = viewModel::refresh,
            )
            is AccountUiState.Content -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                Identity(
                    name = value.profile.name,
                    phone = value.profile.phone,
                    imageUrl = value.profile.imageUrl,
                )
                value.message?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.sm),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Everything about the person.
                DirectorySettingRow(
                    title = AccountCopy.EDIT_PROFILE,
                    onClick = onEditProfile,
                    icon = DirectoryIcons.person,
                    value = value.provinces.firstOrNull { it.id == value.profile.provinceId }?.nameAr,
                )
                DirectorySettingRow(
                    title = AccountCopy.FAVORITES,
                    onClick = onFavorites,
                    icon = DirectoryIcons.star,
                )
                DirectorySettingRow(
                    title = AccountCopy.NOTIFICATIONS,
                    onClick = onNotifications,
                    icon = DirectoryIcons.bell,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // One row, not two: the invitation until they have joined, their own after.
                if (value.ownsFacility) {
                    DirectorySettingRow(
                        title = AccountCopy.FACILITIES,
                        onClick = onFacilities,
                        icon = DirectoryIcons.hospital,
                    )
                } else {
                    DirectorySettingRow(
                        title = AccountCopy.JOIN_AS_OWNER,
                        onClick = onJoinAsOwner,
                        icon = DirectoryIcons.hospital,
                        value = AccountCopy.JOIN_AS_OWNER_HINT,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Everything about the app, in one place — the password and the switches with it.
                DirectorySettingRow(
                    title = AccountCopy.SETTINGS,
                    onClick = onSettings,
                    icon = DirectoryIcons.grid,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // The two that end something, last and marked.
                DirectorySettingRow(
                    title = AccountCopy.DELETE,
                    onClick = { confirmDelete = true },
                    icon = DirectoryIcons.close,
                    danger = true,
                    trailing = false,
                )
                DirectorySettingRow(
                    title = AccountCopy.SIGN_OUT,
                    onClick = { confirmSignOut = true },
                    icon = DirectoryIcons.logout,
                    danger = true,
                    trailing = false,
                )
                (deletion as? DeletionUiState.Error)?.let { failure ->
                    Column(
                        modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.sm),
                        verticalArrangement = Arrangement.spacedBy(Space.xs),
                    ) {
                        Text(
                            text = AccountCopy.DELETE_FAILED,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = failure.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (confirmSignOut) {
        DirectoryConfirmDialog(
            title = AccountCopy.SIGN_OUT_TITLE,
            body = AccountCopy.SIGN_OUT_BODY,
            confirm = AccountCopy.SIGN_OUT,
            onConfirm = {
                confirmSignOut = false
                viewModel.logout()
            },
            onDismiss = { confirmSignOut = false },
            destructive = true,
        )
    }
    if (confirmDelete) {
        DirectoryConfirmDialog(
            title = AccountCopy.DELETE_TITLE,
            body = AccountCopy.DELETE_BODY,
            confirm = AccountCopy.DELETE_CONFIRM,
            onConfirm = {
                confirmDelete = false
                viewModel.deleteAccount()
            },
            onDismiss = { confirmDelete = false },
            destructive = true,
        )
    }
}

/** The account itself, with its picture when it has one and the person mark when it does not. */
@Composable
private fun Identity(name: String, phone: String, imageUrl: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        DirectoryAvatar(imageUrl = imageUrl, size = Sizes.avatar + Space.lg)
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = phone,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** The words of the account, provisional until product copy is approved. */
object AccountCopy {
    const val TITLE = "الملف الشخصي"
    const val ERROR = "تعذر تحميل الحساب"
    const val SIGNED_OUT = "حسابي"
    const val SIGNED_OUT_BODY = "سجّل الدخول لإدارة معلوماتك ومفضّلتك ومنشآتك."
    const val SIGN_IN = "تسجيل الدخول"
    const val REGISTER = "إنشاء حساب"
    const val EDIT_PROFILE = "المعلومات الشخصية"
    const val FAVORITES = "المفضلة"
    const val NOTIFICATIONS = "الإشعارات"
    const val SETTINGS = "الإعدادات"
    const val FACILITIES = "منشآتي"
    const val JOIN_AS_OWNER = "انضم كصاحب منشأة"
    const val JOIN_AS_OWNER_HINT = "أضف منشأتك"
    const val SIGN_OUT = "تسجيل الخروج"
    const val SIGN_OUT_TITLE = "تسجيل الخروج؟"
    const val SIGN_OUT_BODY = "ستحتاج إلى تسجيل الدخول مرة أخرى لإدارة معلوماتك ومنشآتك."
    const val DELETE = "حذف الحساب"
    const val DELETE_TITLE = "حذف الحساب نهائيًا؟"
    const val DELETE_BODY = "سيتم إلغاء جلساتك وإزالة بيانات الحساب الشخصية. " +
        "إذا كنت المالك الوحيد لمنشأة غير مغلقة، يجب نقل الملكية أو إغلاقها أولًا."
    const val DELETE_CONFIRM = "تأكيد الحذف"
    const val DELETE_FAILED = "تعذر طلب حذف الحساب. تحقق من ملكية المنشآت ثم أعد المحاولة."
}
