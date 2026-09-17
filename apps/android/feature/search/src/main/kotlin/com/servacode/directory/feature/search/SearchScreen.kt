package com.servacode.directory.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SearchScreen(
    onFacility: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("البحث", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::updateQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("اسم المنشأة أو الاختصاص") },
        )
        when (val value = state) {
            SearchUiState.Idle -> Text("اكتب للبحث ضمن المحافظة المختارة", Modifier.padding(top = 16.dp))
            SearchUiState.Loading -> CircularProgressIndicator(Modifier.padding(top = 16.dp))
            SearchUiState.Error -> Text("تعذر تنفيذ البحث", Modifier.padding(top = 16.dp))
            is SearchUiState.Results -> LazyColumn(Modifier.fillMaxSize()) {
                items(value.values, key = { it.id }) { facility ->
                    Column(
                        Modifier.fillMaxWidth().clickable { onFacility(facility.id) }.padding(vertical = 14.dp),
                    ) {
                        Text(facility.nameAr, style = MaterialTheme.typography.titleMedium)
                        Text(facility.category.nameAr)
                    }
                }
            }
        }
    }
}
