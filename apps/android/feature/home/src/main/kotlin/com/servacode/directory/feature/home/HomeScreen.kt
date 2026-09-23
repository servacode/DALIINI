package com.servacode.directory.feature.home

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.AdSlider
import com.servacode.directory.core.designsystem.CategoryCircle
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryChipRow
import com.servacode.directory.core.designsystem.DirectoryFilterChip
import com.servacode.directory.core.designsystem.DirectoryEmptyState
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryOfflineNotice
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectorySearchEntry
import com.servacode.directory.core.designsystem.DirectoryTextButton
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.FacilityRow
import com.servacode.directory.core.designsystem.LoadMoreRow
import com.servacode.directory.core.designsystem.SectionHeader
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.location.FOREGROUND_LOCATION_PERMISSIONS
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.HomeAd
import com.servacode.directory.core.model.HomeSnapshot

/**
 * Screen 04. The app's front door: where the user is, one way to search, the categories the
 * province serves, and the facilities themselves.
 *
 * Nothing here decides what the lists hold or in what order: the backend answers with the
 * sections, and a distance appears on a row exactly when the backend put one there.
 */
@Composable
fun HomeScreen(
    onProvince: () -> Unit,
    onSearch: () -> Unit,
    onCategory: (String) -> Unit,
    onFacility: (String) -> Unit,
    bottomBar: @Composable () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val place by viewModel.place.collectAsStateWithLifecycle()
    val quickFilter by viewModel.quickFilter.collectAsStateWithLifecycle()
    val hasLocation by viewModel.hasLocation.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // The offer to use the location, and only while there is nothing to offer: a device that
    // already allowed it is never asked again from here.
    var offerLocation by rememberSaveable {
        mutableStateOf(
            FOREGROUND_LOCATION_PERMISSIONS.none {
                context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
            },
        )
    }
    val askLocation = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        offerLocation = result.values.none { it }
        // A new answer changes where the app thinks the user is, so both are asked again.
        viewModel.resolvePlace()
        viewModel.refresh()
    }

    // What the header says: the place the platform resolved, else the province the lists are
    // scoped by. The user can still change it, but they are never made to choose it first.
    val province = place?.label
        ?: (state as? HomeUiState.Content)?.snapshot?.province?.nameAr

    DirectoryPage(
        topBar = {
            DirectoryTopBar(
                title = HomeCopy.TITLE,
                subtitle = province,
                onSubtitle = if (province == null) null else onProvince,
                actionIcon = DirectoryIcons.pin,
                actionLabel = HomeCopy.PROVINCE,
                onAction = onProvince,
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        when (val value = state) {
            HomeUiState.Loading -> DirectoryLoading(Modifier.padding(padding))
            HomeUiState.ProvinceRequired -> DirectoryEmptyState(
                title = HomeCopy.PROVINCE_REQUIRED,
                modifier = Modifier.padding(padding),
                body = HomeCopy.PROVINCE_REQUIRED_BODY,
                icon = DirectoryIcons.pin,
                action = HomeCopy.PROVINCE_CHOOSE,
                onAction = onProvince,
            )
            is HomeUiState.Error -> DirectoryErrorState(
                title = HomeCopy.ERROR,
                modifier = Modifier.padding(padding),
                body = value.message,
                onRetry = viewModel::refresh,
            )
            is HomeUiState.Content -> HomeContent(
                value = value,
                padding = padding,
                offerLocation = offerLocation,
                quickFilter = quickFilter,
                hasLocation = hasLocation,
                onQuickFilter = viewModel::select,
                onLoadMore = viewModel::loadMore,
                onUseLocation = { askLocation.launch(FOREGROUND_LOCATION_PERMISSIONS.toTypedArray()) },
                onRefresh = viewModel::refresh,
                onSearch = onSearch,
                onCategory = onCategory,
                onFacility = onFacility,
            )
        }
    }
}

@Composable
private fun HomeContent(
    value: HomeUiState.Content,
    padding: PaddingValues,
    offerLocation: Boolean,
    quickFilter: QuickFilterState?,
    hasLocation: Boolean,
    onQuickFilter: (HomeQuickFilter?) -> Unit,
    onLoadMore: () -> Unit,
    onUseLocation: () -> Unit,
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onCategory: (String) -> Unit,
    onFacility: (String) -> Unit,
) {
    val snapshot = value.snapshot
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(bottom = Space.xxl),
    ) {
        item(key = "search") {
            DirectorySearchEntry(
                placeholder = HomeCopy.SEARCH,
                onClick = onSearch,
                modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.sm),
            )
        }
        if (value.stale) {
            item(key = "stale") {
                DirectoryOfflineNotice(
                    modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.xs),
                    onRetry = onRefresh,
                )
            }
        }
        if (offerLocation) {
            item(key = "location") {
                LocationOffer(
                    onUseLocation = onUseLocation,
                    modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.xs),
                )
            }
        }
        if (snapshot.ads.isNotEmpty()) {
            item(key = "ads") {
                AdSlider(
                    ads = snapshot.ads,
                    onAd = { ad -> ad.destination()?.let(onFacility) },
                    modifier = Modifier.padding(vertical = Space.sm),
                )
            }
        }
        item(key = "quick-filters") {
            QuickFilterRow(
                selected = quickFilter?.filter,
                hasLocation = hasLocation,
                onSelect = onQuickFilter,
            )
        }
        if (quickFilter != null) {
            quickFilterBody(quickFilter, onFacility, onLoadMore)
            return@LazyColumn
        }
        if (snapshot.categories.isNotEmpty()) {
            item(key = "categories") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Space.screen),
                    horizontalArrangement = Arrangement.spacedBy(Space.xs),
                    modifier = Modifier.padding(vertical = Space.sm),
                ) {
                    items(snapshot.categories, key = { it.id }) { category ->
                        CategoryCircle(
                            label = category.nameAr,
                            icon = DirectoryIcons.category(category.iconKey),
                            selected = false,
                            onClick = { onCategory(category.id) },
                        )
                    }
                }
            }
        }
        facilitySection(HomeCopy.DUTY, snapshot.dutyNow, "duty", onFacility)
        facilitySection(HomeCopy.OPEN, snapshot.openNearby, "open", onFacility)
        facilitySection(HomeHeadings.nearby(snapshot.nearby), snapshot.nearby, "near", onFacility)
        if (snapshot.isEmpty()) {
            item(key = "empty") {
                DirectoryEmptyState(
                    title = HomeCopy.EMPTY,
                    body = HomeCopy.EMPTY_BODY,
                    modifier = Modifier.padding(top = Space.xxl),
                )
            }
        }
    }
}

/** Nothing the backend returned for this province is worth a list. */
private fun HomeSnapshot.isEmpty(): Boolean =
    nearby.isEmpty() && openNearby.isEmpty() && dutyNow.isEmpty()

/**
 * One titled run of facilities. An empty list writes nothing at all: a heading over no rows
 * reads as a failure, and these sections are simply absent when the backend sends none.
 */
private fun LazyListScope.facilitySection(
    title: String,
    facilities: List<FacilitySummary>,
    keyPrefix: String,
    onFacility: (String) -> Unit,
) {
    if (facilities.isEmpty()) return
    item(key = "$keyPrefix-header") {
        SectionHeader(
            title = title,
            modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.xs),
        )
    }
    itemsIndexed(facilities, key = { _, facility -> "$keyPrefix-${facility.id}" }) { index, facility ->
        Column {
            FacilityRow(facility = facility, onClick = { onFacility(facility.id) })
            if (index < facilities.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Space.screen),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

/**
 * The one place the app asks for the location outside the first run, and it asks by explaining
 * what it would add. Refusing costs the distances and the nearest-first order, nothing else.
 */
@Composable
private fun LocationOffer(onUseLocation: () -> Unit, modifier: Modifier = Modifier) {
    DirectoryCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            DirectoryIcon(DirectoryIcons.pin, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(
                    text = HomeCopy.LOCATION_TITLE,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = HomeCopy.LOCATION_BODY,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DirectoryTextButton(HomeCopy.LOCATION_ACTION, onUseLocation)
        }
    }
}

/**
 * The three questions people arrive with. A chosen chip replaces the sections below with the
 * backend's own answer to that question; choosing it again puts the sections back.
 */
@Composable
private fun QuickFilterRow(
    selected: HomeQuickFilter?,
    hasLocation: Boolean,
    onSelect: (HomeQuickFilter?) -> Unit,
) {
    DirectoryChipRow(
        modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.xs),
    ) {
        HomeQuickFilter.entries.forEach { filter ->
            // Nearest is not offered without a position: it would promise an order the app
            // cannot produce, and the backend is the only thing that computes distance.
            if (!filter.isAvailable(hasLocation)) return@forEach
            DirectoryFilterChip(
                text = HomeCopy.quickFilter(filter),
                selected = filter == selected,
                onClick = { onSelect(if (filter == selected) null else filter) },
            )
        }
    }
}

/** What one chip is showing: the backend's list, its own loading, and its own way to go on. */
private fun LazyListScope.quickFilterBody(
    state: QuickFilterState,
    onFacility: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    if (state.loading) {
        item(key = "filter-loading") { DirectoryLoading(Modifier.padding(top = Space.xxl)) }
        return
    }
    if (state.error != null && state.items.isEmpty()) {
        item(key = "filter-error") {
            DirectoryErrorState(
                title = HomeCopy.ERROR,
                body = state.error,
                modifier = Modifier.padding(top = Space.xxl),
            )
        }
        return
    }
    if (state.items.isEmpty()) {
        item(key = "filter-empty") {
            DirectoryEmptyState(
                title = HomeCopy.quickFilterEmpty(state.filter),
                body = HomeCopy.EMPTY_BODY,
                modifier = Modifier.padding(top = Space.xxl),
            )
        }
        return
    }
    itemsIndexed(state.items, key = { _, facility -> "filter-${facility.id}" }) { index, facility ->
        Column {
            FacilityRow(facility = facility, onClick = { onFacility(facility.id) })
            if (index < state.items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Space.screen),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
    if (state.hasMore || state.error != null) {
        item(key = "filter-more") {
            LoadMoreRow(loading = state.loadingMore, onLoadMore = onLoadMore, error = state.error)
        }
    }
}

/**
 * Where an advertisement leads. Only a facility is followed: it is the one destination this app
 * can honour without leaving itself, and the backend already restricts what an ad may carry.
 */
private fun HomeAd.destination(): String? = facilityId

/** The words of Home, in one place, provisional until product copy is approved. */
object HomeCopy {
    const val TITLE = "الدليل"
    const val PROVINCE = "المحافظة"
    const val SEARCH = "ابحث عن منشأة أو تخصص..."
    const val DUTY = "المناوبون الآن"
    const val OPEN = "مفتوح الآن"
    const val ERROR = "حدث خطأ"
    const val EMPTY = "لا توجد منشآت"
    const val EMPTY_BODY = "لم يتم العثور على منشآت في هذه المحافظة بعد."
    const val PROVINCE_REQUIRED = "اختر المحافظة للبدء"
    const val PROVINCE_REQUIRED_BODY = "يعرض الدليل المنشآت داخل المحافظة التي تختارها."
    const val PROVINCE_CHOOSE = "اختيار المحافظة"
    const val LOCATION_TITLE = "الترتيب بالأقرب إليك"
    const val LOCATION_BODY = "فعّل الموقع لعرض المسافة وترتيب المنشآت بالأقرب."
    const val LOCATION_ACTION = "تفعيل"

    fun quickFilter(filter: HomeQuickFilter): String = when (filter) {
        HomeQuickFilter.NEAREST -> "الأقرب إليك"
        HomeQuickFilter.OPEN_NOW -> "مفتوح الآن"
        HomeQuickFilter.DUTY_NOW -> "مناوب الآن"
    }

    fun quickFilterEmpty(filter: HomeQuickFilter): String = when (filter) {
        HomeQuickFilter.NEAREST -> "لا توجد منشآت قريبة"
        HomeQuickFilter.OPEN_NOW -> "لا توجد منشأة مفتوحة الآن"
        HomeQuickFilter.DUTY_NOW -> "لا توجد منشأة مناوبة الآن"
    }
}
