package com.servacode.directory.feature.home

import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.AdSlider
import com.servacode.directory.core.designsystem.BrandColors
import com.servacode.directory.core.designsystem.CategoryCircle
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.BrandSymbol
import com.servacode.directory.core.designsystem.FacilityCard
import com.servacode.directory.core.designsystem.DirectoryEmptyState
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryCompactFilterChip
import com.servacode.directory.core.designsystem.DirectoryInlineLoading
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryIconButton
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryOfflineNotice
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectorySearchEntry
import com.servacode.directory.core.designsystem.DirectoryTextButton
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
                onSearch = onSearch,
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
 * The app, where the reader is, what is waiting for them, and the one thing they came to do.
 *
 * It used to be a flat band with a name in the middle and two things pinned to its ends, and it
 * read as a title bar with a page under it rather than as the top of this page. Now it is a
 * soft green field with a rounded foot: the mark and the name together on one side — a name
 * beside its own mark is read as a brand and not as a heading — the bell on the other, the
 * place under them as something obviously pressed, and the search at its edge, because looking
 * for a pharmacy is why the page was opened.
 */
@Composable
private fun HomeHeader(
    place: String?,
    unread: Int,
    onPlace: () -> Unit,
    onNotifications: () -> Unit,
    onSearch: () -> Unit,
) {
    Surface(
        color = BrandColors.softer,
        shape = RoundedCornerShape(bottomStart = Radius.xl, bottomEnd = Radius.xl),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = Space.base)
                .padding(top = Space.sm, bottom = Space.base),
            verticalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    BrandSymbol(size = HEADER_MARK)
                    Text(
                        text = HomeCopy.TITLE,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
                NotificationBell(unread = unread, onClick = onNotifications)
            }
            HerePlace(place = place, onClick = onPlace)
            DirectorySearchEntry(placeholder = HomeCopy.SEARCH, onClick = onSearch)
        }
    }
}

/** Beside the name, small enough to be a signature rather than a picture. */
private val HEADER_MARK = 36.dp

/**
 * Where the reader is, drawn as something that can be changed.
 *
 * It was two lines of loose text that looked like a caption, so nobody pressed it. On its own
 * white pill with a pin at one end and a chevron at the other, it says what it is and that it
 * opens something, and a long "province — neighbourhood" ellipsises inside it.
 */
@Composable
private fun HerePlace(place: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(Radius.pill),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Space.md, vertical = Space.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            DirectoryIcon(
                icon = DirectoryIcons.pin,
                contentDescription = null,
                size = IconSize.small,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = HomeCopy.YOU_ARE_IN,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = place ?: HomeCopy.PROVINCE_CHOOSE,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            DirectoryIcon(
                icon = DirectoryIcons.chevron,
                contentDescription = null,
                size = IconSize.small,
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

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
            tint = MaterialTheme.colorScheme.primary,
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
        // A breath under the bar. Without it the search field starts hard against the dark
        // edge and the two read as one block, which makes the bar look taller than it is.
        contentPadding = PaddingValues(top = Space.base, bottom = Space.xxl),
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
                // A narrower margin than the rest of the page: this container has to hold
                // four filters across, and every point given to the margin is taken from them.
                modifier = Modifier.padding(horizontal = Space.md),
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
        verticalArrangement = Arrangement.spacedBy(Space.xs),
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
        // Every filter on screen at once, and none of them scrolled to. A row that scrolls
        // hides its last chip behind an edge, and the last chip here is the one a pharmacy
        // reader came for. The chips keep their natural widths so the long name is not padded
        // out to match the short one, and the compact chip is what buys the room.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.sm),
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            chips.forEach { chip ->
                DirectoryCompactFilterChip(
                    text = HomeCopy.chip(chip),
                    selected = filters.isOn(chip),
                    onClick = { onChip(chip) },
                    icon = chipIcon(chip),
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
        // The next page is asked for by reading, not by pressing. A button at the foot of an
        // endless list is a toll gate: the reader has already said what they want by scrolling
        // towards it. Asking a few rows early means the page is usually there before they
        // arrive, and asking on first composition of that row means it is asked exactly once.
        LaunchedEffect(index, list.items.size) {
            if (index >= list.items.size - LOAD_AHEAD) onLoadMore()
        }
        FacilityCard(
            facility = facility,
            onClick = { onFacility(facility.id) },
            modifier = Modifier.padding(horizontal = Space.base),
        )
    }
    if (list.loadingMore) {
        item(key = "list-loading-more") {
            DirectoryInlineLoading(
                message = HomeCopy.LOADING_MORE,
                modifier = Modifier.padding(Space.base),
            )
        }
    }
    // A failure is the one case that still needs a press: retrying by itself would spin
    // against a backend that is already refusing, and say nothing while it did.
    if (list.error != null) {
        item(key = "list-more-failed") {
            LoadMoreRow(loading = false, onLoadMore = onLoadMore, error = list.error)
        }
    }
}

/**
 * The mark beside each filter.
 *
 * A heading arrow for distance, a clock for the hour, a day on a calendar for the roster: each
 * says what its word says, so the chips can be picked out at a glance once they are familiar,
 * without the word ever being replaced.
 */
@DrawableRes
private fun chipIcon(chip: HomeChip): Int = when (chip) {
    HomeChip.NEAREST -> DirectoryIcons.route
    HomeChip.OPEN_NOW -> DirectoryIcons.clock
    HomeChip.DUTY_TODAY -> DirectoryIcons.schedule
}

/** How many rows from the end to ask for the next page. */
private const val LOAD_AHEAD = 4

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
