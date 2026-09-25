package com.servacode.directory.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryAvatar
import com.servacode.directory.core.designsystem.DirectoryConfirmDialog
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryMenuDivider
import com.servacode.directory.core.designsystem.DirectoryMenuGroup
import com.servacode.directory.core.designsystem.DirectoryMenuRow
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectorySectionLabel
import com.servacode.directory.core.designsystem.DirectoryTopBar
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
 * The page is read in three passes rather than one: who this is, then what can be done about
 * the account, then the two things that end it. Each group is a card under a label of its own,
 * and what a row knows about itself is written under its title rather than beside it — a value
 * and a heading on one line are two headings, which is how this page used to read.
 */
@Composable
fun AccountScreen(
    onFacilities: () -> Unit,
    onJoinAsOwner: () -> Unit,
    onAccountDeleted: () -> Unit,
    onBack: (() -> Unit)? = null,
    onEditProfile: () -> Unit,
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

    LaunchedEffect(deletion) {
        if (deletion == DeletionUiState.Deleted) onAccountDeleted()
    }

    DirectoryPage(
        topBar = { DirectoryTopBar(title = AccountCopy.TITLE, onBack = onBack) },
        bottomBar = bottomBar,
    ) { padding ->
        when (val value = state) {
            AccountUiState.Loading -> DirectoryLoading(Modifier.padding(padding))
            AccountUiState.SignedOut -> DirectoryLoading(Modifier.padding(padding))
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.screen),
                verticalArrangement = Arrangement.spacedBy(Space.lg),
            ) {
                Identity(
                    name = value.profile.name,
                    phone = value.profile.phone,
                    imageUrl = value.profile.imageUrl,
                    province = value.provinces.firstOrNull { it.id == value.profile.provinceId }?.nameAr,
                )
                value.message?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Everything about the person.
                Section(AccountCopy.SECTION_ACCOUNT) {
                    DirectoryMenuRow(
                        title = AccountCopy.EDIT_PROFILE,
                        onClick = onEditProfile,
                        icon = DirectoryIcons.person,
                        subtitle = AccountCopy.EDIT_PROFILE_HINT,
                    )
                    DirectoryMenuDivider()
                    DirectoryMenuRow(
                        title = AccountCopy.FAVORITES,
                        onClick = onFavorites,
                        icon = DirectoryIcons.star,
                    )
                    DirectoryMenuDivider()
                    DirectoryMenuRow(
                        title = AccountCopy.NOTIFICATIONS,
                        onClick = onNotifications,
                        icon = DirectoryIcons.bell,
                    )
                }

                // One row, not two: the invitation until they have joined, their own after.
                Section(AccountCopy.SECTION_FACILITIES) {
                    if (value.ownsFacility) {
                        DirectoryMenuRow(
                            title = AccountCopy.FACILITIES,
                            onClick = onFacilities,
                            icon = DirectoryIcons.hospital,
                            subtitle = AccountCopy.FACILITIES_HINT,
                        )
                    } else {
                        DirectoryMenuRow(
                            title = AccountCopy.JOIN_AS_OWNER,
                            onClick = onJoinAsOwner,
                            icon = DirectoryIcons.hospital,
                            subtitle = AccountCopy.JOIN_AS_OWNER_HINT,
                        )
                    }
                }

                // Everything about the app, in one place — the password and the switches with it.
                Section(AccountCopy.SECTION_APP) {
                    DirectoryMenuRow(
                        title = AccountCopy.SETTINGS,
                        onClick = onSettings,
                        icon = DirectoryIcons.grid,
                        subtitle = AccountCopy.SETTINGS_HINT,
                    )
                    DirectoryMenuDivider()
                    // Not inside settings: what the platform publishes about itself is read,
                    // not changed, and burying a thing people look for is how it is not found.
                    DirectoryMenuRow(
                        title = AccountCopy.HELP,
                        onClick = onHelp,
                        icon = DirectoryIcons.info,
                        subtitle = AccountCopy.HELP_HINT,
                    )
                }

                // The two that end something, last, apart, and marked.
                DirectoryMenuGroup {
                    DirectoryMenuRow(
                        title = AccountCopy.DELETE,
                        onClick = { confirmDelete = true },
                        icon = DirectoryIcons.close,
                        danger = true,
                        trailing = false,
                    )
                    DirectoryMenuDivider()
                    DirectoryMenuRow(
                        title = AccountCopy.SIGN_OUT,
                        onClick = { confirmSignOut = true },
                        icon = DirectoryIcons.logout,
                        danger = true,
                        trailing = false,
                    )
                }
                (deletion as? DeletionUiState.Error)?.let { failure ->
                    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
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
                Spacer(Modifier.height(Space.lg))
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

/** A label and the card under it: one subject, drawn as one thing. */
@Composable
private fun Section(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        DirectorySectionLabel(label)
        DirectoryMenuGroup(content = content)
    }
}

/**
 * Who this account is: the picture, the name, the number, and where it lives.
 *
 * It is a card of its own rather than three lines floating above the menu, because it is not
 * something to go to — it is the answer to "whose account is this", and the menu below it is
 * what can be done about it. The number is the account's name to the backend, so it is shown
 * as it was proved, in the Latin digits it was sent in.
 */
@Composable
private fun Identity(name: String, phone: String, imageUrl: String?, province: String?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.xl),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Space.lg, horizontal = Space.base),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Radius.pill))
                    .padding(Space.xs),
            ) {
                DirectoryAvatar(imageUrl = imageUrl, size = Sizes.avatar + Space.lg)
            }
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = phone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (province != null) {
                Surface(
                    shape = RoundedCornerShape(Radius.pill),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Text(
                        text = province,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = Space.md, vertical = Space.xs),
                    )
                }
            }
        }
    }
}

/** The words of the account, provisional until product copy is approved. */
object AccountCopy {
    const val TITLE = "الملف الشخصي"
    const val ERROR = "تعذر تحميل الحساب"
    const val EDIT_PROFILE = "المعلومات الشخصية"
    const val FAVORITES = "المفضلة"
    const val NOTIFICATIONS = "الإشعارات"
    const val SETTINGS = "الإعدادات"
    const val FACILITIES = "منشآتي"
    const val JOIN_AS_OWNER = "انضم كصاحب منشأة"
    const val JOIN_AS_OWNER_HINT = "أضف منشأتك"
    const val FACILITIES_HINT = "المواعيد والمناوبة والصور"
    const val EDIT_PROFILE_HINT = "الاسم والعنوان والصورة والمحافظة"
    const val SETTINGS_HINT = "كلمة المرور والإشعارات والأذونات"
    const val HELP = "المساعدة والمعلومات"
    const val HELP_HINT = "عن التطبيق والخصوصية والشروط"
    const val SECTION_ACCOUNT = "حسابي"
    const val SECTION_FACILITIES = "المنشآت"
    const val SECTION_APP = "التطبيق"
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
