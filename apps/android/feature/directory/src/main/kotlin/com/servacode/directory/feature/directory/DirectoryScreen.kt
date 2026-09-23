package com.servacode.directory.feature.directory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryEmptyState
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryOfflineNotice
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectorySettingRow
import com.servacode.directory.core.designsystem.DirectorySwitchRow
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.FacilityRow
import com.servacode.directory.core.designsystem.LoadMoreRow
import com.servacode.directory.core.designsystem.Space

/**
 * Screen 04's list for one category, and Screen 07's filters over it.
 *
 * The filters are the ones the backend takes and no others: open now, on duty now, and the
 * province the whole app is set to. Nothing is filtered on the device.
 */
@Composable
fun DirectoryScreen(
    onFacility: (String) -> Unit,
    onProvince: () -> Unit,
    onBack: () -> Unit,
    viewModel: DirectoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var filtersOpen by remember { mutableStateOf(false) }
    val content = state as? DirectoryUiState.Content
    BackHandler(enabled = filtersOpen) { filtersOpen = false }

    if (filtersOpen && content != null) {
        FiltersPage(
            filter = content.filter,
            onOpenNow = viewModel::setOpenNow,
            onDutyNow = viewModel::setDutyNow,
            onProvince = onProvince,
            onReset = {
                viewModel.setOpenNow(false)
                viewModel.setDutyNow(false)
            },
            onClose = { filtersOpen = false },
        )
        return
    }

    DirectoryPage(
        topBar = {
            DirectoryTopBar(
                title = content?.values?.firstOrNull()?.category?.nameAr ?: DirectoryCopy.TITLE,
                onBack = onBack,
                actionIcon = if (content == null) null else DirectoryIcons.filter,
                actionLabel = DirectoryCopy.FILTERS,
                onAction = if (content == null) null else ({ filtersOpen = true }),
            )
        },
    ) { padding ->
        when (val value = state) {
            DirectoryUiState.Loading -> DirectoryLoading(Modifier.padding(padding))
            DirectoryUiState.ProvinceRequired -> DirectoryEmptyState(
                title = DirectoryCopy.PROVINCE_REQUIRED,
                modifier = Modifier.padding(padding),
                icon = DirectoryIcons.pin,
                action = DirectoryCopy.PROVINCE_CHOOSE,
                onAction = onProvince,
            )
            is DirectoryUiState.Error -> DirectoryErrorState(
                title = DirectoryCopy.ERROR,
                modifier = Modifier.padding(padding),
                body = value.message,
                onRetry = viewModel::refresh,
            )
            is DirectoryUiState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = Space.xxl),
            ) {
                if (value.stale) {
                    item(key = "stale") {
                        DirectoryOfflineNotice(
                            modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.sm),
                            onRetry = viewModel::refresh,
                        )
                    }
                }
                if (value.filter.openNow || value.filter.dutyNow) {
                    item(key = "applied") {
                        Text(
                            text = appliedFilters(value.filter),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.sm),
                        )
                    }
                }
                if (value.values.isEmpty()) {
                    item(key = "empty") {
                        DirectoryEmptyState(
                            title = DirectoryCopy.EMPTY,
                            body = DirectoryCopy.EMPTY_BODY,
                            modifier = Modifier.padding(top = Space.xxl),
                        )
                    }
                }
                itemsIndexed(value.values, key = { _, facility -> facility.id }) { index, facility ->
                    Column {
                        FacilityRow(facility = facility, onClick = { onFacility(facility.id) })
                        if (index < value.values.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = Space.screen),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
                if (value.hasMore || value.moreError != null) {
                    item(key = "more") {
                        LoadMoreRow(
                            loading = value.loadingMore,
                            onLoadMore = viewModel::loadMore,
                            error = value.moreError,
                        )
                    }
                }
            }
        }
    }
}

/** Screen 07. Only what the backend filters on, so nothing here promises more than it does. */
@Composable
private fun FiltersPage(
    filter: DirectoryFilter,
    onOpenNow: (Boolean) -> Unit,
    onDutyNow: (Boolean) -> Unit,
    onProvince: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
) {
    DirectoryPage(
        topBar = {
            DirectoryTopBar(
                title = DirectoryCopy.FILTERS,
                onBack = onClose,
                actionIcon = DirectoryIcons.refresh,
                actionLabel = DirectoryCopy.RESET,
                onAction = onReset,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            DirectorySwitchRow(
                title = DirectoryCopy.OPEN_NOW,
                checked = filter.openNow,
                onCheckedChange = onOpenNow,
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Space.screen),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            DirectorySwitchRow(
                title = DirectoryCopy.DUTY_NOW,
                checked = filter.dutyNow,
                onCheckedChange = onDutyNow,
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Space.screen),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            DirectorySettingRow(
                title = DirectoryCopy.PROVINCE,
                onClick = onProvince,
                icon = DirectoryIcons.pin,
            )
        }
    }
}

/** What the list is being narrowed by, said once above it. */
private fun appliedFilters(filter: DirectoryFilter): String = listOfNotNull(
    DirectoryCopy.OPEN_NOW.takeIf { filter.openNow },
    DirectoryCopy.DUTY_NOW.takeIf { filter.dutyNow },
).joinToString(" • ")

/** The words of the list and its filters, provisional until product copy is approved. */
object DirectoryCopy {
    const val TITLE = "المنشآت"
    const val FILTERS = "الفلاتر"
    const val RESET = "إعادة ضبط"
    const val OPEN_NOW = "مفتوح الآن"
    const val DUTY_NOW = "مناوب الآن"
    const val PROVINCE = "المحافظة"
    const val ERROR = "تعذر تحميل المنشآت"
    const val EMPTY = "لا توجد منشآت"
    const val EMPTY_BODY = "لا توجد منشآت تطابق ما اخترته."
    const val PROVINCE_REQUIRED = "اختر المحافظة أولًا"
    const val PROVINCE_CHOOSE = "اختيار المحافظة"
}
