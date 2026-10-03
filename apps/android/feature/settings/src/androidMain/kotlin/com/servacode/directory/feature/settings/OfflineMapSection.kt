package com.servacode.directory.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryMenuDivider
import com.servacode.directory.core.designsystem.DirectoryMenuRow
import com.servacode.directory.core.designsystem.DirectoryMenuSection
import com.servacode.directory.core.maps.MapPackFailure
import com.servacode.directory.core.maps.MapPackState
import com.servacode.directory.core.maps.MapPackTarget
import com.servacode.directory.core.maps.packEstimatedBytes
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

/**
 * The province's map, kept on the device.
 *
 * One row that says where the pack stands, and one action that fits that state: fetch it, stop
 * fetching it, carry on, or give the space back. The app does this by itself on a connection
 * nobody pays by the megabyte for; this is for the reader who wants it now, or not at all.
 */
@Composable
internal fun OfflineMapSection(viewModel: OfflineMapViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val target by viewModel.target.collectAsStateWithLifecycle()

    DirectoryMenuSection(SettingsCopy.OFFLINE_MAP) {
        DirectoryMenuRow(
            title = packTitle(state),
            onClick = {
                when (state) {
                    is MapPackState.Downloading -> if ((state as MapPackState.Downloading).running) {
                        viewModel.pause()
                    } else {
                        viewModel.download()
                    }
                    is MapPackState.Ready, MapPackState.Unknown -> Unit
                    else -> viewModel.download()
                }
            },
            icon = DirectoryIcons.map,
            subtitle = packDetail(state, target),
            trailing = state !is MapPackState.Ready && state != MapPackState.Unknown,
        )
        if (state is MapPackState.Ready) {
            DirectoryMenuDivider()
            DirectoryMenuRow(
                title = SettingsCopy.OFFLINE_DELETE,
                onClick = viewModel::remove,
                icon = DirectoryIcons.close,
                danger = true,
                trailing = false,
            )
        }
    }
}

/** What the row says it is offering, which is different in each state. */
@Composable
private fun packTitle(state: MapPackState): String = when (state) {
    MapPackState.Unknown -> SettingsCopy.OFFLINE_MAP
    MapPackState.Absent -> SettingsCopy.OFFLINE_DOWNLOAD
    is MapPackState.Downloading ->
        if (state.running) SettingsCopy.OFFLINE_DOWNLOADING else SettingsCopy.OFFLINE_PAUSED
    is MapPackState.Ready -> SettingsCopy.OFFLINE_READY
    is MapPackState.Failed -> SettingsCopy.OFFLINE_FAILED
}

/**
 * The cost, the progress or the reason, under the title.
 *
 * Before a download there is only an estimate, computed from the box and the zooms; once one is
 * running the engine's own byte count replaces it, because an estimate shown next to a real figure
 * is the one that will be wrong.
 */
@Composable
private fun packDetail(state: MapPackState, target: MapPackTarget?): String = when (state) {
    MapPackState.Unknown -> SettingsCopy.OFFLINE_NO_PROVINCE
    MapPackState.Absent -> when (target) {
        null -> SettingsCopy.OFFLINE_HINT
        else -> stringResource(
            Res.string.settings_offline_estimate,
            megabytes(packEstimatedBytes(target.box)),
        )
    }
    is MapPackState.Downloading -> when (val fraction = state.fraction) {
        null -> SettingsCopy.OFFLINE_COUNTING
        else -> when {
            state.running -> stringResource(
                Res.string.settings_offline_progress,
                (fraction * 100).roundToInt(),
                megabytes(state.bytes),
            )
            else -> stringResource(Res.string.settings_offline_paused_at, (fraction * 100).roundToInt())
        }
    }
    is MapPackState.Ready -> stringResource(Res.string.settings_offline_size, megabytes(state.bytes))
    is MapPackState.Failed -> stringResource(
        when (state.reason) {
            MapPackFailure.CONNECTION -> Res.string.settings_offline_failed_connection
            MapPackFailure.SERVER -> Res.string.settings_offline_failed_server
            MapPackFailure.TILE_LIMIT -> Res.string.settings_offline_failed_limit
            MapPackFailure.OTHER -> Res.string.settings_offline_failed_other
        },
    )
}

/** Bytes as a reader counts them, rounded to the nearest megabyte and never below one. */
private fun megabytes(bytes: Long): Int =
    ((bytes + HALF_MEGABYTE) / MEGABYTE).toInt().coerceAtLeast(1)

private const val MEGABYTE = 1_000_000L
private const val HALF_MEGABYTE = 500_000L
