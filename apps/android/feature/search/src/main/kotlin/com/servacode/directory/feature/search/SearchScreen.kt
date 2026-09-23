package com.servacode.directory.feature.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryEmptyState
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryMessageState
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectorySearchField
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.FacilityCard
import com.servacode.directory.core.designsystem.LoadMoreRow
import com.servacode.directory.core.designsystem.Space

/**
 * Screen 06. One field, and what the backend answered under it.
 *
 * The search itself is unchanged: the same normalised query, the same wait before asking, the
 * same province, and the same cursor when more is wanted.
 */
@Composable
fun SearchScreen(
    onFacility: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    DirectoryPage(
        topBar = { DirectoryTopBar(title = SearchCopy.TITLE, onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = Space.xxl),
        ) {
            item(key = "field") {
                DirectorySearchField(
                    value = query,
                    onValueChange = viewModel::updateQuery,
                    placeholder = SearchCopy.PLACEHOLDER,
                    modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.sm),
                )
            }
            when (val value = state) {
                SearchUiState.Idle -> item(key = "idle") {
                    DirectoryMessageState(
                        icon = DirectoryIcons.search,
                        title = SearchCopy.IDLE,
                        body = SearchCopy.IDLE_BODY,
                        modifier = Modifier.padding(top = Space.xxl),
                    )
                }
                SearchUiState.Loading -> item(key = "loading") {
                    DirectoryLoading(Modifier.padding(top = Space.xxl))
                }
                is SearchUiState.Error -> item(key = "error") {
                    DirectoryErrorState(
                        title = SearchCopy.ERROR,
                        body = value.message,
                        modifier = Modifier.padding(top = Space.xxl),
                        onRetry = { viewModel.updateQuery(query) },
                    )
                }
                is SearchUiState.Results -> {
                    if (value.values.isEmpty()) {
                        item(key = "empty") {
                            DirectoryEmptyState(
                                title = SearchCopy.EMPTY,
                                body = SearchCopy.EMPTY_BODY,
                                modifier = Modifier.padding(top = Space.xxl),
                            )
                        }
                    }
                    items(value.values, key = { it.id }) { facility ->
                        FacilityCard(
                            facility = facility,
                            onClick = { onFacility(facility.id) },
                            modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.xs),
                        )
                    }
                    if (value.hasMore || value.moreError != null) {
                        item(key = "more") {
                            LoadMoreRow(
                                loading = value.loadingMore,
                                onLoadMore = viewModel::loadMore,
                                error = value.moreError,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The words of the search, provisional until product copy is approved. */
object SearchCopy {
    const val TITLE = "بحث"
    const val PLACEHOLDER = "ابحث عن منشأة أو تخصص..."
    const val IDLE = "ابحث داخل محافظتك"
    const val IDLE_BODY = "اكتب اسم المنشأة أو الاختصاص لعرض النتائج."
    const val ERROR = "تعذر البحث"
    const val EMPTY = "لا توجد نتائج"
    const val EMPTY_BODY = "جرّب كلمة أخرى أو تحقق من الإملاء."
}
