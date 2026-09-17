package com.servacode.directory.feature.ratings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RatingsScreen(viewModel: RatingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("تقييماتي", style = MaterialTheme.typography.headlineLarge)
        when (val value = state) {
            RatingsUiState.Loading -> CircularProgressIndicator(Modifier.padding(top = 16.dp))
            RatingsUiState.Error -> Button(onClick = viewModel::refresh) { Text("إعادة المحاولة") }
            is RatingsUiState.Content -> LazyColumn(Modifier.fillMaxSize()) {
                items(value.values, key = { it.id }) { rating ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Text(rating.facilityNameAr, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { stars ->
                                OutlinedButton(
                                    onClick = { viewModel.update(rating.facilityId, stars) },
                                    enabled = value.savingFacilityId != rating.facilityId,
                                ) { Text(stars.toString()) }
                            }
                        }
                        OutlinedButton(onClick = { viewModel.delete(rating.facilityId) }) {
                            Text("حذف التقييم")
                        }
                    }
                }
            }
        }
    }
}
