package com.servacode.directory.widget

import com.servacode.directory.core.model.FacilitySummary
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What the «المناوب الآن» widget shows, kept between refreshes.
 *
 * Public data only — names, categories, distances, published numbers — so it can sit in the
 * widget's own storage. It is replaced whole on every refresh; a failed refresh keeps the last
 * answer and marks it [offline] rather than emptying the widget.
 */
@Serializable
data class DutyWidgetState(
    /** False until a province is chosen in the app: the widget has nothing to ask for. */
    val provinceChosen: Boolean = false,
    val provinceNameAr: String? = null,
    val facilities: List<DutyWidgetFacility> = emptyList(),
    /** The last answer from the platform; null when there has never been one. */
    val refreshedAtEpochMillis: Long? = null,
    /** The last attempt could not reach the platform; what is shown is the last answer. */
    val offline: Boolean = false,
) {
    val kind: DutyWidgetKind
        get() = when {
            !provinceChosen -> DutyWidgetKind.NO_PROVINCE
            facilities.isNotEmpty() -> DutyWidgetKind.LIST
            refreshedAtEpochMillis != null -> DutyWidgetKind.EMPTY
            offline -> DutyWidgetKind.OFFLINE
            else -> DutyWidgetKind.LOADING
        }

    fun encode(): String = JSON.encodeToString(serializer(), this)

    companion object {
        private val JSON = Json { ignoreUnknownKeys = true }

        /** Anything unreadable — an older shape, a truncated write — is no state at all. */
        fun decode(value: String?): DutyWidgetState? =
            value?.let { runCatching { JSON.decodeFromString(serializer(), it) }.getOrNull() }
    }
}

enum class DutyWidgetKind {
    NO_PROVINCE,

    /** Asked, and not answered yet. */
    LOADING,

    /** Answered, with facilities to show. */
    LIST,

    /** Answered: nobody is on duty right now. */
    EMPTY,

    /** Never answered, and the last attempt found no connection. */
    OFFLINE,
}

/** One row: the facility, how far it is, and the number to call when it is known. */
@Serializable
data class DutyWidgetFacility(
    val id: String,
    val nameAr: String,
    val categoryNameAr: String? = null,
    val distanceMeters: Double? = null,
    val phone: String? = null,
)

/** How the platform's answer becomes the widget's rows. */
object DutyWidgetContent {
    /** At most this many rows; a bigger widget has room for them, a small one shows the first. */
    const val MAX_ROWS = 3

    val NO_PROVINCE = DutyWidgetState(provinceChosen = false)

    /**
     * The facilities on duty now, nearest first where a distance is known, and otherwise in the
     * order the platform served them; each with its published number when one is at hand.
     */
    fun from(
        provinceNameAr: String?,
        onDuty: List<FacilitySummary>,
        phones: Map<String, String>,
        nowEpochMillis: Long,
    ): DutyWidgetState = DutyWidgetState(
        provinceChosen = true,
        provinceNameAr = provinceNameAr,
        facilities = nearestFirst(onDuty).take(MAX_ROWS).map {
            DutyWidgetFacility(
                id = it.id,
                nameAr = it.nameAr,
                categoryNameAr = it.category.nameAr,
                distanceMeters = it.distanceMeters,
                phone = phones[it.id]?.takeIf(String::isNotBlank),
            )
        },
        refreshedAtEpochMillis = nowEpochMillis,
        offline = false,
    )

    /** The platform could not be reached: the last answer stays, marked as such. */
    fun offline(previous: DutyWidgetState?, provinceChosen: Boolean = true): DutyWidgetState =
        if (!provinceChosen) NO_PROVINCE else (previous ?: DutyWidgetState(provinceChosen = true)).copy(
            provinceChosen = true,
            offline = true,
        )

    /** A stable sort, so equal or unknown distances keep the platform's order. */
    fun nearestFirst(values: List<FacilitySummary>): List<FacilitySummary> =
        values.sortedWith(compareBy(nullsLast()) { it.distanceMeters })

    /** How many rows fit a widget [heightDp] tall: a header, then rows of about 44dp. */
    fun rowsFor(heightDp: Float): Int = ((heightDp - HEADER_DP) / ROW_DP).toInt().coerceIn(1, MAX_ROWS)

    private const val HEADER_DP = 56f
    private const val ROW_DP = 44f
}
