package com.servacode.directory.feature.home

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.AdSlider
import com.servacode.directory.core.designsystem.BrandColors
import com.servacode.directory.core.designsystem.CategoryCircle
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryEmptyState
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryFilterChip
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryIconButton
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryOfflineNotice
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectorySearchEntry
import com.servacode.directory.core.designsystem.DirectoryTextButton
import com.servacode.directory.core.designsystem.FacilityRow
import com.servacode.directory.core.designsystem.IconSize
import com.servacode.directory.core.designsystem.LoadMoreRow
import com.servacode.directory.core.designsystem.Radius
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.location.FOREGROUND_LOCATION_PERMISSIONS
import com.servacode.directory.core.model.Category

/**
 * Screen 04. The app's front door.
 *
 * Read from the top it answers, in order, the questions someone opens a health directory with:
 * where am I, what am I looking for, what kinds of place are there, what is nearest, what is
 * open, and who is on duty tonight. None of it needs a second screen.
 *
 * Nothing here decides what the list holds or in what order. Every chip is a flag on the
 * backend's own directory query, so the interface cannot drift from what the server means by
 * open or by on duty.
 */
@Composable
fun HomeScreen(
    onProvince: () -> Unit,
    onSearch: () -> Unit,
    onFacility: (String) -> Unit,
    onNotifications: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val place by viewModel.place.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val list by viewModel.list.collectAsStateWithLifecycle()
    val hasLocation by viewModel.hasLocation.collectAsStateWithLifecycle()
    val unread by viewModel.unread.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // The offer to use the location, and only while there is something to offer: a device that
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

    // What the header says: the place the platform resolved for this device, else the province
    // the lists are scoped by. The user can still change it, but is never made to choose first.
    val here = place?.label ?: (state as? HomeUiState.Content)?.snapshot?.province?.nameAr

    DirectoryPage(
        topBar = {
            HomeHeader(
                place = here,
                unread = unread,
                onPlace = onProvince,
                onNotifications = onNotifications,
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
                filters = filters,
                category = category,
                list = list,
                hasLocation = hasLocation,
                onChip = viewModel::toggle,
                onCategory = viewModel::select,
                onLoadMore = viewModel::loadMore,
                onUseLocation = { askLocation.launch(FOREGROUND_LOCATION_PERMISSIONS.toTypedArray()) },
                onRefresh = viewModel::refresh,
                onSearch = onSearch,
                onFacility = onFacility,
            )
        }
    }
}

/**
 * Where the user is, what the app is called, and what is waiting for them.
 *
 * A box rather than a row of three, because the name has to sit in the middle of the screen
 * rather than in the middle of whatever is left over: a long place name on one side would
 * otherwise push it off centre. Start and end place the other two, which mirrors itself for
 * Arabic without naming a side.
 */
@Composable
private fun HomeHeader(
    place: String?,
    unread: Int,
    onPlace: () -> Unit,
    onNotifications: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.base, vertical = Space.md),
        ) {
            HerePlace(
                place = place,
                onClick = onPlace,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Text(
                text = HomeCopy.TITLE,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.align(Alignment.Center),
            )
            NotificationBell(
                unread = unread,
                onClick = onNotifications,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

/**
 * "You are now in" and the place itself.
 *
 * The place is the information, so it is the line that is emphasised; the label above it only
 * says what the line means. A pin sits beside it because the two together read as a location
 * faster than either does alone — but the name is never replaced by the pin.
 */
@Composable
private fun HerePlace(place: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            // Capped so a long "province — neighbourhood" ellipsises instead of pushing the
            // app's name off the middle of the screen.
            .fillMaxWidth(PLACE_WIDTH_FRACTION)
            .clickable(onClick = onClick)
            .padding(vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        DirectoryIcon(
            icon = DirectoryIcons.pin,
            contentDescription = null,
            size = IconSize.small,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column {
            Text(
                text = HomeCopy.YOU_ARE_IN,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            // The place is the information, so it is the line that is emphasised; the label
            // above only says what it means. One line, always: the header must not grow a row
            // taller because the platform happens to know a neighbourhood here and not there.
            Text(
                text = place ?: HomeCopy.PROVINCE_CHOOSE,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** About a third of the width, which leaves the centred name its middle and the bell its end. */
private const val PLACE_WIDTH_FRACTION = 0.34f

/**
 * The bell, and a count only when there is one.
 *
 * Nothing is drawn for an empty inbox: a badge showing zero is a badge that has stopped meaning
 * anything, and a signed-out reader has no inbox to count at all.
 */
@Composable
private fun NotificationBell(unread: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        DirectoryIconButton(
            icon = DirectoryIcons.bell,
            label = HomeCopy.NOTIFICATIONS,
            onClick = onClick,
        )
        if (unread > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(Space.lg)
                    .background(BrandColors.danger, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (unread > 99) HomeCopy.MANY else unread.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    value: HomeUiState.Content,
    padding: PaddingValues,
    offerLocation: Boolean,
    filters: HomeFilters,
    category: Category?,
    list: HomeListState,
    hasLocation: Boolean,
    onChip: (HomeChip) -> Unit,
    onCategory: (Category) -> Unit,
    onLoadMore: () -> Unit,
    onUseLocation: () -> Unit,
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onFacility: (String) -> Unit,
) {
    val snapshot = value.snapshot
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(bottom = Space.xxl),
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        if (value.stale) {
            item(key = "stale") {
                DirectoryOfflineNotice(
                    modifier = Modifier.padding(horizontal = Space.base),
                    onRetry = onRefresh,
                )
            }
        }
        if (snapshot.ads.isNotEmpty()) {
            item(key = "ads") {
                AdSlider(
                    ads = snapshot.ads,
                    onAd = { ad -> ad.facilityId?.let(onFacility) },
                    modifier = Modifier.padding(horizontal = Space.base),
                )
            }
        }
        item(key = "search") {
            DirectorySearchEntry(
                placeholder = HomeCopy.SEARCH,
                onClick = onSearch,
                modifier = Modifier.padding(horizontal = Space.base),
            )
        }
        if (offerLocation) {
            item(key = "location") {
                LocationOffer(onUseLocation, Modifier.padding(horizontal = Space.base))
            }
        }
        if (snapshot.categories.isNotEmpty()) {
            item(key = "categories") {
                CategoryRail(
                    categories = snapshot.categories,
                    selected = category,
                    onCategory = onCategory,
                )
            }
        }
        item(key = "filters") {
            FilterBar(
                filters = filters,
                chips = homeChips(hasLocation, category),
                onChip = onChip,
                modifier = Modifier.padding(horizontal = Space.base),
            )
        }
        facilityList(list, filters, onFacility, onLoadMore)
    }
}

/** The province's own taxonomy, in the province's own order. Nothing is named in this file. */
@Composable
private fun CategoryRail(
    categories: List<Category>,
    selected: Category?,
    onCategory: (Category) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Space.base),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        items(categories, key = { it.id }) { category ->
            CategoryCircle(
                label = category.nameAr,
                icon = DirectoryIcons.category(category.iconKey),
                selected = category.id == selected?.id,
                onClick = { onCategory(category) },
            )
        }
    }
}

/**
 * The chips, inside one outlined container.
 *
 * The container is what says "these narrow the list", which is why there is no heading above
 * it: a title would repeat what the frame and the icon already make obvious, and cost a line
 * that the list itself can use.
 */
@Composable
private fun FilterBar(
    filters: HomeFilters,
    chips: List<HomeChip>,
    onChip: (HomeChip) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                // A rounded rectangle now that the container is two rows tall: a pill only
                // reads as one on a single line, and would cut the corners off the chips.
                shape = RoundedCornerShape(Radius.large),
            )
            .padding(vertical = Space.sm),
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        // Its own row, so the chips below start at the container's edge and keep the full
        // width to scroll through. Start, not right, so it mirrors itself for Arabic.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            DirectoryIcon(
                icon = DirectoryIcons.filter,
                contentDescription = null,
                size = IconSize.small,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = HomeCopy.FILTERS,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        // One line, always: a chip that wrapped would change the container's height as the
        // category changed how many there are. What does not fit is scrolled to.
        LazyRow(
            contentPadding = PaddingValues(horizontal = Space.md),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            items(chips, key = { it.name }) { chip ->
                DirectoryFilterChip(
                    text = HomeCopy.chip(chip),
                    selected = filters.isOn(chip),
                    onClick = { onChip(chip) },
                )
            }
        }
    }
}

/**
 * The facilities themselves.
 *
 * One list for every combination of chips, so paging, failure and emptiness are handled once.
 * The empty message names the question that was asked, because "no results" leaves the reader
 * to guess which of their choices produced none.
 */
private fun LazyListScope.facilityList(
    list: HomeListState,
    filters: HomeFilters,
    onFacility: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    if (list.loading) {
        item(key = "list-loading") { DirectoryLoading(Modifier.padding(top = Space.xxl)) }
        return
    }
    if (list.error != null && list.items.isEmpty()) {
        item(key = "list-error") {
            DirectoryErrorState(
                title = HomeCopy.ERROR,
                body = list.error,
                onRetry = onLoadMore,
                modifier = Modifier.padding(top = Space.lg),
            )
        }
        return
    }
    if (list.items.isEmpty()) {
        item(key = "list-empty") {
            DirectoryEmptyState(
                title = HomeCopy.emptyFor(filters),
                icon = DirectoryIcons.search,
                modifier = Modifier.padding(top = Space.lg),
            )
        }
        return
    }
    itemsIndexed(list.items, key = { _, facility -> "row-${facility.id}" }) { index, facility ->
        Column {
            FacilityRow(facility = facility, onClick = { onFacility(facility.id) })
            if (index < list.items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Space.base),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
    if (list.hasMore || list.error != null) {
        item(key = "list-more") {
            LoadMoreRow(loading = list.loadingMore, onLoadMore = onLoadMore, error = list.error)
        }
    }
}

@Composable
private fun LocationOffer(onUseLocation: () -> Unit, modifier: Modifier = Modifier) {
    DirectoryCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(
                text = HomeCopy.LOCATION_TITLE,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = HomeCopy.LOCATION_BODY,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                overflow = TextOverflow.Ellipsis,
            )
            DirectoryTextButton(text = HomeCopy.LOCATION_ACTION, onClick = onUseLocation)
        }
    }
}
