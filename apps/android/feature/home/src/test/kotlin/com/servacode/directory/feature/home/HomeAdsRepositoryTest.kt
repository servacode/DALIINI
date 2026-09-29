package com.servacode.directory.feature.home

import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.model.AdAction
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.HomeAd
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.testing.FakePublicCache
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAdsRepositoryTest {
    private val raqqa = Province("raqqa", "الرقة")
    private val cache = FakePublicCache()
    private val api = ScriptedPublicApi()

    private fun ad(id: String, duration: Int = 5_000, action: AdAction = AdAction.None) = HomeAd(
        id = id,
        imageUrl = "https://cdn.test/$id.jpg",
        titleAr = "إعلان $id",
        slideDurationMs = duration,
        action = action,
    )

    private fun snapshot(vararg ads: HomeAd) =
        HomeSnapshot(raqqa, emptyList(), emptyList(), ads = ads.toList(), refreshedAtEpochMillis = 1)

    private fun load() = HomeAdsRepository(cache, api).load("raqqa")

    @Test fun `cached ads first, then the backend's, written back into the home snapshot`() = runTest {
        cache.homes["raqqa"] = snapshot(ad("old"))
        api.adsAnswer = { listOf(ad("b"), ad("a")) }

        val emitted = load().toList()

        assertEquals(listOf(listOf(ad("old")), listOf(ad("b"), ad("a"))), emitted)
        assertEquals(listOf(ad("b"), ad("a")), cache.homes["raqqa"]?.ads)
        assertEquals(listOf("ads:raqqa"), api.calls)
    }

    @Test fun `the backend's order is kept`() = runTest {
        api.adsAnswer = { listOf(ad("3"), ad("1"), ad("2")) }

        assertEquals(listOf("3", "1", "2"), load().toList().last().map { it.id })
    }

    @Test fun `offline with nothing cached hides the slider`() = runTest {
        assertEquals(listOf(emptyList<HomeAd>()), load().toList())
    }

    @Test fun `offline with a cache keeps the cached slides`() = runTest {
        cache.homes["raqqa"] = snapshot(ad("old"))

        assertEquals(listOf(ad("old")), load().toList().last())
    }

    @Test fun `without a snapshot the fresh answer is shown but not cached`() = runTest {
        api.adsAnswer = { listOf(ad("a")) }

        assertEquals(listOf(ad("a")), load().toList().single())
        assertTrue(cache.homes.isEmpty())
    }

    @Test fun `undrawable and repeated slides are dropped, a failure is empty`() {
        val blank = ad("x").copy(imageUrl = " ")
        assertEquals(listOf(ad("a")), HomeAds.sanitize(listOf(ad("a"), blank, ad("a"))))
        assertEquals(emptyList<HomeAd>(), HomeAds.visible(Loaded.Failed(AppError(AppError.Kind.OFFLINE))))
    }

    @Test fun `slide duration is bounded and pages wrap`() {
        assertEquals(4_000L, HomeAds.slideMillis(ad("a", duration = 4_000)))
        assertEquals(HomeAds.MIN_SLIDE_MS, HomeAds.slideMillis(ad("a", duration = 0)))
        assertEquals(HomeAds.MAX_SLIDE_MS, HomeAds.slideMillis(ad("a", duration = 600_000)))
        assertEquals(1, HomeAds.nextPage(0, 3))
        assertEquals(0, HomeAds.nextPage(2, 3))
        assertEquals(0, HomeAds.nextPage(0, 0))
    }

    @Test fun `labels and clickability come from the ad`() {
        assertEquals("إعلان a", HomeAds.label(ad("a")))
        assertEquals("sub", HomeAds.label(ad("a").copy(titleAr = " ", subtitleAr = "sub")))
        assertFalse(HomeAds.isClickable(ad("a")))
        assertTrue(HomeAds.isClickable(ad("a", action = AdAction.OpenFacility("f"))))
    }
}
