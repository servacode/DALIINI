package com.servacode.directory.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDirection
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryMenuDivider
import com.servacode.directory.core.designsystem.DirectoryMenuSection
import com.servacode.directory.core.designsystem.DirectoryOfflineNotice
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.LocalDirectoryTones
import com.servacode.directory.core.designsystem.Sizes
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.model.EmergencyNumber

/**
 * «أرقام الطوارئ»: the country's lines, then the province's, each a tap from the dialer.
 *
 * The dialer opens with the number filled in; the call is still the reader's to place. What was
 * last served stays on the device, so the page works with no connection.
 */
@Composable
fun EmergencyNumbersScreen(
    onBack: () -> Unit,
    viewModel: EmergencyNumbersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    DirectoryPage(
        topBar = { DirectoryTopBar(title = EmergencyCopy.TITLE, onBack = onBack) },
    ) { padding ->
        when (val value = state) {
            EmergencyUiState.Loading -> DirectoryLoading(Modifier.padding(padding))
            is EmergencyUiState.Content -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.screen, vertical = Space.base),
                verticalArrangement = Arrangement.spacedBy(Space.lg),
            ) {
                if (value.builtIn) {
                    // Not the platform's list: the few the app carries, to be checked.
                    val warning = LocalDirectoryTones.current.warning
                    Text(
                        text = EmergencyCopy.VERIFY,
                        style = MaterialTheme.typography.bodyMedium,
                        color = warning.content,
                    )
                }
                if (value.stale) DirectoryOfflineNotice(onRetry = viewModel::refresh)
                if (value.numbers.national.isNotEmpty()) {
                    NumberSection(EmergencyCopy.NATIONAL, value.numbers.national) { dial(context, it) }
                }
                if (value.numbers.province.isNotEmpty()) {
                    NumberSection(EmergencyCopy.PROVINCE, value.numbers.province) { dial(context, it) }
                }
            }
        }
    }
}

@Composable
private fun NumberSection(label: String, numbers: List<EmergencyNumber>, onCall: (String) -> Unit) {
    DirectoryMenuSection(label) {
        numbers.forEachIndexed { index, number ->
            if (index > 0) DirectoryMenuDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Sizes.touchTarget + Space.md)
                    .clickable(
                        onClickLabel = EmergencyCopy.call(number.nameAr),
                        role = Role.Button,
                    ) { onCall(number.number) }
                    .padding(horizontal = Space.base, vertical = Space.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.md),
            ) {
                DirectoryIcon(DirectoryIcons.phone, null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = number.nameAr,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                // Digits read left to right, even in an Arabic line.
                Text(
                    text = number.number,
                    style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun dial(context: Context, number: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$number".toUri()))
    } catch (_: ActivityNotFoundException) {
        // A device with no dialer (a tablet): the number is on screen to be read.
    }
}

object EmergencyCopy {
    val TITLE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.emergency_title)
    val NATIONAL: String @Composable @ReadOnlyComposable get() = stringResource(R.string.emergency_national)
    val PROVINCE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.emergency_province)
    val VERIFY: String @Composable @ReadOnlyComposable get() = stringResource(R.string.emergency_verify)

    @Composable
    @ReadOnlyComposable
    fun call(name: String): String = stringResource(R.string.emergency_call, name)
}
