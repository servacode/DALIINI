package com.servacode.directory.feature.facility

import com.servacode.directory.core.database.RecentlyViewedStore
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.model.RecentFacility
import com.servacode.directory.core.testing.FakeRecentlyViewedStore
import com.servacode.directory.core.testing.facility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordVisitTest {
    private val store = FakeRecentlyViewedStore()
    private var now = 1_000L
    private val record = RecordVisitUseCase(store) { now }

    private fun detail(id: String) = FacilityDetail(summary = facility(id).copy(category = Category("c", "صيدلية")))

    @Test fun `an opened facility is remembered with its name and category`() = runTest {
        record(detail("a"))

        assertEquals(listOf(RecentFacility("a", facility("a").nameAr, "صيدلية", 1_000L)), store.observe().first())
    }

    @Test fun `opening one again moves it to the top instead of repeating it`() = runTest {
        record(detail("a"))
        now = 2_000L
        record(detail("b"))
        now = 3_000L
        record(detail("a"))

        assertEquals(listOf("a", "b"), store.observe().first().map { it.id })
    }

    @Test fun `only the last twenty are kept`() = runTest {
        (1..25).forEach { index ->
            now = index.toLong()
            record(detail("f$index"))
        }

        val kept = store.observe().first()
        assertEquals(RecentlyViewedStore.LIMIT, kept.size)
        assertEquals("f25", kept.first().id)
    }

    @Test fun `a store that cannot write does not reach the screen`() = runTest {
        val broken = object : RecentlyViewedStore {
            override fun observe(): Flow<List<RecentFacility>> = emptyFlow()
            override suspend fun record(value: RecentFacility) = error("disk full")
            override suspend fun clear() = Unit
        }

        RecordVisitUseCase(broken) { 0L }(detail("a"))
    }
}
