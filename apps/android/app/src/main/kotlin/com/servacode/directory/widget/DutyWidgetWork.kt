package com.servacode.directory.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * The widget's own refresh, every half hour while at least one widget is placed.
 *
 * No network constraint on purpose: a run without a connection is what marks the widget as not
 * current, instead of leaving an hours-old roster looking fresh. The request itself is one small
 * list, and the nearest facility's number when the app does not have it yet.
 */
class DutyWidgetWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val refresher = EntryPointAccessors
            .fromApplication(applicationContext, DutyWidgetEntryPoint::class.java)
            .refresher()
        refresher.refresh()
        // A failed refresh is shown on the widget, and the next run is half an hour away anyway:
        // retrying sooner would spend the battery the widget exists to save a trip for.
        return Result.success()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DutyWidgetEntryPoint {
    fun refresher(): DutyWidgetRefresher
}

object DutyWidgetWork {
    private const val PERIODIC = "duty-widget-refresh"
    private const val NOW = "duty-widget-refresh-now"

    /** Android's floor for periodic work is 15 minutes; the roster does not change that fast. */
    private const val INTERVAL_MINUTES = 30L

    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DutyWidgetWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES).build(),
        )
    }

    /** A first answer as soon as the widget is placed, rather than half an hour later. */
    fun refreshNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            NOW,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<DutyWidgetWorker>().build(),
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).run {
            cancelUniqueWork(PERIODIC)
            cancelUniqueWork(NOW)
        }
    }
}
