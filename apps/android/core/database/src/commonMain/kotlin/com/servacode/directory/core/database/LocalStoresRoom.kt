package com.servacode.directory.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.inject.Singleton
import com.servacode.directory.core.model.EmergencyNumber
import com.servacode.directory.core.model.EmergencyScope
import com.servacode.directory.core.model.RecentFacility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

@Entity(tableName = "recently_viewed")
data class RecentlyViewedEntity(
    @PrimaryKey val facilityId: String,
    val nameAr: String,
    val categoryNameAr: String?,
    val viewedAtEpochMillis: Long,
)

@Entity(tableName = "emergency_number_cache", primaryKeys = ["cacheKey", "position"])
data class EmergencyNumberEntity(
    /** "national", or the province id the row was served for. */
    val cacheKey: String,
    val position: Int,
    val nameAr: String,
    val number: String,
    val scope: String,
    val provinceId: String?,
    val updatedAtEpochMillis: Long,
)

@Dao
interface LocalStoresDao {
    @Query("SELECT * FROM recently_viewed ORDER BY viewedAtEpochMillis DESC LIMIT :limit")
    fun recentlyViewed(limit: Int): Flow<List<RecentlyViewedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putRecentlyViewed(value: RecentlyViewedEntity)

    @Query(
        "DELETE FROM recently_viewed WHERE facilityId NOT IN " +
            "(SELECT facilityId FROM recently_viewed ORDER BY viewedAtEpochMillis DESC LIMIT :limit)",
    )
    suspend fun trimRecentlyViewed(limit: Int)

    @Query("DELETE FROM recently_viewed")
    suspend fun clearRecentlyViewed()

    @Transaction
    suspend fun recordRecentlyViewed(value: RecentlyViewedEntity, limit: Int) {
        putRecentlyViewed(value)
        trimRecentlyViewed(limit)
    }

    @Query(
        "SELECT * FROM emergency_number_cache WHERE cacheKey IN (:keys) " +
            "ORDER BY cacheKey = 'national' DESC, position ASC",
    )
    suspend fun emergencyNumbers(keys: List<String>): List<EmergencyNumberEntity>

    @Query("DELETE FROM emergency_number_cache WHERE cacheKey = :key")
    suspend fun clearEmergencyNumbers(key: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putEmergencyNumbers(values: List<EmergencyNumberEntity>)

    @Transaction
    suspend fun replaceEmergencyNumbers(keys: List<String>, values: List<EmergencyNumberEntity>) {
        keys.forEach { clearEmergencyNumbers(it) }
        putEmergencyNumbers(values)
    }
}

@Singleton
class RoomRecentlyViewedStore @Inject constructor(
    private val dao: LocalStoresDao,
) : RecentlyViewedStore {
    override fun observe(): Flow<List<RecentFacility>> =
        dao.recentlyViewed(RecentlyViewedStore.LIMIT).map { rows ->
            rows.map { RecentFacility(it.facilityId, it.nameAr, it.categoryNameAr, it.viewedAtEpochMillis) }
        }

    override suspend fun record(value: RecentFacility) {
        dao.recordRecentlyViewed(
            RecentlyViewedEntity(value.id, value.nameAr, value.categoryNameAr, value.viewedAtEpochMillis),
            RecentlyViewedStore.LIMIT,
        )
    }

    override suspend fun clear() = dao.clearRecentlyViewed()
}

@Singleton
class RoomEmergencyNumbersCache @Inject constructor(
    private val dao: LocalStoresDao,
) : EmergencyNumbersCache {
    override suspend fun read(provinceId: String?): List<EmergencyNumber> =
        dao.emergencyNumbers(listOfNotNull(NATIONAL, provinceId)).mapNotNull { row ->
            val scope = runCatching { EmergencyScope.valueOf(row.scope) }.getOrNull() ?: return@mapNotNull null
            EmergencyNumber(row.nameAr, row.number, scope, row.provinceId)
        }

    override suspend fun write(provinceId: String?, values: List<EmergencyNumber>) {
        val now = Clock.System.now().toEpochMilliseconds()
        val rows = values.groupBy { if (it.scope == EmergencyScope.NATIONAL) NATIONAL else provinceId ?: NATIONAL }
            .flatMap { (key, group) ->
                group.mapIndexed { index, value ->
                    EmergencyNumberEntity(
                        key, index, value.nameAr, value.number, value.scope.name, value.provinceId, now,
                    )
                }
            }
        dao.replaceEmergencyNumbers(listOfNotNull(NATIONAL, provinceId), rows)
    }

    private companion object {
        const val NATIONAL = "national"
    }
}
