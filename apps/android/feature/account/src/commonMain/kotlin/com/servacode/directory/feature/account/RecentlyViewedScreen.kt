package com.servacode.directory.feature.account

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryEmptyState
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryMenuDivider
import com.servacode.directory.core.designsystem.DirectoryMenuGroup
import com.servacode.directory.core.designsystem.DirectoryMenuRow
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.Space
import org.jetbrains.compose.resources.stringResource

/**
 * «شوهدت مؤخراً»: the facilities opened on this device, newest first, and a way to forget them.
 *
 * They are kept on the device only; clearing them here is all there is to clear.
 */
@Composable
fun RecentlyViewedScreen(
    viewModel: RecentlyViewedViewModel,
    onFacility: (String) -> Unit,
    onBack: () -> Unit,
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    DirectoryPage(
        topBar = {
            DirectoryTopBar(
                title = RecentCopy.TITLE,
                onBack = onBack,
                actionIcon = if (items.isNullOrEmpty()) null else DirectoryIcons.trash,
                actionLabel = RecentCopy.CLEAR,
                onAction = if (items.isNullOrEmpty()) null else viewModel::clear,
            )
        },
    ) { padding ->
        val value = items
        when {
            value == null -> DirectoryLoading(Modifier.padding(padding))
            value.isEmpty() -> DirectoryEmptyState(
                title = RecentCopy.EMPTY,
                body = RecentCopy.EMPTY_BODY,
                modifier = Modifier.padding(padding),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.screen, vertical = Space.base),
            ) {
                DirectoryMenuGroup {
                    value.forEachIndexed { index, facility ->
                        if (index > 0) DirectoryMenuDivider()
                        DirectoryMenuRow(
                            title = facility.nameAr,
                            subtitle = facility.categoryNameAr,
                            onClick = { onFacility(facility.id) },
                            icon = DirectoryIcons.history,
                        )
                    }
                }
            }
        }
    }
}

object RecentCopy {
    val TITLE: String @Composable get() = stringResource(Res.string.recent_title)
    val CLEAR: String @Composable get() = stringResource(Res.string.recent_clear)
    val EMPTY: String @Composable get() = stringResource(Res.string.recent_empty)
    val EMPTY_BODY: String @Composable get() = stringResource(Res.string.recent_empty_body)
}
