package com.servacode.directory.widget

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.servacode.directory.core.designsystem.Radius
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.designsystem.Sizes
import com.servacode.directory.core.designsystem.TypeScale
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.servacode.directory.AppEntries
import com.servacode.directory.MainActivity
import com.servacode.directory.R
import com.servacode.directory.core.designsystem.DirectoryPalettes
import com.servacode.directory.core.designsystem.R as DesignSystemR
import com.servacode.directory.core.model.DamascusTime
import com.servacode.directory.core.model.RoundedDistance
import com.servacode.directory.core.model.roundedDistance

/**
 * «المناوب الآن» on the home screen: the nearest facilities on duty right now in the reader's
 * province, a tap to call the first, a tap on any to open it in the app.
 *
 * Arabic, right to left, whatever the phone's own language: a widget is drawn by the launcher in
 * the phone's direction, so on a left-to-right phone the rows are laid out in reverse and the
 * text set against the right edge — what the app does for its own screens by fixing their
 * direction. Light and dark follow the phone, in the app's own token colours.
 */
class DutyWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Where a widget placed after the last refresh starts: the last answer, not nothing.
        val saved = DutyWidgetStore(context).read()
        provideContent {
            val state = DutyWidgetState.decode(currentState<Preferences>()[STATE]) ?: saved ?: DutyWidgetState()
            GlanceTheme(colors = COLORS) {
                DutyWidgetBody(state)
            }
        }
    }

    companion object {
        /** The widget's state, as the refresher writes it. */
        val STATE = stringPreferencesKey("duty_widget_state")

        private val COLORS = ColorProviders(light = DirectoryPalettes.light, dark = DirectoryPalettes.dark)
    }
}

class DutyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DutyWidget()

    /** The first widget placed: refresh now, then every half hour. */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        DutyWidgetWork.schedule(context)
        DutyWidgetWork.refreshNow(context)
    }

    /** The last one removed: nothing is refreshed for nobody. */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        DutyWidgetWork.cancel(context)
    }
}

@Composable
private fun DutyWidgetBody(state: DutyWidgetState) {
    val context = LocalContext.current
    val layout = WidgetLayout(context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL)
    val rows = DutyWidgetContent.rowsFor(LocalSize.current.height.value)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(Radius.large)
            .padding(horizontal = Space.md, vertical = Space.sm),
        horizontalAlignment = layout.readingStart,
    ) {
        Header(state, layout)
        Spacer(GlanceModifier.height(Space.sm))
        when (state.kind) {
            DutyWidgetKind.NO_PROVINCE -> Message(
                context.getString(R.string.widget_duty_no_province),
                actionStartActivity(Intent(context, MainActivity::class.java)),
            )
            DutyWidgetKind.LOADING -> Message(context.getString(R.string.widget_duty_loading))
            DutyWidgetKind.OFFLINE -> Message(context.getString(DesignSystemR.string.ds_error_offline))
            DutyWidgetKind.EMPTY -> Message(context.getString(R.string.widget_duty_empty))
            DutyWidgetKind.LIST -> state.facilities.take(rows).forEach { FacilityRow(it, layout) }
        }
        if (state.kind == DutyWidgetKind.LIST || state.kind == DutyWidgetKind.EMPTY) {
            Footer(state)
        }
    }
}

/** «المناوب الآن» and the province; a tap opens the day's roster in the app. */
@Composable
private fun Header(state: DutyWidgetState, layout: WidgetLayout) {
    val context = LocalContext.current
    val open = actionStartActivity(AppEntries.link(context, "/duty"))
    layout.ReadingRow(
        modifier = GlanceModifier.fillMaxWidth().clickable(open),
        start = {
            Text(
                text = context.getString(R.string.widget_duty_title),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = TypeScale.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
        },
        gap = Space.sm,
        end = {
            state.provinceNameAr?.let {
                Text(
                    text = it,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = TypeScale.bodySmall),
                    maxLines = 1,
                )
            }
        },
    )
}

/**
 * One facility: its name, then its category and distance; the call button beside it when its
 * number is known. A tap anywhere else opens its page in the app.
 */
@Composable
private fun FacilityRow(facility: DutyWidgetFacility, layout: WidgetLayout) {
    val context = LocalContext.current
    val details = listOfNotNull(facility.categoryNameAr, facility.distanceMeters?.let { distance(context, it) })
        .joinToString(context.getString(DesignSystemR.string.directory_list_separator))
    layout.ReadingRow(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = Space.xs)
            .clickable(actionStartActivity(AppEntries.link(context, "/f/${facility.id}"))),
        start = {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = layout.readingStart,
            ) {
                Text(
                    text = facility.nameAr,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = TypeScale.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Right,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.fillMaxWidth(),
                )
                if (details.isNotEmpty()) {
                    Text(
                        text = details,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = TypeScale.bodySmall,
                            textAlign = TextAlign.Right,
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.fillMaxWidth(),
                    )
                }
            }
        },
        gap = if (facility.phone != null) Space.sm else 0.dp,
        end = {
            val phone = facility.phone
            if (phone != null) {
                Image(
                    provider = ImageProvider(DesignSystemR.drawable.dl_ic_phone),
                    contentDescription = context.getString(R.string.widget_duty_call, facility.nameAr),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                    modifier = GlanceModifier
                        .size(Sizes.touchTarget)
                        .padding(Space.sm)
                        .cornerRadius(Radius.pill)
                        .background(GlanceTheme.colors.primaryContainer)
                        .clickable(actionStartActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))),
                )
            }
        },
    )
}

/** A sentence where the rows would be, opening the app when [action] is given. */
@Composable
private fun Message(text: String, action: Action? = null) {
    Text(
        text = text,
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = TypeScale.bodyMedium, textAlign = TextAlign.Right),
        maxLines = 3,
        modifier = GlanceModifier
            .fillMaxWidth()
            .let { if (action != null) it.clickable(action) else it },
    )
}

/** When the list was answered, or that it is the last answer and the phone is offline. */
@Composable
private fun Footer(state: DutyWidgetState) {
    val context = LocalContext.current
    val text = when {
        state.offline -> context.getString(DesignSystemR.string.ds_offline)
        else -> state.refreshedAtEpochMillis?.let {
            context.getString(R.string.widget_duty_updated, DamascusTime.clock(it))
        }
    } ?: return
    Spacer(GlanceModifier.height(Space.xs))
    Text(
        text = text,
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = TypeScale.bodySmall, textAlign = TextAlign.Right),
        maxLines = 2,
        modifier = GlanceModifier.fillMaxWidth(),
    )
}

private fun distance(context: Context, meters: Double): String = when (val rounded = roundedDistance(meters)) {
    is RoundedDistance.Metres -> context.getString(DesignSystemR.string.ds_distance_metres, rounded.value)
    is RoundedDistance.Kilometres ->
        context.getString(DesignSystemR.string.ds_distance_kilometres, rounded.whole, rounded.tenth)
}

/**
 * Right to left on any phone. [hostRtl] is the direction the launcher lays the widget out in;
 * where it is left to right, a row's parts are given in reverse so that the reading start is
 * still on the right.
 */
private class WidgetLayout(private val hostRtl: Boolean) {
    /** The right-hand edge, in the launcher's own terms. */
    val readingStart: Alignment.Horizontal = if (hostRtl) Alignment.Start else Alignment.End

    /** [start] is read first, [end] after it, [gap] between them. */
    @Composable
    fun ReadingRow(
        modifier: GlanceModifier,
        start: @Composable RowScope.() -> Unit,
        end: @Composable RowScope.() -> Unit,
        gap: Dp = 0.dp,
    ) {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            if (hostRtl) start() else end()
            if (gap > 0.dp) Spacer(GlanceModifier.width(gap))
            if (hostRtl) end() else start()
        }
    }
}
