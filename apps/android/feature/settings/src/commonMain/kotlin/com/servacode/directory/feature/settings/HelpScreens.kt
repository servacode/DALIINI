package com.servacode.directory.feature.settings

import com.servacode.directory.core.designsystem.DirectoryGlyph
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryEmptyState
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryMenuDivider
import com.servacode.directory.core.designsystem.DirectoryMenuGroup
import com.servacode.directory.core.designsystem.DirectoryMenuRow
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.LegalPage
import com.servacode.directory.core.model.LegalPageKey
import org.jetbrains.compose.resources.stringResource

/**
 * The help and information section: everything the platform publishes about itself.
 *
 * The list comes from the backend, so a page that has not been published simply is not offered —
 * which is why there is no "contact us" here until someone configures one.
 */
@Composable
fun HelpScreen(
    viewModel: LegalViewModel,
    onPage: (LegalPageKey) -> Unit,
    onBack: () -> Unit,
    appVersion: String,
) {
    val state by viewModel.pages.collectAsStateWithLifecycle()

    DirectoryPage(
        topBar = { DirectoryTopBar(title = HelpCopy.TITLE, onBack = onBack) },
    ) { padding ->
        when (val value = state) {
            LegalListState.Loading -> DirectoryLoading(Modifier.padding(padding))
            is LegalListState.Error -> DirectoryErrorState(
                title = HelpCopy.ERROR,
                modifier = Modifier.padding(padding),
                error = value.error,
                onRetry = viewModel::refresh,
            )
            is LegalListState.Content -> if (value.pages.isEmpty()) {
                DirectoryEmptyState(
                    title = HelpCopy.EMPTY,
                    modifier = Modifier.padding(padding),
                    body = HelpCopy.EMPTY_BODY,
                )
            } else {
                // Few enough pages to draw at once, and one card reads as one subject — which
                // is what this is: everything the platform publishes about itself.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Space.screen),
                    verticalArrangement = Arrangement.spacedBy(Space.base),
                ) {
                    DirectoryMenuGroup {
                        value.pages.forEachIndexed { index, page ->
                            if (index > 0) DirectoryMenuDivider()
                            DirectoryMenuRow(
                                title = page.titleAr,
                                onClick = { onPage(page.key) },
                                icon = page.key.icon(),
                            )
                        }
                    }
                    Text(
                        text = HelpCopy.version(appVersion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Space.xxl),
                    )
                }
            }
        }
    }
}

/** One published page, in the words the platform published. */
@Composable
fun LegalPageScreen(
    viewModel: LegalViewModel,
    key: LegalPageKey,
    onBack: () -> Unit,
) {
    val state by viewModel.page.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(key) { viewModel.open(key) }

    DirectoryPage(
        topBar = {
            DirectoryTopBar(
                title = (state as? LegalPageState.Content)?.page?.titleAr.orEmpty(),
                onBack = onBack,
            )
        },
    ) { padding ->
        when (val value = state) {
            LegalPageState.Loading -> DirectoryLoading(Modifier.padding(padding))
            is LegalPageState.Error -> DirectoryErrorState(
                title = HelpCopy.ERROR,
                modifier = Modifier.padding(padding),
                error = value.error,
                onRetry = { viewModel.open(key) },
            )
            is LegalPageState.Content -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Space.screen)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = value.page.bodyAr.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = Space.base),
                )
                Text(
                    text = HelpCopy.pageVersion(value.page.version),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Space.xxl),
                )
            }
        }
    }
}

private fun LegalPageKey.icon(): DirectoryGlyph = when (this) {
    LegalPageKey.ABOUT -> DirectoryIcons.info
    LegalPageKey.PRIVACY -> DirectoryIcons.verified
    LegalPageKey.TERMS -> DirectoryIcons.document
    LegalPageKey.INSTRUCTIONS -> DirectoryIcons.grid
    LegalPageKey.FAQ -> DirectoryIcons.info
    LegalPageKey.CONTACT -> DirectoryIcons.phone
}

/** The words of the help section, provisional until product copy is approved. */
object HelpCopy {
    val TITLE: String @Composable get() = stringResource(Res.string.help_title)
    val ERROR: String @Composable get() = stringResource(Res.string.help_error)
    val EMPTY: String @Composable get() = stringResource(Res.string.help_empty)
    val EMPTY_BODY: String @Composable get() = stringResource(Res.string.help_empty_body)

    @Composable
    fun version(name: String): String = stringResource(Res.string.help_version, name)


    @Composable
    fun pageVersion(version: Int): String = stringResource(Res.string.help_page_version, version)
}

/** Kept so the list and one page can be read from the same repository. */
sealed interface LegalListState {
    data object Loading : LegalListState
    data class Content(val pages: List<LegalPage>) : LegalListState
    data class Error(val error: AppError) : LegalListState
}

sealed interface LegalPageState {
    data object Loading : LegalPageState
    data class Content(val page: LegalPage) : LegalPageState
    data class Error(val error: AppError) : LegalPageState
}
