package com.servacode.directory.feature.ratings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.appErrorText
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryEmptyState
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryTextButton
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.designsystem.StarPicker
import com.servacode.directory.core.model.UserRating

/**
 * Screen 09, as far as the contract reaches: a rating in this app is a number of stars and
 * nothing more, so what a user can see of their own ratings is the facility and the stars.
 *
 * There is no written review, no reviewer and no public list of other people's ratings anywhere
 * in the API, so none is drawn here.
 */
@Composable
fun RatingsScreen(
    onBack: () -> Unit,
    viewModel: RatingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DirectoryPage(
        topBar = { DirectoryTopBar(title = RatingsCopy.TITLE, onBack = onBack) },
    ) { padding ->
        when (val value = state) {
            RatingsUiState.Loading -> DirectoryLoading(Modifier.padding(padding))
            RatingsUiState.Error -> DirectoryErrorState(
                title = RatingsCopy.ERROR,
                modifier = Modifier.padding(padding),
                onRetry = viewModel::refresh,
            )
            is RatingsUiState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = Space.xxl),
            ) {
                value.failure?.let { failure ->
                    item(key = "message") {
                        Text(
                            text = appErrorText(failure),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.sm),
                        )
                    }
                }
                if (value.values.isEmpty()) {
                    item(key = "empty") {
                        DirectoryEmptyState(
                            title = RatingsCopy.EMPTY,
                            body = RatingsCopy.EMPTY_BODY,
                            icon = DirectoryIcons.star,
                            modifier = Modifier.padding(top = Space.xxl),
                        )
                    }
                }
                items(value.values, key = { it.id }) { rating ->
                    RatingCard(
                        rating = rating,
                        saving = value.savingFacilityId == rating.facilityId,
                        onRate = { stars -> viewModel.update(rating.facilityId, stars) },
                        onDelete = { viewModel.delete(rating.facilityId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingCard(
    rating: UserRating,
    saving: Boolean,
    onRate: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    DirectoryCard(
        modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.xs),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(
                text = rating.facilityNameAr,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StarPicker(
                    stars = rating.stars,
                    onRate = onRate,
                    label = { stars -> RatingsCopy.starLabel(stars) },
                    enabled = !saving,
                )
                DirectoryTextButton(RatingsCopy.DELETE, onDelete, enabled = !saving)
            }
        }
    }
}

/** The words of the user's own ratings, provisional until product copy is approved. */
object RatingsCopy {
    val TITLE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.ratings_title)
    val ERROR: String @Composable @ReadOnlyComposable get() = stringResource(R.string.ratings_error)
    val EMPTY: String @Composable @ReadOnlyComposable get() = stringResource(R.string.ratings_empty)
    val EMPTY_BODY: String @Composable @ReadOnlyComposable get() = stringResource(R.string.ratings_empty_body)
    val DELETE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.ratings_delete)

    @Composable
    @ReadOnlyComposable
    fun starLabel(stars: Int): String = stringResource(R.string.ratings_star_label, stars)
}
