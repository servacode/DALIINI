package com.servacode.directory.feature.home

import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.database.PublicCache
import com.servacode.directory.core.database.cacheFirst
import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.model.AdAction
import com.servacode.directory.core.model.HomeAd
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The home slider's advertisements, cache-first.
 *
 * The cached copy is the one inside the province's home snapshot: the home payload carries
 * the same ads, so no second table is needed. A fresh answer from `public/ads` is written back
 * into that snapshot when one is cached.
 *
 * The slider is decoration: every failure, and an empty answer, is an empty list and the
 * slider is not shown at all.
 */
class HomeAdsRepository @Inject constructor(
    private val cache: PublicCache,
    private val api: PublicApiBoundary,
) {
    /** The ads of [provinceId], the province the home is showing. */
    fun load(provinceId: String): Flow<List<HomeAd>> = cacheFirst(
        read = { cache.home(provinceId)?.ads?.takeIf { it.isNotEmpty() } },
        fetch = { api.ads(provinceId) },
        write = { ads -> cache.home(provinceId)?.let { cache.putHome(it.copy(ads = ads)) } },
    ).map(HomeAds::visible)
}

/** The slider's rules, kept free of Android so they are tested on the JVM. */
object HomeAds {
    const val MIN_SLIDE_MS = 2_000L
    const val MAX_SLIDE_MS = 30_000L

    /** What the slider shows for a load: the ads it can draw, or nothing. */
    fun visible(loaded: Loaded<List<HomeAd>>): List<HomeAd> = when (loaded) {
        is Loaded.Cached -> sanitize(loaded.value)
        is Loaded.Fresh -> sanitize(loaded.value)
        is Loaded.Stale -> sanitize(loaded.value)
        is Loaded.Failed -> emptyList()
    }

    /** The backend's order is the sort order; only undrawable and repeated slides go. */
    fun sanitize(ads: List<HomeAd>): List<HomeAd> =
        ads.filter { it.id.isNotBlank() && it.imageUrl.isNotBlank() }.distinctBy { it.id }

    /** How long a slide stays, bounded so a bad value neither flickers nor freezes. */
    fun slideMillis(ad: HomeAd): Long = ad.slideDurationMs.toLong().coerceIn(MIN_SLIDE_MS, MAX_SLIDE_MS)

    /** The page after [current], wrapping to the first. */
    fun nextPage(current: Int, count: Int): Int = if (count <= 0) 0 else (current + 1) % count

    /** Read by screen readers for the slide; the title when there is one. */
    fun label(ad: HomeAd): String? = ad.titleAr?.takeIf { it.isNotBlank() } ?: ad.subtitleAr?.takeIf { it.isNotBlank() }

    fun isClickable(ad: HomeAd): Boolean = ad.action != AdAction.None
}
