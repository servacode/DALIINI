package com.servacode.directory.widget

import android.content.Context
import androidx.core.content.edit
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.servacode.directory.core.database.CacheEvents
import com.servacode.directory.core.database.PublicCache
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.network.DirectoryQuery
import com.servacode.directory.core.network.PublicApiBoundary
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fills the «المناوب الآن» widget, from two places.
 *
 * Every half hour the worker asks the platform the question Home asks — who is on duty right
 * now, nearest first — without touching the app's own cache. And whenever the app itself stores
 * a fresh home snapshot, the widget redraws from that, so opening the app is also a refresh.
 * Nothing here runs when no widget is placed.
 */
@Singleton
class DutyWidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: PublicApiBoundary,
    private val cache: PublicCache,
    private val preferences: DirectoryPreferencesStore,
    private val location: LocationProvider,
) {
    private val store = DutyWidgetStore(context)

    /** Follows the app's own refreshes for as long as the process lives. */
    fun start(scope: CoroutineScope) {
        scope.launch {
            // Periodic work survives restarts on its own; this re-registers it after an update.
            if (hasWidgets()) DutyWidgetWork.schedule(context)
            CacheEvents.homeSnapshots.collect { provinceId ->
                if (hasWidgets()) runCatching { fromHomeSnapshot(provinceId) }
            }
        }
    }

    /**
     * The worker's refresh. Returns false when the platform could not be reached; the widget then
     * keeps its last answer, marked as not current.
     */
    suspend fun refresh(): Boolean {
        val provinceId = preferences.values.first().selectedProvinceId
        if (provinceId == null) {
            push(DutyWidgetContent.NO_PROVINCE)
            return true
        }
        val fix = location.lastKnown()
        return try {
            val onDuty = api.directory(
                DirectoryQuery(
                    provinceId = provinceId,
                    dutyNow = true,
                    latitude = fix?.latitude,
                    longitude = fix?.longitude,
                    pageSize = DutyWidgetContent.MAX_ROWS,
                ),
            ).items
            val rows = DutyWidgetContent.nearestFirst(onDuty).take(DutyWidgetContent.MAX_ROWS)
            // The nearest one gets its number even when the app never opened it: that is the
            // call the widget is for. The others use what the app already knows.
            val phones = phonesFor(rows.map { it.id }, fetch = rows.firstOrNull()?.id)
            push(DutyWidgetContent.from(provinceName(provinceId), rows, phones, System.currentTimeMillis()))
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            push(DutyWidgetContent.offline(store.read()))
            false
        }
    }

    /** The app stored a new home snapshot: redraw from its «مناوب الآن» list, no request made. */
    private suspend fun fromHomeSnapshot(provinceId: String) {
        if (preferences.values.first().selectedProvinceId != provinceId) return
        val snapshot = cache.home(provinceId) ?: return
        val rows = DutyWidgetContent.nearestFirst(snapshot.dutyNow).take(DutyWidgetContent.MAX_ROWS)
        push(
            DutyWidgetContent.from(
                snapshot.province.nameAr,
                rows,
                phonesFor(rows.map { it.id }, fetch = null),
                snapshot.refreshedAtEpochMillis,
            ),
        )
    }

    private suspend fun phonesFor(ids: List<String>, fetch: String?): Map<String, String> =
        ids.mapNotNull { id ->
            val known = runCatching { cache.facility(id)?.phone }.getOrNull()
            val phone = known ?: if (id == fetch) runCatching { api.facility(id).phone }.getOrNull() else null
            phone?.let { id to it }
        }.toMap()

    private suspend fun provinceName(id: String): String? =
        runCatching { cache.provinces().firstOrNull { it.id == id }?.nameAr }.getOrNull()

    private suspend fun hasWidgets(): Boolean =
        runCatching { GlanceAppWidgetManager(context).getGlanceIds(DutyWidget::class.java).isNotEmpty() }
            .getOrDefault(false)

    /** Stores [state] and redraws every placed widget with it. */
    private suspend fun push(state: DutyWidgetState) {
        store.write(state)
        val manager = GlanceAppWidgetManager(context)
        val json = state.encode()
        manager.getGlanceIds(DutyWidget::class.java).forEach { id ->
            updateAppWidgetState(context, id) { it[DutyWidget.STATE] = json }
            DutyWidget().update(context, id)
        }
    }
}

/**
 * The last state, outside any one widget: a widget placed after the last refresh starts from it
 * instead of from nothing.
 */
class DutyWidgetStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun read(): DutyWidgetState? = DutyWidgetState.decode(preferences.getString(KEY, null))

    fun write(state: DutyWidgetState) {
        preferences.edit { putString(KEY, state.encode()) }
    }

    private companion object {
        const val FILE = "duty_widget"
        const val KEY = "state"
    }
}
