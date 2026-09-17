package com.servacode.directory.feature.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.model.AvailabilityState
import com.servacode.directory.core.model.FacilitySummary

@Composable
fun HomeScreen(
    onProvince: () -> Unit,
    onSearch: () -> Unit,
    onCategory: (String) -> Unit,
    onFacility: (String) -> Unit,
    onMap: () -> Unit,
    onAccount: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refresh() }

    when (val value = state) {
        HomeUiState.Loading -> Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { CircularProgressIndicator() }
        HomeUiState.ProvinceRequired -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("اختر المحافظة للبدء", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onProvince) { Text("اختيار المحافظة") }
        }
        HomeUiState.Error -> Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("تعذر تحميل الدليل حاليًا")
            Spacer(Modifier.height(12.dp))
            Button(onClick = viewModel::refresh) { Text("إعادة المحاولة") }
        }
        is HomeUiState.Content -> LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("الدليل", style = MaterialTheme.typography.headlineLarge)
                        Text(value.snapshot.province.nameAr)
                    }
                    OutlinedButton(onClick = onProvince) { Text("تغيير") }
                }
                if (value.stale) {
                    Text(
                        "غير متصل — بعض البيانات وحالة مفتوح/مناوب قد تكون قديمة",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSearch) { Text("بحث") }
                    OutlinedButton(onClick = onMap) { Text("الخريطة") }
                    OutlinedButton(onClick = onAccount) { Text("حسابي") }
                }
                OutlinedButton(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                            ),
                        )
                    },
                ) { Text("استخدام موقعي للترتيب بالأقرب") }
                Text("الأقسام", style = MaterialTheme.typography.titleLarge)
            }
            items(value.snapshot.categories, key = { it.id }) { category ->
                Column(
                    Modifier.fillMaxWidth().clickable { onCategory(category.id) }.padding(vertical = 10.dp),
                ) {
                    Text(category.nameAr, style = MaterialTheme.typography.titleMedium)
                }
                HorizontalDivider()
            }
            item { Text("الأقرب إليك", style = MaterialTheme.typography.titleLarge) }
            items(value.snapshot.nearby, key = { it.id }) { facility ->
                FacilityRow(facility, onFacility)
            }
        }
    }
}

@Composable
private fun FacilityRow(value: FacilitySummary, onFacility: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable { onFacility(value.id) }.padding(vertical = 10.dp),
    ) {
        Text(value.nameAr, style = MaterialTheme.typography.titleMedium)
        val status = when (value.availability) {
            AvailabilityState.OPEN -> "مفتوح الآن"
            AvailabilityState.DUTY -> "مناوب الآن"
            AvailabilityState.TEMP_CLOSED -> "مغلق مؤقتًا"
            AvailabilityState.CLOSED -> "مغلق"
        }
        val distance = value.distanceMeters?.let { " • ${it.toInt()} م" }.orEmpty()
        Text(status + distance, style = MaterialTheme.typography.bodyMedium)
    }
}
