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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

/**
 * The notice a build too old to run shows, and the only way out of it.
 *
 * Built like [MaintenanceScreen] on purpose: the same lockup, illustration and shape, because
 * both say the same thing to the person in front of them — the app cannot go on right now, and
 * it is not their fault. What differs is the way out. Maintenance ends on its own and offers a
 * retry; this one does not, so the only action is to go and get a newer build.
 *
 * [message] is what the backend chose to say. When it said nothing, the app's own wording
 * stands in. [onOpenStore] is null when no address was configured, and the button is then not
 * shown at all rather than shown dead: a button that does nothing is worse than none.
 */
@Composable
fun UpdateRequiredScreen(
    message: String?,
    onOpenStore: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Nothing underneath may be reached: this build must not keep talking to a backend
            // that has moved past it.
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
        DirectoryIllustration(DirectoryIllustrations.maintenance, size = 160.dp)
        Text(
            text = stringResource(Res.string.ds_update_required_title),
            style = MaterialTheme.typography.titleLarge,
            // Named, not inherited: there is no Surface above this screen, so the inherited
            // colour was black — and black on the dark theme's background is unreadable.
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = message ?: stringResource(Res.string.ds_update_required_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (onOpenStore != null) {
            DirectoryPrimaryButton(
                text = stringResource(Res.string.ds_update_required_action),
                onClick = onOpenStore,
                modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
            )
        }
    }
}
