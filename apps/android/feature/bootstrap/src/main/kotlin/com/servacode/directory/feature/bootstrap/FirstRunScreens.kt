package com.servacode.directory.feature.bootstrap

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.servacode.directory.core.designsystem.BrandColors
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectorySecondaryButton
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.IconSize
import com.servacode.directory.core.designsystem.LocationIllustration
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.designsystem.TownIllustration
import com.servacode.directory.core.location.FOREGROUND_LOCATION_PERMISSIONS

/**
 * Screen 02. One page, shown once: a town with the app's pin standing in it, what the app is for
 * in a line, and a single way on. Nothing to swipe through and nothing to read twice.
 */
@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    DirectoryPage { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Space.xl)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Space.xxl))
            TownIllustration()
            Spacer(Modifier.height(Space.xxl))
            Text(
                text = WelcomeCopy.TITLE,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(Space.md))
            Text(
                text = WelcomeCopy.BODY,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Space.xxxl))
            DirectoryPrimaryButton(
                text = WelcomeCopy.CONTINUE,
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Space.xxl))
        }
    }
}

/**
 * Screen 03. Why the app would use the location, in the user's terms, with the system's dialog
 * behind a button they press themselves. Refusing is a plain second choice and costs the
 * distances alone; nothing here asks twice.
 */
@Composable
fun LocationPermissionScreen(
    onDone: () -> Unit,
    onBack: () -> Unit = onDone,
    viewModel: LocationPermissionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val alreadyAllowed = remember {
        FOREGROUND_LOCATION_PERMISSIONS.any {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
    }
    // Granted already, on a reinstall for instance: the question would be noise.
    LaunchedEffect(alreadyAllowed) {
        if (alreadyAllowed) {
            viewModel.answered(LocationAnswer.ALLOWED)
            onDone()
        }
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        viewModel.answered(
            if (result.values.any { it }) LocationAnswer.ALLOWED else LocationAnswer.REFUSED,
        )
        onDone()
    }

    DirectoryPage(
        topBar = { DirectoryTopBar(title = "", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Space.base)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DirectoryCard {
                LocationIllustration()
                Spacer(Modifier.height(Space.lg))
                Text(
                    text = LocationCopy.TITLE,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().semantics { heading() },
                )
                Spacer(Modifier.height(Space.sm))
                Text(
                    text = LocationCopy.NOTE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Space.lg))
                LocationCopy.REASONS.forEach { reason ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Space.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.md),
                    ) {
                        DirectoryIcon(
                            icon = DirectoryIcons.check,
                            contentDescription = null,
                            size = IconSize.small,
                            tint = BrandColors.mark,
                        )
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            Spacer(Modifier.height(Space.xl))
            DirectoryPrimaryButton(
                text = LocationCopy.ALLOW,
                onClick = { ask.launch(FOREGROUND_LOCATION_PERMISSIONS.toTypedArray()) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Space.sm))
            DirectorySecondaryButton(
                text = LocationCopy.LATER,
                onClick = {
                    viewModel.answered(LocationAnswer.LATER)
                    onDone()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Space.xl))
        }
    }
}
