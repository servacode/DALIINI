package com.servacode.directory.feature.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.servacode.directory.core.model.Province

/**
 * Screen 15. Who the user is to this app, and the few things they can do about it.
 *
 * Nothing is offered here that the account API does not have: there is no avatar to upload
 * (INT-017), no saved facilities and no notification inbox, so no such rows are drawn.
 */
@Composable
fun AccountScreen(
    onRatings: () -> Unit,
    onFacilities: () -> Unit,
    onAccountDeleted: () -> Unit,
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onFavorites: () -> Unit,
    onNotifications: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deletion by viewModel.deletionState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    var provincesOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = provincesOpen) { provincesOpen = false }

    LaunchedEffect(deletion) {
        if (deletion == DeletionUiState.Deleted) onAccountDeleted()
    }

    val content = state as? AccountUiState.Content
    if (provincesOpen && content != null) {
        ProvincesPage(
            provinces = content.provinces,
            selectedId = content.profile.provinceId,
            onSelect = {
                viewModel.changeProvince(it)
                provincesOpen = false
            },
            onBack = { provincesOpen = false },
        )
        return
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
                // The platform's own pages belong to everyone, signed in or not.
                DirectorySettingRow(
                    title = AccountCopy.HELP,
                    onClick = onHelp,
                    icon = DirectoryIcons.info,
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
                Identity(name = value.profile.name, phone = value.profile.phone)
                value.message?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.sm),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DirectorySettingRow(
                    title = AccountCopy.EDIT_PROFILE,
                    onClick = onEditProfile,
                    icon = DirectoryIcons.person,
                )
                DirectorySettingRow(
                    title = AccountCopy.PROVINCE,
                    onClick = { provincesOpen = true },
                    value = value.provinces.firstOrNull { it.id == value.profile.provinceId }?.nameAr,
                    icon = DirectoryIcons.pin,
                )
                DirectorySettingRow(
                    title = AccountCopy.RATINGS,
                    onClick = onRatings,
                    icon = DirectoryIcons.star,
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
                DirectorySettingRow(
                    title = AccountCopy.FACILITIES,
                    onClick = onFacilities,
                    icon = DirectoryIcons.hospital,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DirectorySettingRow(
                    title = AccountCopy.SECURITY,
                    onClick = onChangePassword,
                    icon = DirectoryIcons.verified,
                )
                DirectorySettingRow(
                    title = AccountCopy.SETTINGS,
                    onClick = onSettings,
                    icon = DirectoryIcons.grid,
                )
                DirectorySettingRow(
                    title = AccountCopy.HELP,
                    onClick = onHelp,
                    icon = DirectoryIcons.info,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DirectorySettingRow(
                    title = AccountCopy.SIGN_OUT,
                    onClick = { confirmSignOut = true },
                    icon = DirectoryIcons.logout,
                    danger = true,
                    trailing = false,
                )
                DirectorySettingRow(
                    title = AccountCopy.DELETE,
                    onClick = { confirmDelete = true },
                    icon = DirectoryIcons.close,
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

/** The account itself: the app has no picture for a user, so it draws the person mark. */
@Composable
private fun Identity(name: String, phone: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.avatar + Space.lg)
                .clip(RoundedCornerShape(Radius.pill))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            DirectoryIcon(
                icon = DirectoryIcons.person,
                contentDescription = null,
                size = IconSize.large,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
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

/** The account's own province, which is the backend's, not the device's browsing choice. */
@Composable
private fun ProvincesPage(
    provinces: List<Province>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
) {
    DirectoryPage(
        topBar = { DirectoryTopBar(title = AccountCopy.PROVINCE, onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = Space.xxl),
        ) {
            items(provinces, key = { it.id }) { province ->
                DirectorySettingRow(
                    title = province.nameAr,
                    onClick = { onSelect(province.id) },
                    icon = if (province.id == selectedId) DirectoryIcons.check else DirectoryIcons.pin,
                    trailing = false,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Space.screen),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

/** The words of the account, provisional until product copy is approved. */
object AccountCopy {
    const val TITLE = "الملف الشخصي"
    const val ERROR = "تعذر تحميل الحساب"
    const val SIGNED_OUT = "حسابي"
    const val SIGNED_OUT_BODY = "سجّل الدخول لإدارة تقييماتك ومنشآتك."
    const val SIGN_IN = "تسجيل الدخول"
    const val REGISTER = "إنشاء حساب"
    const val EDIT_PROFILE = "المعلومات الشخصية"
    const val PROVINCE = "المحافظة"
    const val FAVORITES = "المفضلة"
    const val NOTIFICATIONS = "الإشعارات"
    const val SECURITY = "تغيير كلمة المرور"
    const val SETTINGS = "الإعدادات"
    const val HELP = "المساعدة والمعلومات"
    const val RATINGS = "تقييماتي"
    const val FACILITIES = "منشآتي"
    const val SIGN_OUT = "تسجيل الخروج"
    const val SIGN_OUT_TITLE = "تسجيل الخروج؟"
    const val SIGN_OUT_BODY = "ستحتاج إلى تسجيل الدخول مرة أخرى لإدارة تقييماتك ومنشآتك."
    const val DELETE = "حذف الحساب"
    const val DELETE_TITLE = "حذف الحساب نهائيًا؟"
    const val DELETE_BODY = "سيتم إلغاء جلساتك وإزالة بيانات الحساب الشخصية. " +
        "إذا كنت المالك الوحيد لمنشأة غير مغلقة، يجب نقل الملكية أو إغلاقها أولًا."
    const val DELETE_CONFIRM = "تأكيد الحذف"
    const val DELETE_FAILED = "تعذر طلب حذف الحساب. تحقق من ملكية المنشآت ثم أعد المحاولة."
}
