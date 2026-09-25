package com.servacode.directory.feature.province

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.appErrorText
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryOfflineNotice
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectorySettingRow
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.Space

/**
 * Where the whole app is pointed. It is not one of the numbered screens, but it is what every
 * list in the app is scoped by, so it wears the same frame as the rest.
 */
@Composable
fun ProvinceScreen(
    onSelected: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: ProvinceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DirectoryPage(
        topBar = { DirectoryTopBar(title = ProvinceCopy.TITLE, onBack = onBack) },
    ) { padding ->
        when (val value = state) {
            ProvinceUiState.Loading -> DirectoryLoading(Modifier.padding(padding))
            is ProvinceUiState.Error -> DirectoryErrorState(
                title = ProvinceCopy.ERROR,
                modifier = Modifier.padding(padding),
                body = appErrorText(value.error),
                onRetry = viewModel::refresh,
            )
            is ProvinceUiState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = Space.xxl),
            ) {
                if (value.stale) {
                    item(key = "stale") {
                        DirectoryOfflineNotice(
                            modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.sm),
                            text = ProvinceCopy.STALE,
                            onRetry = viewModel::refresh,
                        )
                    }
                }
                itemsIndexed(value.provinces, key = { _, province -> province.id }) { index, province ->
                    DirectorySettingRow(
                        title = province.nameAr,
                        onClick = { viewModel.select(province.id, onSelected) },
                        icon = DirectoryIcons.pin,
                    )
                    if (index < value.provinces.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = Space.screen),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}

/** The words of the province picker, provisional until product copy is approved. */
object ProvinceCopy {
    val TITLE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.province_title)
    val ERROR: String @Composable @ReadOnlyComposable get() = stringResource(R.string.province_error)
    val STALE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.province_stale)
}
