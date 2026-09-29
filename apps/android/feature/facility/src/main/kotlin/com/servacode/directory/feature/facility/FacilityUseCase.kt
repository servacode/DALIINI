package com.servacode.directory.feature.facility

import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.database.RecentlyViewedStore
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.model.RecentFacility
import com.servacode.directory.core.model.FacilityReportReason
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FacilityUseCase @Inject constructor(
    private val repository: FacilityRepository,
) {
    operator fun invoke(id: String): Flow<Loaded<FacilityDetail>> = repository.load(id)
    suspend fun myRating(id: String): Result<Int?> = repository.myRating(id)
    suspend fun rate(id: String, stars: Int): Result<Int> = repository.rate(id, stars)

    /** Taking the rating back, which the account may do as freely as it gave it. */
    suspend fun removeRating(id: String): Result<Unit> = repository.removeRating(id)

    /** Saving is the account's, not the device's: the backend holds it. */
    suspend fun save(id: String): Result<Boolean> = repository.save(id)
    suspend fun unsave(id: String): Result<Boolean> = repository.unsave(id)
}

/** A problem with a facility's details, sent to the operators. Anyone may send one. */
class ReportFacilityUseCase @Inject constructor(
    private val repository: FacilityRepository,
) {
    suspend operator fun invoke(id: String, reason: FacilityReportReason, note: String?): Result<Unit> =
        repository.report(id, reason, note)
}

/**
 * Remembers that this facility was opened, for «شوهدت مؤخراً». On the device only; a failure to
 * write it never reaches the screen.
 */
class RecordVisitUseCase(
    private val store: RecentlyViewedStore,
    private val clock: () -> Long,
) {
    @Inject constructor(store: RecentlyViewedStore) : this(store, System::currentTimeMillis)

    suspend operator fun invoke(detail: FacilityDetail) {
        runCatching {
            store.record(
                RecentFacility(
                    id = detail.summary.id,
                    nameAr = detail.summary.nameAr,
                    categoryNameAr = detail.summary.category.nameAr,
                    viewedAtEpochMillis = clock(),
                ),
            )
        }
    }
}
