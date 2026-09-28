package com.servacode.directory.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The full-screen maintenance notice of product spec §17.
 *
 * It covers the whole app, swallows touches meant for the screens underneath and offers one
 * action: ask the backend again. [message] is the backend's own text when it sent one; the
 * app's words stand in when it did not.
 */
@Composable
fun MaintenanceScreen(
    message: String?,
    retrying: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Nothing underneath may be reached while the notice is up.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .verticalScroll(rememberScrollState())
            .padding(Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.base, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandLockup()
        TownBackdrop(Modifier.fillMaxWidth(), height = 160.dp)
        Text(
            text = stringResource(R.string.ds_maintenance_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = message ?: stringResource(R.string.ds_maintenance_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        DirectoryPrimaryButton(
            text = stringResource(R.string.ds_retry),
            onClick = onRetry,
            loading = retrying,
            modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
        )
        Text(
            text = stringResource(R.string.ds_maintenance_auto_retry),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
