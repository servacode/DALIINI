package com.servacode.directory.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import com.servacode.directory.core.designsystem.LocalDirectoryTones
import com.servacode.directory.core.designsystem.Sizes
import com.servacode.directory.core.model.RecentFacility
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.servacode.directory.core.model.HomeAd
import com.servacode.directory.core.model.AdAction
import androidx.core.net.toUri
import android.content.Intent
import android.content.Context
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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
import com.servacode.directory.core.designsystem.appErrorText
import com.servacode.directory.core.designsystem.BrandColors
import com.servacode.directory.core.designsystem.BrandSymbol
import com.servacode.directory.core.designsystem.CategoryCircle
import com.servacode.directory.core.designsystem.DirectoryBrandHeader
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryCompactFilterChip
import com.servacode.directory.core.designsystem.DirectoryEmptyState
import com.servacode.directory.core.designsystem.DirectoryFilterChip
import com.servacode.directory.core.designsystem.DirectoryIllustrations
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryIconButton
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryInlineLoading
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryOfflineNotice
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryPill
import com.servacode.directory.core.designsystem.DirectorySearchEntry
import com.servacode.directory.core.designsystem.DirectoryTextButton
import com.servacode.directory.core.designsystem.FacilityCard
import com.servacode.directory.core.designsystem.IconSize
import com.servacode.directory.core.designsystem.LoadMoreRow
import com.servacode.directory.core.designsystem.Radius
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.location.FOREGROUND_LOCATION_PERMISSIONS
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.CategoryTags
import com.servacode.directory.core.model.FacilityTag

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
    onEmergencyNumbers: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    extras: HomeExtrasViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recent by extras.recent.collectAsStateWithLifecycle()
    val dataSaver by extras.dataSaver.collectAsStateWithLifecycle()
    val offerDataSaver by extras.offerDataSaver.collectAsStateWithLifecycle()
    val place by viewModel.place.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val list by viewModel.list.collectAsStateWithLifecycle()
    val hasLocation by viewModel.hasLocation.collectAsStateWithLifecycle()
    val unread by viewModel.unread.collectAsStateWithLifecycle()
    val ads by viewModel.ads.collectAsStateWithLifecycle()
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
                illustration = DirectoryIllustrations.location,
                action = HomeCopy.PROVINCE_CHOOSE,
                onAction = onProvince,
            )
            is HomeUiState.Error -> DirectoryErrorState(
                title = HomeCopy.ERROR,
                modifier = Modifier.padding(padding),
                error = value.error,
                onRetry = viewModel::refresh,
            )
            is HomeUiState.Content -> HomeContent(
                value = value,
                padding = padding,
                place = here,
                offerLocation = offerLocation,
                filters = filters,
                category = category,
                tags = tags,
                list = list,
                // Data saver: no slider, so none of its pictures are fetched.
                ads = if (dataSaver) emptyList() else ads,
                hasLocation = hasLocation,
                extraItems = {
                    if (offerDataSaver) {
                        item(key = "data-saver") {
                            DataSaverOffer(
                                onAccept = extras::acceptDataSaver,
                                onDismiss = extras::dismissDataSaver,
                                modifier = Modifier.padding(horizontal = Space.base),
                            )
                        }
                    }
                    item(key = "emergency") {
                        EmergencyShortcut(onEmergencyNumbers, Modifier.padding(horizontal = Space.base))
                    }
                    if (recent.isNotEmpty()) {
                        item(key = "recent") { RecentRail(recent, onFacility) }
                    }
                },
                onChip = viewModel::toggle,
                onCategory = viewModel::select,
                onSpecialty = viewModel::chooseSpecialty,
                onService = viewModel::chooseService,
                onClearTags = viewModel::clearTags,
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
    DirectoryBrandHeader {
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
    DirectoryPill(modifier = modifier, onClick = onClick) {
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
            // The count is part of what the bell says, not a stray number read after it.
            label = if (unread > 0) HomeCopy.unreadNotifications(unread) else HomeCopy.NOTIFICATIONS,
            onClick = onClick,
            tint = MaterialTheme.colorScheme.primary,
        )
        if (unread > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(Space.lg)
                    .background(BrandColors.danger, CircleShape)
                    .clearAndSetSemantics { },
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
    place: String?,
    offerLocation: Boolean,
    filters: HomeFilters,
    category: Category?,
    tags: CategoryTags,
    list: HomeListState,
    ads: List<HomeAd>,
    hasLocation: Boolean,
    extraItems: LazyListScope.() -> Unit,
    onChip: (HomeChip) -> Unit,
    onCategory: (Category) -> Unit,
    onSpecialty: (String?) -> Unit,
    onService: (String?) -> Unit,
    onClearTags: () -> Unit,
    onLoadMore: () -> Unit,
    onUseLocation: () -> Unit,
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onFacility: (String) -> Unit,
) {
    val snapshot = value.snapshot
    // Pulling the page down is what people do when they want to know it is current, so it does
    // what they mean: the snapshot, the list and the place are all asked again.
    PullToRefreshBox(
        isRefreshing = list.loading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize().padding(padding),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
            if (ads.isNotEmpty()) {
                item(key = "ads") {
                    val context = LocalContext.current
                    AdSlider(
                        ads = ads,
                        onAd = { ad ->
                            when (val action = ad.action) {
                                AdAction.None -> Unit
                                is AdAction.OpenFacility -> onFacility(action.facilityId)
                                // A category the province serves is chosen on this page; one it
                                // does not serve is not followed.
                                is AdAction.OpenCategory -> snapshot.categories
                                    .firstOrNull { it.id == action.categoryId }
                                    ?.let(onCategory)
                                is AdAction.OpenUrl -> openExternalPage(context, action.url)
                            }
                        },
                        modifier = Modifier.padding(horizontal = Space.base),
                    )
                }
            }
            if (offerLocation) {
                item(key = "location") {
                    LocationOffer(onUseLocation, Modifier.padding(horizontal = Space.base))
                }
            }
            extraItems()
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
            if (!tags.isEmpty) {
                item(key = "tags") {
                    TagFilters(
                        tags = tags,
                        filters = filters,
                        onSpecialty = onSpecialty,
                        onService = onService,
                        onClear = onClearTags,
                    )
                }
            }
            facilityList(list, filters, place, onFacility, onLoadMore, onClearTags)
        }
    }
}

/**
 * The chosen category's specialties and services, a row of each, under the chips.
 *
 * One of each at a time, as the backend narrows by one of each. «كل التخصصات» and «كل الخدمات»
 * lead their rows because nothing chosen is a state the reader can see and return to, and
 * «امسح التصفية» appears once something is chosen, for when the choice has scrolled out of
 * sight. The rows scroll rather than wrap: a clinic may offer twenty specialties, and wrapped
 * they would push the list off the screen.
 */
@Composable
private fun TagFilters(
    tags: CategoryTags,
    filters: HomeFilters,
    onSpecialty: (String?) -> Unit,
    onService: (String?) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        if (tags.specialties.isNotEmpty()) {
            TagRow(
                label = HomeCopy.SPECIALTIES,
                all = HomeCopy.ALL_SPECIALTIES,
                choices = tags.specialties,
                chosen = filters.specialtyId,
                onChoose = onSpecialty,
            )
        }
        if (tags.services.isNotEmpty()) {
            TagRow(
                label = HomeCopy.SERVICES,
                all = HomeCopy.ALL_SERVICES,
                choices = tags.services,
                chosen = filters.serviceTagId,
                onChoose = onService,
            )
        }
        if (filters.hasTags) {
            DirectoryCompactFilterChip(
                text = HomeCopy.CLEAR_TAGS,
                selected = false,
                onClick = onClear,
                icon = DirectoryIcons.close,
                modifier = Modifier.padding(horizontal = Space.base),
            )
        }
    }
}

/**
 * One labelled row: «all» first, then each choice in the operators' order. A screen reader hears
 * the label as a heading, then each chip with whether it is the one chosen.
 */
@Composable
private fun TagRow(
    label: String,
    all: String,
    choices: List<FacilityTag>,
    chosen: String?,
    onChoose: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Space.base).semantics { heading() },
        )
        LazyRow(
            modifier = Modifier.semantics { selectableGroup() },
            contentPadding = PaddingValues(horizontal = Space.base),
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            item(key = "all") {
                DirectoryFilterChip(text = all, selected = chosen == null, onClick = { onChoose(null) })
            }
            items(choices, key = { it.id }) { choice ->
                DirectoryFilterChip(
                    text = choice.nameAr,
                    selected = choice.id == chosen,
                    onClick = { onChoose(choice.id) },
                )
            }
        }
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
    place: String?,
    onFacility: (String) -> Unit,
    onLoadMore: () -> Unit,
    onClearTags: () -> Unit,
) {
    if (list.loading) {
        item(key = "list-loading") { DirectoryLoading(Modifier.padding(top = Space.xxl)) }
        return
    }
    if (list.error != null && list.items.isEmpty()) {
        item(key = "list-error") {
            DirectoryErrorState(
                title = HomeCopy.ERROR,
                error = list.error,
                onRetry = onLoadMore,
                modifier = Modifier.padding(top = Space.lg),
            )
        }
        return
    }
    if (list.items.isEmpty()) {
        item(key = "list-empty") {
            // A specialty or a service is the one choice here that can be undone in place, so
            // the empty list says so and offers to.
            DirectoryEmptyState(
                title = HomeCopy.emptyFor(filters),
                modifier = Modifier.padding(top = Space.lg),
                body = if (filters.hasTags) HomeCopy.EMPTY_CHOICE_BODY else HomeCopy.emptyBodyFor(place),
                illustration = DirectoryIllustrations.noResults,
                action = if (filters.hasTags) HomeCopy.CLEAR_TAGS else null,
                onAction = if (filters.hasTags) onClearTags else null,
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
            LoadMoreRow(loading = false, onLoadMore = onLoadMore, error = appErrorText(list.error))
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

/** «أرقام الطوارئ», one tap from Home: the numbers someone may need before anything else. */
@Composable
private fun EmergencyShortcut(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val danger = LocalDirectoryTones.current.danger
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.large))
            .background(danger.container)
            .clickable(role = Role.Button, onClick = onOpen)
            .heightIn(min = Sizes.touchTarget)
            .padding(horizontal = Space.base, vertical = Space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        DirectoryIcon(DirectoryIcons.emergency, null, tint = danger.content)
        Text(
            text = HomeCopy.EMERGENCY,
            style = MaterialTheme.typography.titleSmall,
            color = danger.content,
            modifier = Modifier.weight(1f),
        )
        DirectoryIcon(DirectoryIcons.chevron, null, size = IconSize.small, tint = danger.content)
    }
}

/** «شوهدت مؤخراً»: the last facilities opened on this device, a swipe across. */
@Composable
private fun RecentRail(recent: List<RecentFacility>, onFacility: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Text(
            text = HomeCopy.RECENT,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = Space.base).semantics { heading() },
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = Space.base),
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            items(recent, key = { "recent-" + it.id }) { facility ->
                DirectoryPill(onClick = { onFacility(facility.id) }) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = RECENT_WIDTH)
                            .heightIn(min = Sizes.touchTarget)
                            .padding(horizontal = Space.md, vertical = Space.sm),
                    ) {
                        Text(
                            text = facility.nameAr,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        facility.categoryNameAr?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

private val RECENT_WIDTH = 200.dp

/** Offered once, on a metered connection: save data from now on, or not. */
@Composable
private fun DataSaverOffer(onAccept: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    DirectoryCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(
                text = HomeCopy.DATA_SAVER_TITLE,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = HomeCopy.DATA_SAVER_BODY,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                DirectoryTextButton(text = HomeCopy.DATA_SAVER_ACCEPT, onClick = onAccept)
                DirectoryTextButton(text = HomeCopy.DATA_SAVER_DISMISS, onClick = onDismiss)
            }
        }
    }
}

/**
 * Opens an advertisement's page in the browser. The mapper already admitted only `https`; it is
 * checked again here, at the last moment, and a phone without a browser simply does nothing.
 */
private fun openExternalPage(context: Context, url: String) {
    val uri = url.toUri()
    if (uri.scheme != "https" || uri.host.isNullOrBlank()) return
    val intent = Intent(Intent.ACTION_VIEW, uri)
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Nothing can show it; the tap is a no-op rather than a crash.
    }
}
