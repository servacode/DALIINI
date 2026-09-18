package com.servacode.directory.feature.province

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProvinceScreen(
    onSelected: () -> Unit,
    viewModel: ProvinceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val value = state) {
        ProvinceUiState.Loading -> Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { CircularProgressIndicator() }
        is ProvinceUiState.Error -> Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("تعذر تحميل المحافظات")
            Text(value.message)
            Button(onClick = viewModel::refresh) { Text("إعادة المحاولة") }
        }
        is ProvinceUiState.Content -> LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Text("اختر المحافظة", style = MaterialTheme.typography.headlineLarge)
                if (value.stale) Text("يعرض آخر قائمة محفوظة")
            }
            items(value.provinces, key = { it.id }) { province ->
                Text(
                    text = province.nameAr,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.select(province.id, onSelected) }
                        .padding(vertical = 18.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                HorizontalDivider()
            }
        }
    }
}
