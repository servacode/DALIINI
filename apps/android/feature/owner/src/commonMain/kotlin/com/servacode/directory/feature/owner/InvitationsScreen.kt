package com.servacode.directory.feature.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryEmptyState
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectorySecondaryButton
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.OwnerWords
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.designsystem.appErrorText
import com.servacode.directory.core.model.DamascusTime
import com.servacode.directory.core.model.ReceivedInvitation
import org.jetbrains.compose.resources.stringResource

/**
 * «دعوات الإدارة»: the facilities whose owners asked this account, by its phone number, to help
 * run them (DECISION-064).
 *
 * One card per invitation, naming the facility the way a list of facilities does and who sent
 * it. Accepting opens the facility's management page straight away, because that is the reason
 * to accept; declining only takes the card away.
 */
@Composable
fun InvitationsScreen(
    viewModel: InvitationsViewModel,
    onJoined: (facilityId: String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val joined by viewModel.joined.collectAsStateWithLifecycle()
    LaunchedEffect(joined) {
        joined?.let { facilityId ->
            viewModel.consumeJoined()
            onJoined(facilityId)
        }
    }

    DirectoryPage(
        topBar = { DirectoryTopBar(title = InvitationsCopy.TITLE, onBack = onBack) },
    ) { padding ->
        when (val value = state) {
            InvitationsUiState.Loading -> DirectoryLoading(Modifier.padding(padding))
            is InvitationsUiState.Error -> DirectoryErrorState(
                title = InvitationsCopy.ERROR,
                modifier = Modifier.padding(padding),
                error = value.error,
                onRetry = viewModel::refresh,
            )
            is InvitationsUiState.Content -> if (value.items.isEmpty()) {
                DirectoryEmptyState(
                    title = InvitationsCopy.EMPTY,
                    modifier = Modifier.padding(padding),
                    body = InvitationsCopy.EMPTY_BODY,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(
                        start = Space.screen,
                        end = Space.screen,
                        top = Space.base,
                        bottom = Space.xxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Space.md),
                ) {
                    value.failure?.let { failure ->
                        item(key = "failure") {
                            Text(
                                text = appErrorText(failure),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        }
                    }
                    items(value.items, key = { it.id }) { invitation ->
                        InvitationCard(
                            invitation = invitation,
                            busy = value.busy == invitation.id,
                            enabled = value.busy == null,
                            onAccept = { viewModel.accept(invitation.id) },
                            onDecline = { viewModel.decline(invitation.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvitationCard(
    invitation: ReceivedInvitation,
    busy: Boolean,
    enabled: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    DirectoryCard {
        Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
            Text(
                text = invitation.facilityNameAr,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "${invitation.categoryNameAr} - ${invitation.provinceNameAr}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = InvitationsCopy.body(invitation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = InvitationsCopy.expires(DamascusTime.format(invitation.expiresAtEpochMillis)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                DirectoryPrimaryButton(
                    text = InvitationsCopy.ACCEPT,
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    loading = busy,
                )
                DirectorySecondaryButton(
                    text = InvitationsCopy.DECLINE,
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                )
            }
        }
    }
}

object InvitationsCopy {
    val TITLE: String @Composable get() = stringResource(Res.string.received_invitations_title)
    val ERROR: String @Composable get() = stringResource(Res.string.received_invitations_error)
    val EMPTY: String @Composable get() = stringResource(Res.string.received_invitations_empty)
    val EMPTY_BODY: String
        @Composable get() = stringResource(Res.string.received_invitations_empty_body)
    val ACCEPT: String @Composable get() = stringResource(Res.string.received_invitation_accept)
    val DECLINE: String @Composable get() = stringResource(Res.string.received_invitation_decline)

    /** Who asked, and for which part: "سامر يدعوك إلى إدارتها بصفة مدير". */
    @Composable
    fun body(invitation: ReceivedInvitation): String {
        val role = OwnerWords.role(invitation.role)
        return invitation.invitedByName?.takeIf { it.isNotBlank() }
            ?.let { stringResource(Res.string.received_invitation_body, it, role) }
            ?: stringResource(Res.string.received_invitation_body_anonymous, role)
    }

    @Composable
    fun expires(date: String): String = stringResource(Res.string.received_invitation_expires, date)
}
