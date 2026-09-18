package com.servacode.directory.feature.directory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.model.AvailabilityLabel

@Composable
fun DirectoryScreen(
    onFacility: (String) -> Unit,
    onProvince: () -> Unit,
    viewModel: DirectoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("المنشآت", style = MaterialTheme.typography.headlineLarge)
        when (val value = state) {
            DirectoryUiState.Loading -> CircularProgressIndicator(Modifier.padding(top = 20.dp))
            DirectoryUiState.ProvinceRequired -> Text(
                "اختر المحافظة أولًا",
                Modifier.clickable(onClick = onProvince).padding(top = 20.dp),
            )
            is DirectoryUiState.Error -> Column(Modifier.padding(top = 20.dp)) {
                Text("تعذر تحميل المنشآت")
                Text(value.message)
                OutlinedButton(onClick = viewModel::refresh) { Text("إعادة المحاولة") }
            }
            is DirectoryUiState.Content -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = value.filter.openNow,
                        onClick = { viewModel.setOpenNow(!value.filter.openNow) },
                        label = { Text("مفتوح الآن") },
                    )
                    FilterChip(
                        selected = value.filter.dutyNow,
                        onClick = { viewModel.setDutyNow(!value.filter.dutyNow) },
                        label = { Text("مناوب الآن") },
                    )
                }
                if (value.stale) Text("غير متصل — بعض البيانات وحالة مفتوح/مناوب قد تكون قديمة")
                LazyColumn(Modifier.fillMaxSize()) {
                    items(value.values, key = { it.id }) { facility ->
                        Column(
                            Modifier.fillMaxWidth().clickable { onFacility(facility.id) }.padding(vertical = 14.dp),
                        ) {
                            Text(facility.nameAr, style = MaterialTheme.typography.titleMedium)
                            Text(facility.category.nameAr)
                            Text(AvailabilityLabel.of(facility))
                            facility.distanceMeters?.let { Text("${it.toInt()} م") }
                        }
                    }
                    if (value.hasMore || value.moreError != null) {
                        item(key = "more") {
                            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                value.moreError?.let { Text(it) }
                                if (value.loadingMore) {
                                    CircularProgressIndicator()
                                } else {
                                    OutlinedButton(onClick = viewModel::loadMore) { Text("عرض المزيد") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
