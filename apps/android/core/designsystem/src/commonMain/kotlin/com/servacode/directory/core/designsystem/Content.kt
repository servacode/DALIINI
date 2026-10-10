package com.servacode.directory.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.servacode.directory.core.model.AdAction
import com.servacode.directory.core.model.AvailabilityState
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.HomeAd
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * A picture from the backend, on the app's own placeholder until it arrives. Decorative by
 * default: a facility's name is always written beside its picture.
 */
@Composable
fun DirectoryImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(Radius.medium),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (url != null) {
            // The caller gives the frame its size; the picture fills exactly that and is cropped
            // to it, so a slow or missing image never changes the layout around it.
            RemoteImage(
                url = url,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/**
 * Whether a facility is open, on duty, closed for now or closed, in the backend's words and in a
 * colour that matches them. The word carries the meaning; the colour only agrees with it.
 */
@Composable
fun AvailabilityPill(summary: FacilitySummary, modifier: Modifier = Modifier) {
    AvailabilityPill(summary.availability, AvailabilityWords.of(summary), modifier)
}

/** The same pill where all that is known is the state itself, as on the map. */
@Composable
fun AvailabilityPill(state: AvailabilityState, modifier: Modifier = Modifier) {
    AvailabilityPill(state, AvailabilityWords.of(state), modifier)
}

@Composable
private fun AvailabilityPill(state: AvailabilityState, text: String, modifier: Modifier) {
    StatusChip(text = text, tone = StatusTones.availability(state), modifier = modifier)
}

/** How far away the backend said a facility is. Shown only when it said. */
@Composable
fun DistanceLabel(meters: Double?, modifier: Modifier = Modifier) {
    if (meters == null) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        DirectoryIcon(DirectoryIcons.pin, null, size = IconSize.small, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = DirectoryWords.distance(meters),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A facility's rating, when it has one. */
@Composable
fun RatingBadge(average: Double?, count: Int, modifier: Modifier = Modifier) {
    if (average == null || count <= 0) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        DirectoryIcon(DirectoryIcons.star, null, size = IconSize.small, tint = BrandColors.warning)
        Text(
            text = ratingText(average, count),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "4.5 (12)" — the average as the backend holds it, with how many said so. */
fun ratingText(average: Double, count: Int): String {
    val rounded = (average * 10).roundToInt()
    return "${rounded / 10}.${rounded % 10} ($count)"
}

/** A line of detail: an icon that repeats what the words say, and the words. */
@Composable
fun MetaRow(
    icon: DirectoryGlyph,
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        DirectoryIcon(icon, null, tint = color)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * One facility in a list: its picture when it has one, its name, what it is, whether it is open,
 * how far it is when the backend said, and its rating when it has one.
 */
@Composable
fun FacilityRow(
    facility: FacilitySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // No ripple, for the same reason the category circles have none: a press painted a
            // hard grey rectangle the full width of the row, and a flat rectangle held over a
            // white list is read as a still image rather than as a touch. Nothing is lost by
            // removing it — the facility's page now opens with no transition at all, so the
            // screen changing is itself the acknowledgement, and it arrives sooner.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                // Without a ripple a screen reader is the only way to learn this row acts; it
                // is announced as a button that opens the facility.
                onClickLabel = stringResource(Res.string.ds_open_details),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = Space.md, horizontal = Space.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        FacilityRowContent(facility)
    }
}

/** The same facility, as its own card: how a result of a search or a saved list is shown. */
@Composable
fun FacilityCard(
    facility: FacilitySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * False on a card listed under another day than today (the duty week): «مفتوح الآن» and
     * «مناوبة اليوم» are about this moment, and under Thursday they read as Thursday's.
     */
    showStatusNow: Boolean = true,
) {
    DirectoryCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            FacilityRowContent(facility, showStatusNow)
        }
    }
}

@Composable
private fun RowScope.FacilityRowContent(facility: FacilitySummary, showStatusNow: Boolean = true) {
    // First child, so it sits on the right in Arabic without the layout naming a side.
    FacilityThumbnail(facility.imageUrl, Modifier.size(Sizes.thumbnail))
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Text(
            text = facility.nameAr,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = facility.category.nameAr,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        facility.cityNameAr?.let { city ->
            MetaRow(icon = DirectoryIcons.pin, text = city)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
        ) {
            DistanceLabel(facility.distanceMeters)
            RatingBadge(facility.ratingAverage, facility.ratingCount)
        }
        if (showStatusNow) StatusBadges(facility)
        OpenDetailsButton(Modifier.align(Alignment.End))
    }
}

/**
 * The card's own call to action, drawn as something that is obviously pressed.
 *
 * Words alone in the brand colour can read as a caption; a bordered, tinted shape cannot. The
 * whole row still opens the facility, so this is the row's label rather than a second target
 * inside it — one thing to press, and the screen reader announces it once.
 */
@Composable
private fun OpenDetailsButton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = Space.md, vertical = Space.xs)
            .clearAndSetSemantics { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Text(
            text = ContentText.OPEN_DETAILS,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
        )
        DirectoryIcon(
            icon = DirectoryIcons.chevron,
            contentDescription = null,
            size = IconSize.small,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** What a card's own button says, read from the design system's resources. */
object ContentText {
    val OPEN_DETAILS: String
        @Composable get() = stringResource(Res.string.ds_open_details)
}

/**
 * Whether the doors are open, and whether today's roster names this facility.
 *
 * Two badges because they are two facts. The first is always shown, because "closed" is as
 * useful as "open" to someone deciding where to go. The second appears only when it is true:
 * there is no badge for not being on the roster, since almost nothing is, and saying so of
 * every facility would drown the one that is.
 *
 * Neither badge relies on its colour. The words carry the meaning and the colour agrees with
 * them, which is what keeps them readable to someone who cannot tell green from red.
 */
@Composable
fun StatusBadges(facility: FacilitySummary, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        val state = if (facility.isOpenNow) AvailabilityState.OPEN else AvailabilityState.CLOSED
        StatusChip(text = DirectoryVocabulary.availability(state), tone = StatusTones.availability(state))
        if (facility.isOnDutyToday) {
            StatusChip(text = StatusText.ON_DUTY_TODAY, tone = StatusTones.availability(AvailabilityState.DUTY))
        }
    }
}

/**
 * On today's roster: a different fact from "on duty now", so it keeps its own words. Open and
 * closed are the shared vocabulary's.
 */
object StatusText {
    val ON_DUTY_TODAY: String
        @Composable get() = stringResource(Res.string.ds_on_duty_today)
}

/**
 * The picture a facility wears in a list: its own first photograph, or the brand's mark.
 *
 * The frame is the same size either way, so a row does not resize when a picture arrives and
 * nothing stands in for a photograph the owner never uploaded.
 */
@Composable
fun FacilityThumbnail(imageUrl: String? = null, modifier: Modifier = Modifier) {
    // With data saver on, a list costs no pictures: the placeholder stands in for every one.
    if (imageUrl != null && !LocalDataSaver.current) {
        DirectoryImage(url = imageUrl, modifier = modifier)
        return
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.medium))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        DirectoryIcon(DirectoryIcons.pin, null, size = IconSize.large, tint = MaterialTheme.colorScheme.primary)
    }
}

/**
 * The picture on an account: round, or the person mark when there is none.
 *
 * It is here rather than in the account screen so that the one module that knows how to fetch a
 * picture stays the one module that fetches pictures.
 */
@Composable
fun DirectoryAvatar(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.avatar,
) {
    if (imageUrl != null) {
        DirectoryImage(
            url = imageUrl,
            modifier = modifier.size(size),
            shape = RoundedCornerShape(Radius.pill),
        )
        return
    }
    Box(
        modifier = modifier
            .size(size)
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
}

/**
 * A category, as Home shows them: a circle that fills when it is the one being looked at, with
 * its name under it.
 */
@Composable
fun CategoryCircle(
    label: String,
    icon: DirectoryGlyph,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(Sizes.categoryLabel)
            // No ripple. The press used to paint a hard square across the whole column, which
            // is the one rectangle on this screen with no rounded corner and reads as a glitch.
            // A category answers by becoming selected — the circle fills and the label turns —
            // and that is both faster and clearer than a flash under the finger.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = Space.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.categoryCircle)
                .clip(RoundedCornerShape(Radius.pill))
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                ),
            contentAlignment = Alignment.Center,
        ) {
            DirectoryIcon(
                icon = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            // Two lines, because "مستلزمات طبية" does not fit on one under a 56dp circle, and
            // a category whose name is cut in half is a category the reader has to guess at.
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The pictures a facility has, one at a time, with a mark under them for how many there are.
 *
 * A facility with no picture gets the brand's own frame instead of an empty grey box.
 */
@Composable
fun PhotoPager(
    urls: List<String>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onPhoto: ((Int) -> Unit)? = null,
) {
    if (urls.isEmpty()) {
        FacilityThumbnail(modifier = modifier.fillMaxWidth())
        return
    }
    val pages = rememberPagerState(pageCount = { urls.size })
    Box(modifier = modifier.fillMaxWidth()) {
        // The next photo is fetched ahead of the swipe, unless the reader is saving data.
        HorizontalPager(
            state = pages,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = if (LocalDataSaver.current) 0 else 1,
        ) { page ->
            val photo = Modifier
                .fillMaxSize()
                .let { if (onPhoto == null) it else it.clickable { onPhoto(page) } }
            DirectoryImage(
                url = urls[page],
                modifier = photo,
                contentDescription = contentDescription,
                shape = RectangleShape,
            )
        }
        if (urls.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Space.base)
                    .clearAndSetSemantics { },
                horizontalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                urls.indices.forEach { index ->
                    val here = index == pages.currentPage
                    Box(
                        modifier = Modifier
                            .size(if (here) Space.sm else Space.xs)
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(
                                if (here) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            ),
                    )
                }
            }
        }
    }
}

/**
 * One of the few things a screen offers to do: a soft circle, the icon inside it, and the word
 * under it. Disabled when the backend gave nothing to act on, rather than hidden.
 */
@Composable
fun ActionCircle(
    label: String,
    icon: DirectoryGlyph,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Column(
        modifier = modifier
            .width(Sizes.actionCircle + Space.xl)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = Space.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.actionCircle)
                .clip(RoundedCornerShape(Radius.pill))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            DirectoryIcon(icon, null, tint = tint)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A rating as stars. The number is what the backend holds; the stars only draw it, rounding to
 * the nearest whole star, and the figure is written beside them wherever it matters.
 */
@Composable
fun StarRow(
    stars: Int,
    modifier: Modifier = Modifier,
    size: Dp = IconSize.small,
    max: Int = MAX_STARS,
) {
    Row(
        modifier = modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        (1..max).forEach { star ->
            DirectoryIcon(
                icon = DirectoryIcons.star,
                contentDescription = null,
                size = size,
                tint = if (star <= stars) BrandColors.warning else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/** The stars the user themselves gives, out of five, each one its own target. */
@Composable
fun StarPicker(
    stars: Int?,
    onRate: (Int) -> Unit,
    /** Composable because the words come from resources now, and resources are read in it. */
    label: @Composable (Int) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
        (1..MAX_STARS).forEach { star ->
            DirectoryIconButton(
                icon = DirectoryIcons.star,
                label = label(star),
                onClick = { onRate(star) },
                enabled = enabled,
                tint = if (star <= (stars ?: 0)) BrandColors.warning else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/**
 * What a facility's rating adds up to: the average as the backend computed it, the stars that
 * agree with it, and how many people it is made of. Nothing is shown where nobody has rated.
 */
@Composable
fun RatingSummary(
    average: Double?,
    count: Int,
    modifier: Modifier = Modifier,
    emptyText: String = stringResource(Res.string.ds_no_ratings),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        if (average == null || count <= 0) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        Text(
            text = averageText(average),
            style = DirectoryTextStyles.display,
            color = MaterialTheme.colorScheme.onBackground,
        )
        StarRow(stars = average.roundToInt(), size = IconSize.large)
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "4.6" — one decimal, as the rest of the app writes an average. */
fun averageText(average: Double): String {
    val rounded = (average * 10).roundToInt()
    return "${rounded / 10}.${rounded % 10}"
}

const val MAX_STARS = 5

/**
 * The advertisements a province wants seen first.
 *
 * One at a time, at one fixed shape, so the page under it never jumps when an image arrives
 * late or not at all. It advances by itself at the pace the backend set for each slide, and it
 * stops doing that the moment the user touches it or the app leaves the foreground — an
 * advertisement that moves while someone is reading it is worse than one that does not move.
 */
@Composable
fun AdSlider(
    ads: List<HomeAd>,
    onAd: (HomeAd) -> Unit,
    modifier: Modifier = Modifier,
    onShown: (HomeAd) -> Unit = {},
) {
    if (ads.isEmpty()) return
    val pages = rememberPagerState(pageCount = { ads.size })
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val shown by rememberUpdatedState(onShown)

    // A slide is seen once it has settled while the page is in front of someone; one swiped
    // past mid-scroll, or turning behind another screen, is not.
    LaunchedEffect(pages, ads) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { pages.settledPage }.collect { page -> ads.getOrNull(page)?.let(shown) }
        }
    }

    if (ads.size > 1) {
        LaunchedEffect(pages, ads) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    val slide = ads[pages.currentPage].slideDurationMs.toLong()
                    delay(slide.coerceIn(MIN_SLIDE_MS, MAX_SLIDE_MS))
                    if (!pages.isScrollInProgress) {
                        pages.animateScrollToPage((pages.currentPage + 1) % ads.size)
                    }
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pages,
            contentPadding = PaddingValues(horizontal = Space.screen),
            pageSpacing = Space.md,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val ad = ads[page]
            // Read by its title; a slide with no words is still named, never skipped silently.
            val label = ad.titleAr?.takeIf { it.isNotBlank() }
                ?: ad.subtitleAr?.takeIf { it.isNotBlank() }
                ?: stringResource(Res.string.ds_ad_label)
            // Only a slide that leads somewhere is announced and pressed as a button.
            val tap = if (ad.action != AdAction.None) {
                Modifier.clickable(
                    onClickLabel = stringResource(Res.string.ds_ad_open),
                    role = Role.Button,
                ) { onAd(ad) }
            } else {
                Modifier
            }
            DirectoryImage(
                url = ad.imageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(AD_RATIO)
                    .clip(RoundedCornerShape(Radius.large))
                    .then(tap),
                contentDescription = label,
                shape = RoundedCornerShape(Radius.large),
            )
        }
        if (ads.size > 1) {
            val position = stringResource(Res.string.ds_ad_position, pages.currentPage + 1, ads.size)
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = Space.sm)
                    // The dots are one statement: which slide of how many.
                    .clearAndSetSemantics { contentDescription = position },
                horizontalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                ads.indices.forEach { index ->
                    val here = index == pages.currentPage
                    Box(
                        modifier = Modifier
                            .size(width = if (here) Space.lg else Space.sm, height = Space.sm)
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(
                                if (here) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                    )
                }
            }
        }
    }
}

/** Sixteen by nine: the shape every advertisement is laid out at, filled or not. */
private const val AD_RATIO = 16f / 9f
private const val MIN_SLIDE_MS = 2_000L
private const val MAX_SLIDE_MS = 30_000L
